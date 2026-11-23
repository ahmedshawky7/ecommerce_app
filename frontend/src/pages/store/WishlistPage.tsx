import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Heart, Trash2, Star } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { ProductImage } from '../../components/ui/ProductImageFallback';
import { EmptyState } from '../../components/ui/EmptyState';
import { formatNumber, getApiErrorMessage } from '../../lib/utils';
import { WishlistItemResponse } from '../../types/api';

export function WishlistPage() {
  const queryClient = useQueryClient();
  const { t } = useI18n();
  const { formatPrice } = useCurrency();

  const { data: items = [], isLoading } = useQuery({
    queryKey: ['wishlist'],
    queryFn: async () => (await api.get<WishlistItemResponse[]>('/api/wishlist')).data,
  });

  const removeMutation = useMutation({
    mutationFn: async (productId: number) => api.delete(`/api/wishlist/${productId}`),
    onSuccess: () => {
      toast.success(t('wishlist.removed'));
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
      queryClient.invalidateQueries({ queryKey: ['wishlist-ids'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  return (
    <div className="w-full px-4 py-8 md:px-6">
      <h1 className="text-2xl font-extrabold text-ink">
        {t('wishlist.title', { count: formatNumber(items.length) })}
      </h1>

      {isLoading ? (
        <div className="mt-6 h-48 animate-pulse rounded-2xl bg-subtle" />
      ) : items.length === 0 ? (
        <div className="mt-8">
          <EmptyState
            icon={Heart}
            title={t('wishlist.empty')}
            action={
              <Link
                to="/"
                className="rounded-xl bg-accent px-5 py-2.5 text-sm font-bold text-accent-fg hover:bg-accent-hover"
              >
                {t('wishlist.discover')}
              </Link>
            }
          />
        </div>
      ) : (
        <div className="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {items.map((item) => {
            const hasReviews = (item.reviewCount ?? 0) > 0;
            return (
              <div
                key={item.id}
                className="group overflow-hidden rounded-2xl border border-line bg-surface shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:border-line-strong hover:shadow-card-hover"
              >
                <Link
                  to={`/products/${item.productId}`}
                  className="block aspect-[4/3] overflow-hidden bg-subtle"
                >
                  <ProductImage
                    src={item.imageUrl}
                    alt={item.productName}
                    className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
                  />
                </Link>
                <div className="p-4">
                  <div className="flex items-center justify-between text-xs text-ink-secondary">
                    <span className="rounded-md bg-subtle px-2 py-0.5 font-semibold">
                      {item.categoryName}
                    </span>
                    {hasReviews ? (
                      <span className="flex items-center gap-1 font-semibold text-ink">
                        <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                        <span dir="ltr">
                          {formatNumber(Number(item.averageRating), {
                            minimumFractionDigits: 1,
                            maximumFractionDigits: 1,
                          })}
                        </span>
                      </span>
                    ) : (
                      <span className="text-[11px] text-ink-muted">
                        {t('shop.noReviewsShort')}
                      </span>
                    )}
                  </div>
                  <Link
                    to={`/products/${item.productId}`}
                    dir="auto"
                    className="mt-2 line-clamp-2 min-h-[2.75rem] font-bold text-ink hover:text-accent"
                  >
                    {item.productName}
                  </Link>
                  <div className="mt-3 flex items-center justify-between border-t border-line pt-3">
                    <span dir="ltr" className="text-lg font-extrabold text-ink">
                      {formatPrice(item.price)}
                    </span>
                    <button
                      type="button"
                      onClick={() => removeMutation.mutate(item.productId)}
                      aria-label={t('common.delete')}
                      title={t('common.delete')}
                      className="rounded-lg p-2 text-danger hover:bg-danger-soft"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}