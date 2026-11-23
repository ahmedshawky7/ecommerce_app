import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Search,
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight,
  X,
  Truck,
  ShieldCheck,
  Award,
  Tag,
  Info,
  Layers,
  PackageCheck,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useAuth } from '../../context/AuthContext';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { ProductCard } from '../../components/ProductCard';
import { ProductCardSkeleton } from '../../components/ui/Skeletons';
import { EmptyState } from '../../components/ui/EmptyState';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { fieldBaseClass } from '../../components/ui/Input';
import { formatNumber, getApiErrorMessage } from '../../lib/utils';
import { CategoryResponse, PagedResponse, ProductResponse } from '../../types/api';

export function ShopPage() {
  const { isAuthenticated, isAdmin } = useAuth();
  const { t } = useI18n();
  const { displayCurrency, currencyConfig, isEstimatedDisplay } = useCurrency();
  const queryClient = useQueryClient();

  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [minRating, setMinRating] = useState('');
  const [inStockOnly, setInStockOnly] = useState(false);
  const [sort, setSort] = useState('createdAt,desc');
  const [page, setPage] = useState(0);
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setKeyword(searchInput);
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await api.get<CategoryResponse[]>('/api/categories')).data,
  });

  const { data: productsPage, isLoading } = useQuery({
    queryKey: [
      'products',
      { keyword, categoryId, minPrice, maxPrice, minRating, inStockOnly, sort, page },
    ],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (keyword.trim()) params.set('keyword', keyword.trim());
      if (categoryId !== null) params.set('categoryId', String(categoryId));
      if (minPrice) params.set('minPrice', minPrice);
      if (maxPrice) params.set('maxPrice', maxPrice);
      if (minRating) params.set('minRating', minRating);
      if (inStockOnly) params.set('inStockOnly', 'true');
      params.set('page', String(page));
      params.set('size', '16');
      params.set('sort', sort);
      return (await api.get<PagedResponse<ProductResponse>>(`/api/products/filter?${params}`)).data;
    },
  });

  const { data: wishlistIds = [] } = useQuery({
    queryKey: ['wishlist-ids'],
    queryFn: async () => (await api.get<number[]>('/api/wishlist/ids')).data,
    enabled: isAuthenticated && !isAdmin,
  });

  const toggleWishlistMutation = useMutation({
    mutationFn: async (productId: number) => {
      const isFav = wishlistIds.includes(productId);
      if (isFav) {
        await api.delete(`/api/wishlist/${productId}`);
        return { productId, added: false };
      } else {
        await api.post(`/api/wishlist/${productId}`);
        return { productId, added: true };
      }
    },
    onSuccess: ({ added }) => {
      toast.success(added ? t('shop.addedWishlist') : t('shop.removedWishlist'));
      queryClient.invalidateQueries({ queryKey: ['wishlist-ids'] });
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const resetFilters = () => {
    setSearchInput('');
    setKeyword('');
    setCategoryId(null);
    setMinPrice('');
    setMaxPrice('');
    setMinRating('');
    setInStockOnly(false);
    setSort('createdAt,desc');
    setPage(0);
  };

  const products = productsPage?.content ?? [];
  const totalPages = productsPage?.totalPages ?? 0;

  return (
    <div className="w-full px-4 py-6 md:px-6">
      {/* Balanced Architectural Hero Section */}
      <section className="mb-6 rounded-2xl border border-line bg-surface p-6 shadow-card sm:p-8 lg:p-10">
        <div className="grid grid-cols-1 items-center gap-8 lg:grid-cols-12">
          {/* Left: Headline, Value Prop & Prominent Search */}
          <div className="lg:col-span-8">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="accent">{t('shop.badge')}</Badge>
              <Badge variant="warning">
                <Tag className="h-3 w-3" />
                <span dir="auto">{t('shop.promoBanner')}</span>
              </Badge>
            </div>

            <h1 className="mt-4 text-2xl font-extrabold tracking-tight text-ink sm:text-3xl lg:text-4xl">
              {t('shop.heroTitle')}
            </h1>
            <p className="mt-2.5 max-w-2xl text-sm leading-relaxed text-ink-secondary sm:text-base">
              {t('shop.heroSubtitle')}
            </p>

            {/* Clear High-Contrast Search Bar */}
            <div className="mt-6 flex max-w-2xl items-center rounded-xl border border-line-strong bg-subtle/70 p-1.5 transition focus-within:border-accent focus-within:bg-surface focus-within:ring-2 focus-within:ring-accent/20">
              <Search className="ms-3 h-5 w-5 shrink-0 text-ink-muted" />
              <input
                type="search"
                aria-label={t('common.search')}
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder={t('shop.searchPlaceholder')}
                className="w-full bg-transparent px-3 py-2 text-sm text-ink placeholder:text-ink-muted focus:outline-none"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => setSearchInput('')}
                  aria-label={t('common.close')}
                  className="me-1 rounded-lg p-1.5 text-ink-muted hover:bg-subtle hover:text-ink"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>

            {/* Trust Row */}
            <div className="mt-5 flex flex-wrap gap-x-6 gap-y-2 text-xs font-medium text-ink-secondary">
              <span className="inline-flex items-center gap-1.5">
                <Truck className="h-4 w-4 text-success" />
                {t('shop.fastShippingBadge')}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Award className="h-4 w-4 text-warning" />
                {t('shop.verifiedReviewsBadge')}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <ShieldCheck className="h-4 w-4 text-accent" />
                {t('shop.securePayBadge')}
              </span>
            </div>
          </div>

          {/* Right: Live Storefront Summary Panel (Eliminates Empty Void) */}
          <div className="grid grid-cols-2 gap-3 lg:col-span-4 lg:grid-cols-1">
            <div className="rounded-xl border border-line bg-subtle/60 p-4">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-ink-muted">
                <Layers className="h-4 w-4 text-accent" />
                <span>{t('shop.highlightsTitle')}</span>
              </div>
              <p className="mt-2 text-lg font-extrabold text-ink">
                {t('shop.highlightCategories', { count: formatNumber(categories.length) })}
              </p>
              <p className="mt-0.5 text-xs text-ink-secondary">
                {t('shop.highlightCatalog', {
                  count: formatNumber(productsPage?.totalElements ?? 0),
                })}
              </p>
            </div>

            <div className="rounded-xl border border-line bg-subtle/60 p-4">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-ink-muted">
                <PackageCheck className="h-4 w-4 text-success" />
                <span>{t('common.usdBaseNote')}</span>
              </div>
              <p className="mt-2 text-xs leading-relaxed text-ink-secondary">
                {isEstimatedDisplay
                  ? t('shop.indicativeCurrencyNotice', {
                      currency: displayCurrency,
                      rate: formatNumber(currencyConfig.indicativeRate),
                    })
                  : t('shop.securePayBadge')}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Indicative Currency Banner (Visible on Mobile/All screens when non-USD) */}
      {isEstimatedDisplay && (
        <div className="mb-5 flex items-center gap-2 rounded-xl border border-accent/25 bg-accent-soft px-4 py-2.5 text-xs font-medium text-ink dark:bg-accent/10">
          <Info className="h-4 w-4 shrink-0 text-accent" />
          <span>
            {t('shop.indicativeCurrencyNotice', {
              currency: displayCurrency,
              rate: formatNumber(currencyConfig.indicativeRate),
            })}
          </span>
        </div>
      )}

      {/* Categories Bar */}
      <div className="mb-6 flex w-full items-center gap-2 overflow-x-auto pb-1">
        <button
          type="button"
          onClick={() => {
            setCategoryId(null);
            setPage(0);
          }}
          className={`whitespace-nowrap rounded-xl border px-4 py-2 text-sm font-semibold transition-colors ${
            categoryId === null
              ? 'border-accent bg-accent text-accent-fg shadow-sm'
              : 'border-line bg-surface text-ink-secondary hover:border-line-strong hover:text-ink'
          }`}
        >
          {t('shop.allCategories')}
        </button>
        {categories.map((cat) => (
          <button
            type="button"
            key={cat.id}
            onClick={() => {
              setCategoryId(cat.id);
              setPage(0);
            }}
            className={`whitespace-nowrap rounded-xl border px-4 py-2 text-sm font-semibold transition-colors ${
              categoryId === cat.id
                ? 'border-accent bg-accent text-accent-fg shadow-sm'
                : 'border-line bg-surface text-ink-secondary hover:border-line-strong hover:text-ink'
            }`}
          >
            <span>{cat.name}</span>{' '}
            <span dir="ltr" className="opacity-75">
              ({formatNumber(cat.productCount)})
            </span>
          </button>
        ))}
      </div>

      {/* Main Content: Compact Filter Sidebar + 4-Col Product Grid */}
      <div className="flex w-full flex-col gap-6 lg:flex-row">
        <aside
          className={`w-full shrink-0 lg:block lg:w-64 ${
            showMobileFilters ? 'block' : 'hidden'
          }`}
        >
          <div className="space-y-5 rounded-2xl border border-line bg-surface p-5 shadow-card">
            <div className="flex items-center justify-between border-b border-line pb-3">
              <h2 className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-ink">
                <SlidersHorizontal className="h-4 w-4 text-accent" />
                {t('shop.filters')}
              </h2>
              <button
                type="button"
                onClick={resetFilters}
                className="text-xs font-semibold text-accent hover:underline"
              >
                {t('shop.reset')}
              </button>
            </div>

            <div>
              <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-ink-secondary">
                {t('shop.priceRange')}
              </label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  min={0}
                  dir="ltr"
                  placeholder={t('shop.minPrice')}
                  value={minPrice}
                  onChange={(e) => {
                    setMinPrice(e.target.value);
                    setPage(0);
                  }}
                  className={fieldBaseClass}
                />
                <input
                  type="number"
                  min={0}
                  dir="ltr"
                  placeholder={t('shop.maxPrice')}
                  value={maxPrice}
                  onChange={(e) => {
                    setMaxPrice(e.target.value);
                    setPage(0);
                  }}
                  className={fieldBaseClass}
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="filter-rating"
                className="mb-2 block text-xs font-semibold uppercase tracking-wide text-ink-secondary"
              >
                {t('shop.minRating')}
              </label>
              <select
                id="filter-rating"
                value={minRating}
                onChange={(e) => {
                  setMinRating(e.target.value);
                  setPage(0);
                }}
                className={fieldBaseClass}
              >
                <option value="">{t('shop.anyRating')}</option>
                <option value="4">{t('shop.starsUp4')}</option>
                <option value="3">{t('shop.starsUp3')}</option>
                <option value="2">{t('shop.starsUp2')}</option>
              </select>
            </div>

            <label className="flex cursor-pointer items-center justify-between rounded-xl border border-line bg-subtle/50 p-3 transition-colors hover:border-line-strong">
              <span className="text-sm font-semibold text-ink">{t('shop.inStockOnly')}</span>
              <input
                type="checkbox"
                checked={inStockOnly}
                onChange={(e) => {
                  setInStockOnly(e.target.checked);
                  setPage(0);
                }}
                className="h-4 w-4 rounded border-line-strong text-accent focus:ring-accent"
              />
            </label>
          </div>
        </aside>

        {/* Products Area */}
        <div className="min-w-0 flex-1">
          <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowMobileFilters((v) => !v)}
                className="lg:hidden"
              >
                <SlidersHorizontal className="h-4 w-4" />
                <span>{t('shop.filters')}</span>
              </Button>
              <p className="text-sm text-ink-secondary">
                {t('shop.showingPrefix')}{' '}
                <span dir="ltr" className="font-bold text-ink">
                  {formatNumber(products.length)}
                </span>{' '}
                {t('shop.of')}{' '}
                <span dir="ltr" className="font-bold text-ink">
                  {formatNumber(productsPage?.totalElements ?? 0)}
                </span>{' '}
                {t('shop.productsSuffix')}
              </p>
            </div>

            <select
              aria-label="Sort products"
              value={sort}
              onChange={(e) => {
                setSort(e.target.value);
                setPage(0);
              }}
              className="rounded-xl border border-line bg-surface px-3.5 py-2 text-sm font-semibold text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20"
            >
              <option value="createdAt,desc">{t('shop.newest')}</option>
              <option value="price,asc">{t('shop.priceLowHigh')}</option>
              <option value="price,desc">{t('shop.priceHighLow')}</option>
              <option value="averageRating,desc">{t('shop.topRated')}</option>
            </select>
          </div>

          {isLoading ? (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {Array.from({ length: 8 }).map((_, idx) => (
                <ProductCardSkeleton key={idx} />
              ))}
            </div>
          ) : products.length === 0 ? (
            <EmptyState
              icon={Search}
              title={t('shop.noProductsTitle')}
              description={t('shop.noProductsDesc')}
              action={
                <Button onClick={resetFilters}>{t('shop.clearFilters')}</Button>
              }
            />
          ) : (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {products.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  isFav={wishlistIds.includes(product.id)}
                  showWishlistButton={isAuthenticated && !isAdmin}
                  onToggleWishlist={(productId) => toggleWishlistMutation.mutate(productId)}
                />
              ))}
            </div>
          )}

          {totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                <ChevronLeft className="h-4 w-4 rtl:rotate-180" />
                <span>{t('common.prevShort')}</span>
              </Button>
              <span className="px-3 text-sm font-semibold text-ink-secondary">
                {t('common.pageOf', {
                  page: formatNumber(page + 1),
                  total: formatNumber(totalPages),
                })}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                <span>{t('common.next')}</span>
                <ChevronRight className="h-4 w-4 rtl:rotate-180" />
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}