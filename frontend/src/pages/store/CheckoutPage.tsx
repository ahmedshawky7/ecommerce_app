import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CreditCard, Info, MapPin, ShieldCheck, Truck } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { Button } from '../../components/ui/Button';
import { fieldBaseClass } from '../../components/ui/Input';
import { formatNumber, generateIdempotencyKey, getApiErrorMessage } from '../../lib/utils';
import {
  AddressResponse,
  CartResponse,
  CheckoutResponse,
  CheckoutSessionResponse,
  PricingBreakdownResponse,
} from '../../types/api';

export function CheckoutPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { t } = useI18n();
  const { formatPrice, formatBasePrice, isEstimatedDisplay, displayCurrency } = useCurrency();

  const initialCoupon = (location.state as { couponCode?: string } | null)?.couponCode || '';
  const idempotencyKeyRef = useRef<string>(generateIdempotencyKey());

  const [shippingAddress, setShippingAddress] = useState('');
  const [phone, setPhone] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<'STRIPE' | 'CASH_ON_DELIVERY'>('STRIPE');
  const [couponCode, setCouponCode] = useState(initialCoupon);
  const [validatedPricing, setValidatedPricing] = useState<PricingBreakdownResponse | null>(null);

  const { data: cart } = useQuery({
    queryKey: ['cart'],
    queryFn: async () => (await api.get<CartResponse>('/api/cart')).data,
  });

  const { data: addresses = [] } = useQuery({
    queryKey: ['addresses'],
    queryFn: async () => (await api.get<AddressResponse[]>('/api/addresses')).data,
  });

  const { data: basePricing } = useQuery({
    queryKey: ['pricing-preview', cart?.totalPrice],
    queryFn: async () =>
      (await api.get<PricingBreakdownResponse>('/api/coupons/pricing-preview')).data,
    enabled: Boolean(cart && cart.items.length > 0),
  });

  useEffect(() => {
    if (addresses.length > 0 && !shippingAddress) {
      const def = addresses.find((a) => a.isDefault) || addresses[0];
      setShippingAddress(
        `${def.streetAddress}, ${def.city}${def.state ? `, ${def.state}` : ''}, ${def.country}`
      );
      setPhone(def.phone);
    }
  }, [addresses, shippingAddress]);

  useEffect(() => {
    if (initialCoupon && cart && cart.items.length > 0) {
      api
        .post<PricingBreakdownResponse>('/api/coupons/validate', { code: initialCoupon })
        .then((res) => setValidatedPricing(res.data))
        .catch(() => setCouponCode(''));
    }
  }, [initialCoupon, cart]);

  const applyCouponMutation = useMutation({
    mutationFn: async (code: string) =>
      (await api.post<PricingBreakdownResponse>('/api/coupons/validate', { code })).data,
    onSuccess: (data) => {
      setValidatedPricing(data);
      setCouponCode(data.appliedCouponCode || '');
      toast.success(t('checkout.couponApplied'));
    },
    onError: (err) => {
      setValidatedPricing(null);
      toast.error(getApiErrorMessage(err, t('common.errorFallback')));
    },
  });

  const placeOrderMutation = useMutation({
    mutationFn: async () => {
      const payload = {
        shippingAddress,
        phone,
        paymentMethod,
        couponCode: validatedPricing?.appliedCouponCode || undefined,
      };
      const headers = { 'Idempotency-Key': idempotencyKeyRef.current };

      if (paymentMethod === 'STRIPE') {
        const origin = window.location.origin;
        const successUrl = `${origin}/checkout/success`;
        const cancelUrl = `${origin}/checkout/cancel`;
        const res = await api.post<CheckoutSessionResponse>(
          `/api/orders/create-checkout-session?successUrl=${encodeURIComponent(
            successUrl
          )}&cancelUrl=${encodeURIComponent(cancelUrl)}`,
          payload,
          { headers }
        );
        return { type: 'STRIPE' as const, data: res.data };
      }

      const res = await api.post<CheckoutResponse>('/api/orders/checkout', payload, { headers });
      return { type: 'COD' as const, data: res.data };
    },
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      queryClient.invalidateQueries({ queryKey: ['my-orders'] });

      if (result.type === 'STRIPE') {
        if (result.data.sessionUrl && result.data.sessionUrl.startsWith('http')) {
          window.location.href = result.data.sessionUrl;
        } else {
          navigate(`/checkout/success?orderNumber=${result.data.orderNumber}`);
        }
      } else {
        navigate(`/checkout/success?orderNumber=${result.data.orderNumber}`);
      }
    },
    onError: (err) => {
      idempotencyKeyRef.current = generateIdempotencyKey();
      toast.error(getApiErrorMessage(err, t('common.errorFallback')));
    },
  });

  const pricing = validatedPricing ?? basePricing;
  const totalUsd = pricing?.totalAmount ?? cart?.totalPrice ?? 0;

  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p className="text-lg font-bold text-ink">{t('checkout.emptyCart')}</p>
        <Link to="/" className="mt-4 inline-block font-semibold text-accent hover:underline">
          {t('checkout.returnShop')}
        </Link>
      </div>
    );
  }

  return (
    <div className="w-full px-4 py-8 md:px-6">
      <h1 className="text-2xl font-extrabold text-ink">{t('checkout.title')}</h1>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          placeOrderMutation.mutate();
        }}
        className="mt-6 grid grid-cols-1 gap-8 lg:grid-cols-12"
      >
        <div className="space-y-6 lg:col-span-8">
          {addresses.length > 0 && (
            <div className="rounded-2xl border border-line bg-surface p-6 shadow-card">
              <div className="flex items-center justify-between">
                <h2 className="flex items-center gap-2 text-base font-bold text-ink">
                  <MapPin className="h-4 w-4 text-accent" />
                  {t('checkout.savedAddresses')}
                </h2>
                <Link to="/addresses" className="text-xs font-semibold text-accent hover:underline">
                  {t('checkout.manage')}
                </Link>
              </div>
              <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
                {addresses.map((addr) => {
                  const formatted = `${addr.streetAddress}, ${addr.city}${
                    addr.state ? `, ${addr.state}` : ''
                  }, ${addr.country}`;
                  const active = shippingAddress === formatted;
                  return (
                    <button
                      type="button"
                      key={addr.id}
                      onClick={() => {
                        setShippingAddress(formatted);
                        setPhone(addr.phone);
                      }}
                      className={`rounded-xl border p-3.5 text-start text-sm transition ${
                        active
                          ? 'border-accent bg-accent-soft ring-2 ring-accent/20 dark:bg-accent/15'
                          : 'border-line hover:border-line-strong'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-ink">{addr.label}</span>
                        {addr.isDefault && (
                          <span className="rounded bg-ink px-2 py-0.5 text-[10px] font-bold text-surface">
                            {t('checkout.defaultBadge')}
                          </span>
                        )}
                      </div>
                      <p className="mt-1 text-xs text-ink-secondary">{addr.fullName}</p>
                      <p className="mt-0.5 text-xs text-ink-muted">{formatted}</p>
                      <p dir="ltr" className="mt-0.5 inline-block text-xs text-ink-muted">
                        {addr.phone}
                      </p>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div className="space-y-4 rounded-2xl border border-line bg-surface p-6 shadow-card">
            <h2 className="text-base font-bold text-ink">{t('checkout.shippingDetails')}</h2>
            <div>
              <label className="mb-1.5 block text-sm font-semibold text-ink-secondary">
                {t('checkout.fullAddress')}
              </label>
              <textarea
                required
                rows={2}
                dir="auto"
                value={shippingAddress}
                onChange={(e) => setShippingAddress(e.target.value)}
                placeholder={t('checkout.addressPlaceholder')}
                className={fieldBaseClass}
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-semibold text-ink-secondary">
                {t('checkout.phone')}
              </label>
              <input
                type="tel"
                required
                dir="ltr"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 555 123 4567"
                className={fieldBaseClass}
              />
            </div>
          </div>

          <div className="rounded-2xl border border-line bg-surface p-6 shadow-card">
            <h2 className="text-base font-bold text-ink">{t('checkout.paymentMethod')}</h2>
            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
              <button
                type="button"
                onClick={() => setPaymentMethod('STRIPE')}
                className={`flex items-center gap-3 rounded-xl border p-4 text-start transition ${
                  paymentMethod === 'STRIPE'
                    ? 'border-accent bg-accent-soft ring-2 ring-accent/20 dark:bg-accent/15'
                    : 'border-line hover:border-line-strong'
                }`}
              >
                <CreditCard className="h-5 w-5 shrink-0 text-accent" />
                <div>
                  <p className="text-sm font-bold text-ink">{t('checkout.stripeTitle')}</p>
                  <p className="text-xs text-ink-secondary">{t('checkout.stripeDesc')}</p>
                </div>
              </button>

              <button
                type="button"
                onClick={() => setPaymentMethod('CASH_ON_DELIVERY')}
                className={`flex items-center gap-3 rounded-xl border p-4 text-start transition ${
                  paymentMethod === 'CASH_ON_DELIVERY'
                    ? 'border-accent bg-accent-soft ring-2 ring-accent/20 dark:bg-accent/15'
                    : 'border-line hover:border-line-strong'
                }`}
              >
                <Truck className="h-5 w-5 shrink-0 text-success" />
                <div>
                  <p className="text-sm font-bold text-ink">{t('checkout.codTitle')}</p>
                  <p className="text-xs text-ink-secondary">{t('checkout.codDesc')}</p>
                </div>
              </button>
            </div>
          </div>
        </div>

        {/* Authoritative Summary */}
        <div className="h-fit rounded-2xl border border-line bg-surface p-6 shadow-card lg:col-span-4">
          <h2 className="text-lg font-bold text-ink">{t('checkout.finalSummary')}</h2>

          <div className="mt-4 max-h-48 space-y-2 overflow-y-auto border-b border-line pb-4">
            {cart.items.map((item) => (
              <div key={item.id} className="flex justify-between gap-2 text-xs">
                <span dir="auto" className="text-ink-secondary">
                  {item.productName} × {formatNumber(item.quantity)}
                </span>
                <span dir="ltr" className="shrink-0 font-semibold text-ink">
                  {formatBasePrice(item.subtotal)}
                </span>
              </div>
            ))}
          </div>

          <div className="mt-4 flex gap-2">
            <input
              type="text"
              dir="ltr"
              value={couponCode}
              onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
              placeholder={t('checkout.couponPlaceholder')}
              className={`${fieldBaseClass} uppercase`}
            />
            <Button
              variant="secondary"
              onClick={() => couponCode.trim() && applyCouponMutation.mutate(couponCode.trim())}
              disabled={applyCouponMutation.isPending}
            >
              {t('common.apply')}
            </Button>
          </div>

          <div className="mt-5 space-y-2.5 text-sm">
            <div className="flex justify-between text-ink-secondary">
              <span>{t('cart.subtotal')}</span>
              <span dir="ltr">{formatBasePrice(pricing?.subtotal ?? cart.totalPrice)}</span>
            </div>
            {(pricing?.discountAmount ?? 0) > 0 && (
              <div className="flex justify-between font-semibold text-success">
                <span>{t('checkout.discountWithCode', { code: pricing?.appliedCouponCode || '' })}</span>
                <span dir="ltr">-{formatBasePrice(pricing?.discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between text-ink-secondary">
              <span>{t('cart.shipping')}</span>
              <span dir="ltr">
                {pricing?.shippingAmount === 0
                  ? t('cart.free')
                  : formatBasePrice(pricing?.shippingAmount ?? 0)}
              </span>
            </div>
            <div className="flex justify-between text-ink-secondary">
              <span>{t('checkout.tax')}</span>
              <span dir="ltr">{formatBasePrice(pricing?.taxAmount ?? 0)}</span>
            </div>
            <div className="flex justify-between border-t border-line pt-3 text-base font-extrabold text-ink">
              <span>{t('cart.total')}</span>
              <span dir="ltr">{formatBasePrice(totalUsd)}</span>
            </div>
          </div>

          {isEstimatedDisplay && (
            <div className="mt-4 flex items-start gap-2 rounded-xl border border-warning/30 bg-warning-soft p-3 text-xs text-ink dark:bg-warning/10">
              <Info className="mt-0.5 h-4 w-4 shrink-0 text-warning" />
              <div>
                <p>{t('checkout.paymentCurrencyWarning', { currency: displayCurrency, usdAmount: formatBasePrice(totalUsd) })}</p>
                <p dir="ltr" className="mt-1 font-semibold text-ink-secondary">
                  ≈ {formatPrice(totalUsd)}
                </p>
              </div>
            </div>
          )}

          <Button
            type="submit"
            fullWidth
            size="lg"
            disabled={placeOrderMutation.isPending}
            className="mt-6"
          >
            <ShieldCheck className="h-4 w-4" />
            <span>
              {placeOrderMutation.isPending
                ? t('checkout.processing')
                : paymentMethod === 'STRIPE'
                ? t('checkout.payStripe', { amount: formatBasePrice(totalUsd) })
                : t('checkout.placeOrder', { amount: formatBasePrice(totalUsd) })}
            </span>
          </Button>
        </div>
      </form>
    </div>
  );
}