import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Package, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { ProductImage } from '../../components/ui/ProductImageFallback';
import { EmptyState } from '../../components/ui/EmptyState';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Button } from '../../components/ui/Button';
import {
  ORDER_STATUS_STYLES,
  formatDate,
  formatNumber,
  getApiErrorMessage,
} from '../../lib/utils';
import { OrderResponse, OrderStatus, PagedResponse } from '../../types/api';

const TIMELINE_STEPS: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPED',
  'DELIVERED',
];

export function OrdersPage() {
  const [page, setPage] = useState(0);
  const [cancellingOrder, setCancellingOrder] = useState<OrderResponse | null>(null);
  const queryClient = useQueryClient();
  const { t, locale } = useI18n();
  const { formatBasePrice } = useCurrency();

  const { data: ordersPage, isLoading } = useQuery({
    queryKey: ['my-orders', page],
    queryFn: async () =>
      (await api.get<PagedResponse<OrderResponse>>(`/api/orders?page=${page}&size=10`)).data,
  });

  const cancelMutation = useMutation({
    mutationFn: async (orderId: number) =>
      (await api.put<OrderResponse>(`/api/orders/${orderId}/cancel`)).data,
    onSuccess: (updated) => {
      toast.success(t('orders.cancelledMsg', { number: updated.orderNumber }));
      setCancellingOrder(null);
      queryClient.invalidateQueries({ queryKey: ['my-orders'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const orders = ordersPage?.content ?? [];

  return (
    <div className="w-full px-4 py-8 md:px-6">
      <h1 className="text-2xl font-extrabold text-ink">{t('orders.title')}</h1>
      <p className="mt-1 text-sm text-ink-secondary">{t('orders.subtitle')}</p>

      {isLoading ? (
        <div className="mt-6 space-y-4">
          <div className="h-44 animate-pulse rounded-2xl bg-subtle" />
          <div className="h-44 animate-pulse rounded-2xl bg-subtle" />
        </div>
      ) : orders.length === 0 ? (
        <div className="mt-8">
          <EmptyState icon={Package} title={t('orders.noOrders')} />
        </div>
      ) : (
        <div className="mt-6 space-y-5">
          {orders.map((order) => {
            const currentStep = TIMELINE_STEPS.indexOf(order.status);

            return (
              <div
                key={order.id}
                className="rounded-2xl border border-line bg-surface p-6 shadow-card"
              >
                <div className="flex flex-wrap items-center justify-between gap-4 border-b border-line pb-4">
                  <div>
                    <div className="flex flex-wrap items-center gap-3">
                      <span dir="ltr" className="font-mono text-base font-extrabold text-ink">
                        {order.orderNumber}
                      </span>
                      <span
                        className={`rounded-full border px-3 py-0.5 text-xs font-bold ${
                          ORDER_STATUS_STYLES[order.status]
                        }`}
                      >
                        {t(`status.${order.status}`)}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-ink-muted">
                      {t('orders.placedOn', {
                        date: formatDate(order.createdAt, locale),
                        method: order.paymentMethod,
                      })}
                    </p>
                  </div>

                  <div className="flex items-center gap-4">
                    <div className="text-end">
                      <p className="text-xs text-ink-muted">{t('orders.total')}</p>
                      <p dir="ltr" className="text-lg font-extrabold text-ink">
                        {formatBasePrice(order.totalAmount)}
                      </p>
                    </div>
                    {order.status === 'PENDING' && (
                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => setCancellingOrder(order)}
                      >
                        <XCircle className="h-4 w-4" />
                        <span>{t('orders.cancelOrder')}</span>
                      </Button>
                    )}
                  </div>
                </div>

                {order.status !== 'CANCELLED' && (
                  <div className="my-4 grid grid-cols-5 gap-2">
                    {TIMELINE_STEPS.map((step, idx) => {
                      const reached = currentStep >= idx;
                      return (
                        <div key={step} className="text-center">
                          <div
                            className={`h-1.5 rounded-full ${
                              reached ? 'bg-accent' : 'bg-subtle'
                            }`}
                          />
                          <span
                            className={`mt-1.5 block text-[10px] font-bold ${
                              reached ? 'text-accent' : 'text-ink-muted'
                            }`}
                          >
                            {t(`status.${step}`)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}

                <div className="mt-4 divide-y divide-line">
                  {order.items.map((item) => (
                    <div key={item.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                      <div className="flex items-center gap-3">
                        <div className="h-11 w-11 shrink-0 overflow-hidden rounded-lg bg-subtle">
                          <ProductImage
                            src={item.productImageUrl}
                            alt={item.productName}
                            iconClassName="h-5 w-5"
                          />
                        </div>
                        <div>
                          <p dir="auto" className="font-semibold text-ink">
                            {item.productName}
                          </p>
                          <p className="text-xs text-ink-secondary">
                            {item.variantLabel ? `${item.variantLabel} • ` : ''}
                            {t('orders.qty', {
                              qty: formatNumber(item.quantity),
                              price: formatBasePrice(item.unitPrice),
                            })}
                          </p>
                        </div>
                      </div>
                      <span dir="ltr" className="font-bold text-ink">
                        {formatBasePrice(item.subtotal)}
                      </span>
                    </div>
                  ))}
                </div>

                <div className="mt-4 flex flex-wrap gap-x-6 gap-y-1 border-t border-line pt-3 text-xs text-ink-secondary">
                  <span>{t('orders.subtotal', { amount: formatBasePrice(order.subtotalAmount) })}</span>
                  {order.discountAmount > 0 && (
                    <span className="text-success">
                      {t('orders.discount', {
                        code: order.couponCode || '',
                        amount: formatBasePrice(order.discountAmount),
                      })}
                    </span>
                  )}
                  <span>{t('orders.shipping', { amount: formatBasePrice(order.shippingAmount) })}</span>
                  <span>{t('orders.tax', { amount: formatBasePrice(order.taxAmount) })}</span>
                  <span dir="auto" className="ms-auto">
                    {t('orders.shipTo', { address: order.shippingAddress })}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {(ordersPage?.totalPages ?? 0) > 1 && (
        <div className="mt-6 flex justify-center gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            {t('common.previous')}
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= (ordersPage?.totalPages ?? 0)}
            onClick={() => setPage((p) => p + 1)}
          >
            {t('common.next')}
          </Button>
        </div>
      )}

      <ConfirmDialog
        open={cancellingOrder !== null}
        title={t('orders.confirmCancelTitle')}
        description={t('orders.confirmCancelDesc', {
          number: cancellingOrder?.orderNumber ?? '',
        })}
        loading={cancelMutation.isPending}
        onCancel={() => setCancellingOrder(null)}
        onConfirm={() => {
          if (cancellingOrder) cancelMutation.mutate(cancellingOrder.id);
        }}
      />
    </div>
  );
}