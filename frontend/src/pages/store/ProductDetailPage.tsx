import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  CheckCircle2,
  Heart,
  Minus,
  Plus,
  ShoppingCart,
  Star,
  Trash2,
  Pencil,
  ZoomIn,
  X,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useAuth } from '../../context/AuthContext';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { ProductImage } from '../../components/ui/ProductImageFallback';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { EmptyState } from '../../components/ui/EmptyState';
import { fieldBaseClass } from '../../components/ui/Input';
import { formatDate, formatNumber, getApiErrorMessage } from '../../lib/utils';
import {
  PagedResponse,
  ProductResponse,
  ProductVariantResponse,
  ReviewResponse,
} from '../../types/api';

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, isAuthenticated, isAdmin } = useAuth();
  const { t, locale } = useI18n();
  const { formatPrice, formatBasePrice, isEstimatedDisplay, displayCurrency } = useCurrency();

  const [selectedVariant, setSelectedVariant] = useState<ProductVariantResponse | null>(null);
  const [activeImage, setActiveImage] = useState<string>('');
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [quantity, setQuantity] = useState(1);

  const [rating, setRating] = useState(5);
  const [reviewTitle, setReviewTitle] = useState('');
  const [reviewComment, setReviewComment] = useState('');
  const [editingReviewId, setEditingReviewId] = useState<number | null>(null);
  const [deletingReviewId, setDeletingReviewId] = useState<number | null>(null);

  const { data: product, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: async () => (await api.get<ProductResponse>(`/api/products/${id}`)).data,
    enabled: Boolean(id),
  });

  const { data: reviewsPage } = useQuery({
    queryKey: ['product-reviews', id],
    queryFn: async () =>
      (await api.get<PagedResponse<ReviewResponse>>(`/api/products/${id}/reviews?size=20`)).data,
    enabled: Boolean(id),
  });

  const { data: eligibility } = useQuery({
    queryKey: ['review-eligibility', id],
    queryFn: async () =>
      (await api.get<{ eligible: boolean }>(`/api/products/${id}/reviews/eligibility`)).data,
    enabled: Boolean(id) && isAuthenticated && !isAdmin,
  });

  const { data: wishlistIds = [] } = useQuery({
    queryKey: ['wishlist-ids'],
    queryFn: async () => (await api.get<number[]>('/api/wishlist/ids')).data,
    enabled: isAuthenticated && !isAdmin,
  });

  useEffect(() => {
    if (!product) return;
    const firstInStock =
      product.variants.find((v) => v.stockQuantity > 0) || product.variants[0] || null;
    setSelectedVariant(firstInStock);
    setActiveImage(
      firstInStock?.imageUrl || product.images[0]?.imageUrl || product.imageUrl || ''
    );
    setQuantity(1);
  }, [product]);

  useEffect(() => {
    if (!lightboxOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setLightboxOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [lightboxOpen]);

  const galleryImages = useMemo(() => {
    if (!product) return [];
    const urls = new Set<string>();
    if (product.imageUrl) urls.add(product.imageUrl);
    product.images.forEach((img) => urls.add(img.imageUrl));
    product.variants.forEach((v) => {
      if (v.imageUrl) urls.add(v.imageUrl);
    });
    return Array.from(urls);
  }, [product]);

  const addToCartMutation = useMutation({
    mutationFn: async () => {
      if (!product) return;
      return (
        await api.post('/api/cart/items', {
          productId: product.id,
          variantId: selectedVariant?.id,
          quantity,
        })
      ).data;
    },
    onSuccess: () => {
      toast.success(t('product.addedToCart'));
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const toggleWishlistMutation = useMutation({
    mutationFn: async () => {
      if (!product) return;
      if (wishlistIds.includes(product.id)) {
        await api.delete(`/api/wishlist/${product.id}`);
        return false;
      } else {
        await api.post(`/api/wishlist/${product.id}`);
        return true;
      }
    },
    onSuccess: (added) => {
      toast.success(added ? t('product.savedWishlist') : t('product.removedWishlist'));
      queryClient.invalidateQueries({ queryKey: ['wishlist-ids'] });
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const saveReviewMutation = useMutation({
    mutationFn: async () => {
      if (editingReviewId) {
        return (
          await api.put(`/api/reviews/${editingReviewId}`, {
            rating,
            title: reviewTitle,
            comment: reviewComment,
          })
        ).data;
      }
      return (
        await api.post(`/api/products/${id}/reviews`, {
          rating,
          title: reviewTitle,
          comment: reviewComment,
        })
      ).data;
    },
    onSuccess: () => {
      toast.success(editingReviewId ? t('product.reviewUpdated') : t('product.reviewCreated'));
      setEditingReviewId(null);
      setReviewTitle('');
      setReviewComment('');
      setRating(5);
      queryClient.invalidateQueries({ queryKey: ['product-reviews', id] });
      queryClient.invalidateQueries({ queryKey: ['product', id] });
      queryClient.invalidateQueries({ queryKey: ['review-eligibility', id] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const deleteReviewMutation = useMutation({
    mutationFn: async (reviewId: number) => api.delete(`/api/reviews/${reviewId}`),
    onSuccess: () => {
      toast.success(t('product.reviewDeleted'));
      setDeletingReviewId(null);
      queryClient.invalidateQueries({ queryKey: ['product-reviews', id] });
      queryClient.invalidateQueries({ queryKey: ['product', id] });
      queryClient.invalidateQueries({ queryKey: ['review-eligibility', id] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const handleSelectVariant = (variant: ProductVariantResponse) => {
    setSelectedVariant(variant);
    setQuantity(1);
    if (variant.imageUrl) {
      setActiveImage(variant.imageUrl);
    }
  };

  const handleAddToCart = () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    addToCartMutation.mutate();
  };

  if (isLoading) {
    return (
      <div className="w-full px-4 py-8 md:px-6">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
          <div className="aspect-square animate-pulse rounded-2xl bg-subtle lg:col-span-6" />
          <div className="space-y-4 lg:col-span-6">
            <div className="h-8 w-2/3 animate-pulse rounded-xl bg-subtle" />
            <div className="h-10 w-1/3 animate-pulse rounded-xl bg-subtle" />
            <div className="h-32 animate-pulse rounded-xl bg-subtle" />
          </div>
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16 text-center">
        <p className="text-xl font-bold text-ink">{t('product.notFound')}</p>
        <Link to="/" className="mt-4 inline-block font-semibold text-accent hover:underline">
          {t('product.backToShop')}
        </Link>
      </div>
    );
  }

  const currentPrice = selectedVariant?.price ?? product.price;
  const currentStock = selectedVariant?.stockQuantity ?? product.stockQuantity;
  const isFav = wishlistIds.includes(product.id);
  const hasReviews = (product.reviewCount ?? 0) > 0;
  const reviews = reviewsPage?.content ?? [];

  return (
    <div className="w-full px-4 py-6 md:px-6">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="mb-5 flex items-center gap-2 text-xs font-medium text-ink-secondary">
        <Link
          to="/"
          className="inline-flex items-center gap-1.5 font-semibold text-ink-secondary transition hover:text-ink"
        >
          <ArrowLeft className="h-3.5 w-3.5 rtl:rotate-180" />
          <span>{t('product.backToCatalog')}</span>
        </Link>
        <span>/</span>
        <span>{product.categoryName}</span>
        <span>/</span>
        <span dir="auto" className="truncate font-semibold text-ink">
          {product.name}
        </span>
      </nav>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        {/* Left: Gallery + Lightbox Trigger */}
        <div className="lg:col-span-6">
          <div className="group relative aspect-[4/3] overflow-hidden rounded-2xl border border-line bg-surface shadow-card sm:aspect-square">
            <ProductImage
              src={activeImage}
              alt={product.name}
              className="h-full w-full object-cover"
              iconClassName="h-14 w-14"
            />
            {activeImage && (
              <button
                type="button"
                onClick={() => setLightboxOpen(true)}
                aria-label={t('product.zoomImage')}
                title={t('product.zoomImage')}
                className="absolute bottom-3 end-3 inline-flex items-center gap-1.5 rounded-xl border border-line bg-surface/90 px-3 py-1.5 text-xs font-semibold text-ink shadow-sm backdrop-blur transition hover:bg-surface"
              >
                <ZoomIn className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">{t('product.zoomImage')}</span>
              </button>
            )}
          </div>

          {galleryImages.length > 1 && (
            <div className="mt-3 flex gap-3 overflow-x-auto pb-1">
              {galleryImages.map((imgUrl) => (
                <button
                  type="button"
                  key={imgUrl}
                  onClick={() => setActiveImage(imgUrl)}
                  className={`h-20 w-20 shrink-0 overflow-hidden rounded-xl border-2 transition ${activeImage === imgUrl
                      ? 'border-accent ring-2 ring-accent/20'
                      : 'border-line opacity-75 hover:opacity-100'
                    }`}
                >
                  <img src={imgUrl} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Right: Structured Purchase Panel */}
        <div className="lg:col-span-6">
          <div className="rounded-2xl border border-line bg-surface p-6 shadow-card sm:p-7">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <Badge variant="accent">{product.categoryName}</Badge>

              {hasReviews ? (
                <div className="inline-flex items-center gap-1.5 text-sm font-semibold text-ink">
                  <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
                  <span dir="ltr">
                    {formatNumber(Number(product.averageRating), {
                      minimumFractionDigits: 1,
                      maximumFractionDigits: 1,
                    })}
                  </span>
                  <span className="text-ink-muted">
                    {t('product.reviewsCount', { count: formatNumber(product.reviewCount) })}
                  </span>
                </div>
              ) : (
                <span className="text-xs font-medium text-ink-muted">
                  {t('product.noReviewsYet')}
                </span>
              )}
            </div>

            <h1 dir="auto" className="mt-3 text-2xl font-extrabold text-ink sm:text-3xl">
              {product.name}
            </h1>

            {selectedVariant?.sku && (
              <p dir="ltr" className="mt-1 inline-block font-mono text-xs text-ink-muted">
                SKU: {selectedVariant.sku}
              </p>
            )}

            {/* Price & Currency Clarity */}
            <div className="mt-4 rounded-xl border border-line bg-subtle/50 p-4">
              <div className="flex flex-wrap items-baseline gap-3">
                <span dir="ltr" className="text-3xl font-extrabold text-ink">
                  {formatBasePrice(currentPrice)}
                </span>
                {isEstimatedDisplay && (
                  <span dir="ltr" className="text-sm font-semibold text-ink-secondary">
                    {t('product.approxNote', {
                      price: formatPrice(currentPrice),
                      currency: displayCurrency,
                    })}
                  </span>
                )}
              </div>
              {isEstimatedDisplay && (
                <p className="mt-1 text-xs text-ink-muted">
                  {t('product.billedInUsd', { price: formatBasePrice(currentPrice) })}
                </p>
              )}
            </div>

            <p dir="auto" className="mt-4 text-sm leading-relaxed text-ink-secondary sm:text-base">
              {product.description || t('product.defaultDesc')}
            </p>

            {/* Variants */}
            {product.variants.length > 0 && (
              <div className="mt-6">
                <h2 className="text-xs font-bold uppercase tracking-wider text-ink-secondary">
                  {t('product.selectOption')}
                </h2>
                <div className="mt-2.5 flex flex-wrap gap-2.5">
                  {product.variants.map((variant) => {
                    const isSelected = selectedVariant?.id === variant.id;
                    const label =
                      [variant.size, variant.color].filter(Boolean).join(' • ') || variant.sku;
                    return (
                      <button
                        type="button"
                        key={variant.id}
                        onClick={() => handleSelectVariant(variant)}
                        className={`rounded-xl border px-4 py-2.5 text-start text-sm transition ${isSelected
                            ? 'border-accent bg-accent-soft font-semibold text-accent ring-2 ring-accent/20 dark:bg-accent/15'
                            : 'border-line bg-surface text-ink hover:border-line-strong'
                          } ${variant.stockQuantity <= 0 ? 'opacity-55' : ''}`}
                      >
                        <div dir="auto">{label}</div>
                        <div dir="ltr" className="mt-0.5 text-xs text-ink-secondary">
                          {formatBasePrice(variant.price)} •{' '}
                          {variant.stockQuantity > 0
                            ? t('product.left', { count: formatNumber(variant.stockQuantity) })
                            : t('product.outOfStock')}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Quantity + Stock */}
            <div className="mt-6 flex flex-wrap items-center gap-4">
              <div className="inline-flex items-center rounded-xl border border-line bg-subtle/50">
                <button
                  type="button"
                  aria-label="Decrease quantity"
                  onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                  disabled={quantity <= 1}
                  className="p-3 text-ink-secondary hover:text-ink disabled:opacity-40"
                >
                  <Minus className="h-4 w-4" />
                </button>
                <span dir="ltr" className="w-10 text-center text-sm font-bold text-ink">
                  {formatNumber(quantity)}
                </span>
                <button
                  type="button"
                  aria-label="Increase quantity"
                  onClick={() => setQuantity((q) => Math.min(Math.max(1, currentStock), q + 1))}
                  disabled={quantity >= currentStock}
                  className="p-3 text-ink-secondary hover:text-ink disabled:opacity-40"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>

              <Badge variant={currentStock > 10 ? 'success' : currentStock > 0 ? 'warning' : 'danger'}>
                {currentStock > 0
                  ? t('product.inStockCount', { count: formatNumber(currentStock) })
                  : t('product.currentlyOutOfStock')}
              </Badge>
            </div>

            {/* Primary CTA */}
            <div className="mt-6 flex gap-3">
              <Button
                size="lg"
                fullWidth
                onClick={handleAddToCart}
                disabled={currentStock <= 0 || addToCartMutation.isPending || isAdmin}
              >
                <ShoppingCart className="h-5 w-5" />
                <span>
                  {addToCartMutation.isPending ? t('product.adding') : t('product.addToCart')}
                </span>
              </Button>

              {isAuthenticated && !isAdmin && (
                <Button
                  variant="outline"
                  size="lg"
                  aria-label={t('product.toggleWishlist')}
                  title={t('product.toggleWishlist')}
                  onClick={() => toggleWishlistMutation.mutate()}
                  className="shrink-0 px-4"
                >
                  <Heart className={`h-5 w-5 ${isFav ? 'fill-danger text-danger' : ''}`} />
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Reviews Section */}
      <section className="mt-12 border-t border-line pt-8">
        <h2 className="text-xl font-extrabold text-ink sm:text-2xl">
          {t('product.verifiedReviewsTitle', { count: formatNumber(product.reviewCount) })}
        </h2>

        {(eligibility?.eligible || editingReviewId !== null) && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              saveReviewMutation.mutate();
            }}
            className="mt-5 rounded-2xl border border-line bg-surface p-6 shadow-card"
          >
            <h3 className="text-base font-bold text-ink">
              {editingReviewId ? t('product.editReview') : t('product.writeReview')}
            </h3>
            <div className="mt-3 flex items-center gap-1">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  aria-label={`${star} stars`}
                  onClick={() => setRating(star)}
                  className="p-1"
                >
                  <Star
                    className={`h-6 w-6 ${star <= rating ? 'fill-amber-400 text-amber-400' : 'text-line-strong'
                      }`}
                  />
                </button>
              ))}
            </div>
            <input
              type="text"
              dir="auto"
              placeholder={t('product.headlinePlaceholder')}
              value={reviewTitle}
              onChange={(e) => setReviewTitle(e.target.value)}
              className={`${fieldBaseClass} mt-4`}
            />
            <textarea
              required
              minLength={5}
              rows={3}
              dir="auto"
              placeholder={t('product.commentPlaceholder')}
              value={reviewComment}
              onChange={(e) => setReviewComment(e.target.value)}
              className={`${fieldBaseClass} mt-3`}
            />
            <div className="mt-4 flex gap-2">
              <Button type="submit" disabled={saveReviewMutation.isPending}>
                {saveReviewMutation.isPending ? t('product.saving') : t('product.submitReview')}
              </Button>
              {editingReviewId && (
                <Button
                  variant="outline"
                  onClick={() => {
                    setEditingReviewId(null);
                    setReviewTitle('');
                    setReviewComment('');
                  }}
                >
                  {t('common.cancel')}
                </Button>
              )}
            </div>
          </form>
        )}

        <div className="mt-5 space-y-4">
          {reviews.length === 0 ? (
            <EmptyState
              compact
              icon={Star}
              title={t('product.noReviewsYet')}
              description={t('product.noReviews')}
            />
          ) : (
            reviews.map((review) => {
              const isOwner = user?.id === review.userId;
              return (
                <div
                  key={review.id}
                  className="rounded-2xl border border-line bg-surface p-5 shadow-card"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span dir="auto" className="font-bold text-ink">
                          {review.username}
                        </span>
                        {review.verifiedPurchase && (
                          <Badge variant="success">
                            <CheckCircle2 className="h-3 w-3" />
                            {t('product.verifiedBuyer')}
                          </Badge>
                        )}
                      </div>
                      <div className="mt-1.5 flex items-center gap-1">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <Star
                            key={i}
                            className={`h-4 w-4 ${i < review.rating ? 'fill-amber-400 text-amber-400' : 'text-line-strong'
                              }`}
                          />
                        ))}
                        <span dir="ltr" className="ms-2 text-xs text-ink-muted">
                          {formatDate(review.createdAt, locale)}
                        </span>
                      </div>
                    </div>

                    {(isOwner || isAdmin) && (
                      <div className="flex gap-1">
                        {isOwner && (
                          <button
                            type="button"
                            aria-label={t('common.edit')}
                            title={t('common.edit')}
                            onClick={() => {
                              setEditingReviewId(review.id);
                              setRating(review.rating);
                              setReviewTitle(review.title || '');
                              setReviewComment(review.comment);
                            }}
                            className="rounded-lg p-2 text-ink-muted hover:bg-subtle hover:text-ink"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                        )}
                        <button
                          type="button"
                          aria-label={t('common.delete')}
                          title={t('common.delete')}
                          onClick={() => setDeletingReviewId(review.id)}
                          className="rounded-lg p-2 text-ink-muted hover:bg-danger-soft hover:text-danger"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    )}
                  </div>

                  {review.title && (
                    <h4 dir="auto" className="mt-3 font-bold text-ink">
                      {review.title}
                    </h4>
                  )}
                  <p dir="auto" className="mt-1.5 text-sm leading-relaxed text-ink-secondary">
                    {review.comment}
                  </p>
                </div>
              );
            })
          )}
        </div>
      </section>

      {/* Image Lightbox Modal */}
      {lightboxOpen && activeImage && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
          onClick={() => setLightboxOpen(false)}
        >
          <button
            type="button"
            aria-label={t('common.close')}
            onClick={() => setLightboxOpen(false)}
            className="absolute end-4 top-4 rounded-full bg-surface/90 p-2 text-ink shadow-lg"
          >
            <X className="h-5 w-5" />
          </button>
          <img
            src={activeImage}
            alt={product.name}
            onClick={(e) => e.stopPropagation()}
            className="max-h-[85vh] max-w-full rounded-2xl object-contain shadow-2xl"
          />
        </div>
      )}

      <ConfirmDialog
        open={deletingReviewId !== null}
        title={t('common.delete')}
        description={t('product.reviewDeleted')}
        loading={deleteReviewMutation.isPending}
        onCancel={() => setDeletingReviewId(null)}
        onConfirm={() => {
          if (deletingReviewId !== null) deleteReviewMutation.mutate(deletingReviewId);
        }}
      />
    </div>
  );
}