import { Link } from 'react-router-dom';
import { Star, Heart, ArrowRight } from 'lucide-react';
import { ProductResponse } from '../types/api';
import { ProductImage } from './ui/ProductImageFallback';
import { Badge } from './ui/Badge';
import { useI18n } from '../context/I18nContext';
import { useCurrency } from '../context/CurrencyContext';
import { formatNumber } from '../lib/utils';

interface ProductCardProps {
  product: ProductResponse;
  isFav: boolean;
  showWishlistButton: boolean;
  onToggleWishlist: (productId: number) => void;
}

export function ProductCard({
  product,
  isFav,
  showWishlistButton,
  onToggleWishlist,
}: ProductCardProps) {
  const { t } = useI18n();
  const { formatPrice, isEstimatedDisplay, formatBasePrice } = useCurrency();

  const outOfStock = product.stockQuantity <= 0;
  const lowStock = product.stockQuantity > 0 && product.stockQuantity <= 10;
  const hasReviews = (product.reviewCount ?? 0) > 0;

  return (
    <article className="group relative flex flex-col overflow-hidden rounded-2xl border border-line bg-surface shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:border-line-strong hover:shadow-card-hover">
      {/* Product Image */}
      <Link
        to={`/products/${product.id}`}
        className="relative aspect-[4/3] overflow-hidden bg-subtle"
      >
        <ProductImage
          src={product.imageUrl}
          alt={product.name}
          className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
        />

        <div className="absolute start-3 top-3">
          {outOfStock ? (
            <Badge variant="danger" className="bg-surface/95 backdrop-blur">
              {t('shop.outOfStock')}
            </Badge>
          ) : lowStock ? (
            <Badge variant="warning" className="bg-surface/95 backdrop-blur">
              {t('shop.lowStockBadge', { count: formatNumber(product.stockQuantity) })}
            </Badge>
          ) : (
            <Badge variant="success" className="bg-surface/95 backdrop-blur">
              {t('shop.inStockBadge')}
            </Badge>
          )}
        </div>
      </Link>

      {/* Wishlist Toggle */}
      {showWishlistButton && (
        <button
          type="button"
          onClick={() => onToggleWishlist(product.id)}
          aria-label={t('product.toggleWishlist')}
          title={t('product.toggleWishlist')}
          className="absolute end-3 top-3 flex h-9 w-9 items-center justify-center rounded-full border border-line bg-surface/95 text-ink-secondary shadow-sm backdrop-blur transition hover:scale-105 hover:text-danger focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
        >
          <Heart className={`h-4 w-4 ${isFav ? 'fill-danger text-danger' : ''}`} />
        </button>
      )}

      {/* Card Body */}
      <div className="flex flex-1 flex-col p-4">
        <div className="flex items-center justify-between gap-2 text-xs">
          <span className="truncate rounded-md bg-subtle px-2 py-0.5 font-semibold text-ink-secondary">
            {product.categoryName}
          </span>

          {hasReviews ? (
            <span className="inline-flex shrink-0 items-center gap-1 font-semibold text-ink">
              <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
              <span dir="ltr">
                {formatNumber(Number(product.averageRating), {
                  minimumFractionDigits: 1,
                  maximumFractionDigits: 1,
                })}
              </span>
              <span className="text-ink-muted" dir="ltr">
                ({formatNumber(product.reviewCount)})
              </span>
            </span>
          ) : (
            <span className="shrink-0 text-[11px] font-medium text-ink-muted">
              {t('shop.noReviewsShort')}
            </span>
          )}
        </div>

        <Link
          to={`/products/${product.id}`}
          dir="auto"
          className="mt-2.5 line-clamp-2 min-h-[2.75rem] text-[15px] font-bold leading-snug text-ink transition-colors hover:text-accent"
        >
          {product.name}
        </Link>

        <p
          dir="auto"
          className="mt-1.5 line-clamp-2 flex-1 text-xs leading-relaxed text-ink-secondary"
        >
          {product.description || t('shop.defaultDesc')}
        </p>

        {/* Price Row + Full-Width Action */}
        <div className="mt-4 border-t border-line pt-3.5">
          <div className="flex items-baseline justify-between gap-2">
            <div>
              <span
                dir="ltr"
                className="inline-block whitespace-nowrap text-lg font-extrabold tracking-tight text-ink"
              >
                {formatPrice(product.price)}
              </span>
              {isEstimatedDisplay && (
                <span dir="ltr" className="ms-1.5 text-[11px] text-ink-muted">
                  ({formatBasePrice(product.price)})
                </span>
              )}
            </div>
            {product.variants?.length > 1 && (
              <span className="whitespace-nowrap rounded-md bg-subtle px-2 py-0.5 text-[11px] font-semibold text-ink-secondary">
                {t('shop.optionsCount', { count: formatNumber(product.variants.length) })}
              </span>
            )}
          </div>

          <Link
            to={`/products/${product.id}`}
            className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-accent px-4 py-2.5 text-xs font-bold text-accent-fg shadow-sm transition-colors hover:bg-accent-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
          >
            <span>{t('shop.viewOptions')}</span>
            <ArrowRight className="h-3.5 w-3.5 rtl:rotate-180" />
          </Link>
        </div>
      </div>
    </article>
  );
}