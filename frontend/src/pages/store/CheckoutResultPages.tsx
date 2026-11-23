import { useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, XCircle } from 'lucide-react';
import { useI18n } from '../../context/I18nContext';

export function CheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const orderNumber = searchParams.get('orderNumber');
  const queryClient = useQueryClient();
  const { t } = useI18n();

  useEffect(() => {
    queryClient.invalidateQueries({ queryKey: ['cart'] });
    queryClient.invalidateQueries({ queryKey: ['my-orders'] });
  }, [queryClient]);

  return (
    <div className="mx-auto max-w-lg px-4 py-20 text-center">
      <CheckCircle2 className="mx-auto h-16 w-16 text-success" />
      <h1 className="mt-4 text-3xl font-extrabold text-ink">{t('checkout.successTitle')}</h1>
      {orderNumber && (
        <p dir="ltr" className="mt-2 inline-block font-mono text-sm font-bold text-accent">
          {t('checkout.orderNumber', { number: orderNumber })}
        </p>
      )}
      <p className="mt-3 text-sm text-ink-secondary">{t('checkout.successDesc')}</p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link
          to="/orders"
          className="rounded-xl bg-accent px-6 py-3 text-sm font-bold text-accent-fg hover:bg-accent-hover"
        >
          {t('checkout.trackOrders')}
        </Link>
        <Link
          to="/"
          className="rounded-xl border border-line bg-surface px-6 py-3 text-sm font-semibold text-ink hover:bg-subtle"
        >
          {t('checkout.continueShopping')}
        </Link>
      </div>
    </div>
  );
}

export function CheckoutCancelPage() {
  const { t } = useI18n();

  return (
    <div className="mx-auto max-w-lg px-4 py-20 text-center">
      <XCircle className="mx-auto h-16 w-16 text-danger" />
      <h1 className="mt-4 text-3xl font-extrabold text-ink">{t('checkout.cancelTitle')}</h1>
      <p className="mt-3 text-sm text-ink-secondary">{t('checkout.cancelDesc')}</p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link
          to="/orders"
          className="rounded-xl bg-accent px-6 py-3 text-sm font-bold text-accent-fg hover:bg-accent-hover"
        >
          {t('checkout.viewOrders')}
        </Link>
        <Link
          to="/"
          className="rounded-xl border border-line bg-surface px-6 py-3 text-sm font-semibold text-ink hover:bg-subtle"
        >
          {t('checkout.backShop')}
        </Link>
      </div>
    </div>
  );
}