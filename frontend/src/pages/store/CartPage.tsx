import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Minus, Plus, ShoppingBag, Tag, Trash2, ArrowRight, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { ProductImage } from '../../components/ui/ProductImageFallback';
import { EmptyState } from '../../components/ui/EmptyState';
import { Button } from '../../components/ui/Button';
import { fieldBaseClass } from '../../components/ui/Input';
import { formatNumber, getApiErrorMessage } from '../../lib/utils';
import { CartResponse, PricingBreakdownResponse } from '../../types/api';

export function CartPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { t } = useI18n();
  const { formatPrice, formatBasePrice, isEstimatedDisplay } = useCurrency();

  const [couponInput, setCouponInput] = useState('');
  const [appliedPricing, setAppliedPricing] = useState<PricingBreakdownResponse | null>(null);

  const { data: cart, isLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: async () => (await api.get<CartResponse>('/api/cart')).data,
  });

  const { data: defaultPricing } = useQuery({
    queryKey: ['pricing-preview', cart?.totalPrice],
    queryFn: async () =>
      (await api.get<PricingBreakdownResponse>('/api/coupons/pricing-preview')).data,
    enabled: Boolean(cart && cart.items.length > 0),
  });

  const updateQtyMutation = useMutation({
    mutationFn: async ({ itemId, quantity }: { itemId: number; quantity: number }) =>
      (await api.put<CartResponse>(`/api/cart/items/${itemId}`, { quantity })).data,
    onSuccess: (updated) => {
      queryClient.setQueryData(['cart'], updated);
      setAppliedPricing(null);
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const removeItemMutation = useMutation({
    mutationFn: async (itemId: number) =>
      (await api.delete<CartResponse>(`/api/cart/items/${itemId}`)).data,
    onSuccess: (updated) => {
      queryClient.setQueryData(['cart'], updated);
      setAppliedPricing(null);
      toast.success(t('cart.itemRemoved'));
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const validateCouponMutation = useMutation({
    mutationFn: async (code: string) =>
      (await api.post<PricingBreakdownResponse>('/api/coupons/validate', { code })).data,
    onSuccess: (pricing) => {
      setAppliedPricing(pricing);
      toast.success(t('cart.couponApplied', { code: pricing.appliedCouponCode || '' }));
    },
    onError: (err) => {
      setAppliedPricing(null);
      toast.error(getApiErrorMessage(err, t('common.errorFallback')));
    },
  });

  const pricing = appliedPricing ?? defaultPricing;

  if (isLoading) {
    return (
      <div className="w-full px-4 py-10 md:px-6">
        <div className="h-64 animate-pulse rounded-2xl bg-subtle" />
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16">
        <EmptyState
          icon={ShoppingBag}
          title={t('cart.emptyTitle')}
          description={t('cart.emptyDesc')}
          action={
            <Link
              to="/"
              className="inline-flex items-center rounded-xl bg-accent px-5 py-2.5 text-sm font-bold text-accent-fg hover:bg-accent-hover"
            >
              {t('cart.browseProducts')}
            </Link>
          }
        />
      </div>
    );
  }

  const totalUsd = pricing?.totalAmount ?? cart.totalPrice;

  return (
    <div className="w-full px-4 py-8 md:px-6">
      <h1 className="text-2xl font-extrabold text-ink">
        {t('cart.shoppingCart', { count: formatNumber(cart.totalItems) })}
      </h1>

      <div className="mt-6 grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="space-y-4 lg:col-span-8">
          {cart.items.map((item) => (
            <div
              key={item.id}
              className="flex flex-col gap-4 rounded-2xl border border-line bg-surface p-4 shadow-card sm:flex-row sm:items-center"
            >
              <div className="h-24 w-24 shrink-0 overflow-hidden rounded-xl bg-subtle">
                <ProductImage src={item.productImageUrl} alt={item.productName} />
              </div>

              <div className="min-w-0 flex-1">
                <Link
                  to={`/products/${item.productId}`}
                  dir="auto"
                  className="font-bold text-ink hover:text-accent"
                >
                  {item.productName}
                </Link>
                <p className="mt-0.5 text-xs text-ink-secondary">
                  {[item.size, item.color].filter(Boolean).join(' • ')}{' '}
                  <span dir="ltr" className="inline-block font-mono text-ink-muted">
                    ({item.sku})
                  </span>
                </p>
                <p className="mt-1 text-sm font-semibold text-ink-secondary">
                  {t('cart.each', { price: formatPrice(item.unitPrice) })}
                </p>
              </div>

              <div className="flex items-center justify-between gap-4 sm:justify-end">
                <div className="inline-flex items-center rounded-xl border border-line bg-subtle/40">
                  <button
                    type="button"
                    aria-label="Decrease quantity"
                    onClick={() =>
                      updateQtyMutation.mutate({ itemId: item.id, quantity: item.quantity - 1 })
                    }
                    disabled={item.quantity <= 1 || updateQtyMutation.isPending}
                    className="p-2 text-ink-secondary hover:text-ink disabled:opacity-40"
                  >
                    <Minus className="h-4 w-4" />
                  </button>
                  <span dir="ltr" className="w-8 text-center text-sm font-bold text-ink">
                    {formatNumber(item.quantity)}
                  </span>
                  <button
                    type="button"
                    aria-label="Increase quantity"
                    onClick={() =>
                      updateQtyMutation.mutate({ itemId: item.id, quantity: item.quantity + 1 })
                    }
                    disabled={item.quantity >= item.availableStock || updateQtyMutation.isPending}
                    className="p-2 text-ink-secondary hover:text-ink disabled:opacity-40"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>

                <div dir="ltr" className="w-28 text-end font-extrabold text-ink">
                  {formatPrice(item.subtotal)}
                </div>

                <button
                  type="button"
                  onClick={() => removeItemMutation.mutate(item.id)}
                  aria-label={t('cart.removeItem')}
                  title={t('cart.removeItem')}
                  className="rounded-lg p-2 text-ink-muted hover:bg-danger-soft hover:text-danger"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Summary */}
        <div className="h-fit rounded-2xl border border-line bg-surface p-6 shadow-card lg:col-span-4">
          <h2 className="text-lg font-bold text-ink">{t('cart.orderSummary')}</h2>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (couponInput.trim()) validateCouponMutation.mutate(couponInput.trim());
            }}
            className="mt-4"
          >
            <label className="text-xs font-semibold uppercase tracking-wide text-ink-secondary">
              {t('cart.promoCode')}
            </label>
            <div className="mt-1.5 flex gap-2">
              <div className="relative flex-1">
                <Tag className="absolute start-3 top-3 h-4 w-4 text-ink-muted" />
                <input
                  type="text"
                  dir="ltr"
                  value={couponInput}
                  onChange={(e) => setCouponInput(e.target.value.toUpperCase())}
                  placeholder="SAVE20"
                  className={`${fieldBaseClass} ps-9 uppercase`}
                />
              </div>
              <Button
                type="submit"
                variant="secondary"
                disabled={validateCouponMutation.isPending || !couponInput.trim()}
              >
                {t('common.apply')}
              </Button>
            </div>
            {appliedPricing?.appliedCouponCode && (
              <div className="mt-2 flex items-center justify-between rounded-lg bg-success-soft px-3 py-1.5 text-xs font-semibold text-success dark:bg-success/15">
                <span dir="ltr">{t('cart.applied', { code: appliedPricing.appliedCouponCode })}</span>
                <button
                  type="button"
                  aria-label={t('common.close')}
                  onClick={() => {
                    setAppliedPricing(null);
                    setCouponInput('');
                  }}
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            )}
          </form>

          <div className="mt-6 space-y-3 border-t border-line pt-4 text-sm">
            <div className="flex justify-between text-ink-secondary">
              <span>{t('cart.subtotal')}</span>
              <span dir="ltr">{formatPrice(pricing?.subtotal ?? cart.totalPrice)}</span>
            </div>
            {(pricing?.discountAmount ?? 0) > 0 && (
              <div className="flex justify-between font-semibold text-success">
                <span>{t('cart.discount')}</span>
                <span dir="ltr">-{formatPrice(pricing?.discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between text-ink-secondary">
              <span>{t('cart.shipping')}</span>
              <span dir="ltr">
                {pricing?.shippingAmount === 0
                  ? t('cart.free')
                  : formatPrice(pricing?.shippingAmount ?? 0)}
              </span>
            </div>
            <div className="flex justify-between text-ink-secondary">
              <span>{t('cart.estimatedTax')}</span>
              <span dir="ltr">{formatPrice(pricing?.taxAmount ?? 0)}</span>
            </div>
            <div className="flex justify-between border-t border-line pt-3 text-base font-extrabold text-ink">
              <span>{t('cart.total')}</span>
              <span dir="ltr">{formatPrice(totalUsd)}</span>
            </div>
            {isEstimatedDisplay && (
              <p className="text-xs text-ink-muted">
                {t('cart.billingUsdNotice', { amount: formatBasePrice(totalUsd) })}
              </p>
            )}
          </div>

          <Button
            fullWidth
            size="lg"
            className="mt-6"
            onClick={() =>
              navigate('/checkout', {
                state: { couponCode: appliedPricing?.appliedCouponCode || '' },
              })
            }
          >
            <span>{t('cart.proceedCheckout')}</span>
            <ArrowRight className="h-4 w-4 rtl:rotate-180" />
          </Button>
        </div>
      </div>
    </div>
  );
}