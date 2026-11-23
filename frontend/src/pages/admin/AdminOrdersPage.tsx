import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Search, ShoppingBag } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { TableRowsSkeleton } from '../../components/ui/Skeletons';
import { Button } from '../../components/ui/Button';
import { EmptyState } from '../../components/ui/EmptyState';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { fieldBaseClass } from '../../components/ui/Input';
import {
  ORDER_STATUS_STYLES,
  formatDate,
  formatNumber,
  getApiErrorMessage,
} from '../../lib/utils';
import { AdminOrderResponse, OrderStatus, PagedResponse } from '../../types/api';

const NEXT_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
};

export function AdminOrdersPage() {
  const queryClient = useQueryClient();
  const { t, locale } = useI18n();
  const { formatBasePrice } = useCurrency();
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [confirmCancelOrder, setConfirmCancelOrder] = useState<AdminOrderResponse | null>(null);

  const { data: ordersPage, isLoading } = useQuery({
    queryKey: ['admin-orders', statusFilter, keyword, page],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (statusFilter) params.set('status', statusFilter);
      if (keyword.trim()) params.set('keyword', keyword.trim());
      params.set('page', String(page));
      params.set('size', '10');
      return (await api.get<PagedResponse<AdminOrderResponse>>(`/api/admin/orders?${params}`)).data;
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: number; status: OrderStatus }) =>
      (await api.put(`/api/orders/admin/${id}/status`, { status })).data,
    onSuccess: () => {
      toast.success(t('admin.orderUpdated'));
      setConfirmCancelOrder(null);
      queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      queryClient.invalidateQueries({ queryKey: ['admin-products'] });
      queryClient.invalidateQueries({ queryKey: ['admin-low-stock'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const orders = ordersPage?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-ink">{t('admin.ordersTitle')}</h1>
          <p className="mt-1 text-sm text-ink-secondary">{t('admin.ordersSubtitle')}</p>
        </div>

        <div className="flex flex-wrap gap-2">
          <div className="relative">
            <Search className="absolute start-3 top-3 h-4 w-4 text-ink-muted" />
            <input
              type="search"
              dir="ltr"
              placeholder={t('admin.orderSearchPlaceholder')}
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPage(0);
              }}
              className={`${fieldBaseClass} w-48 ps-9`}
            />
          </div>

          <select
            aria-label={t('common.status')}
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className={`${fieldBaseClass} w-auto`}
          >
            <option value="">{t('admin.allStatuses')}</option>
            {(Object.keys(ORDER_STATUS_STYLES) as OrderStatus[]).map((st) => (
              <option key={st} value={st}>
                {t(`status.${st}`)}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-line bg-surface shadow-card">
        <table className="w-full text-start text-sm">
          <thead className="border-b border-line bg-subtle/70 text-xs uppercase text-ink-secondary">
            <tr>
              <th className="px-5 py-3.5 text-start">{t('admin.orderCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.customerCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.itemsCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.totalCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('common.status')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.nextActions')}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {isLoading ? (
              <TableRowsSkeleton cols={6} rows={6} />
            ) : orders.length === 0 ? (
              <tr>
                <td colSpan={6} className="p-6">
                  <EmptyState compact icon={ShoppingBag} title={t('admin.noOrdersMatch')} />
                </td>
              </tr>
            ) : (
              orders.map((order) => (
                <tr key={order.id} className="transition-colors hover:bg-subtle/50">
                  <td className="px-5 py-3.5">
                    <p dir="ltr" className="inline-block font-mono font-bold text-ink">
                      {order.orderNumber}
                    </p>
                    <p dir="ltr" className="text-xs text-ink-muted">
                      {formatDate(order.createdAt, locale)}
                    </p>
                  </td>
                  <td className="px-5 py-3.5">
                    <p dir="auto" className="font-semibold text-ink">
                      {order.customerName}
                    </p>
                    <p dir="ltr" className="inline-block text-xs text-ink-muted">
                      {order.customerEmail}
                    </p>
                  </td>
                  <td dir="auto" className="max-w-xs px-5 py-3.5 text-xs text-ink-secondary">
                    {order.items
                      .map((i) => `${i.productName} ×${formatNumber(i.quantity)}`)
                      .join(', ')}
                  </td>
                  <td className="px-5 py-3.5">
                    <p dir="ltr" className="font-bold text-ink">
                      {formatBasePrice(order.totalAmount)}
                    </p>
                    <p className="text-[11px] text-ink-muted">{order.paymentMethod}</p>
                  </td>
                  <td className="px-5 py-3.5">
                    <span
                      className={`rounded-full border px-2.5 py-0.5 text-xs font-bold ${
                        ORDER_STATUS_STYLES[order.status]
                      }`}
                    >
                      {t(`status.${order.status}`)}
                    </span>
                  </td>
                  <td className="px-5 py-3.5">
                    <div className="flex flex-wrap gap-1.5">
                      {NEXT_TRANSITIONS[order.status].map((nextStatus) => (
                        <button
                          type="button"
                          key={nextStatus}
                          disabled={updateStatusMutation.isPending}
                          onClick={() => {
                            if (nextStatus === 'CANCELLED') {
                              setConfirmCancelOrder(order);
                            } else {
                              updateStatusMutation.mutate({ id: order.id, status: nextStatus });
                            }
                          }}
                          className={`rounded-lg px-2.5 py-1 text-xs font-bold transition ${
                            nextStatus === 'CANCELLED'
                              ? 'bg-danger-soft text-danger hover:opacity-85 dark:bg-danger/15'
                              : 'bg-accent-soft text-accent hover:bg-accent hover:text-accent-fg dark:bg-accent/15'
                          }`}
                        >
                          {t(`status.${nextStatus}`)}
                        </button>
                      ))}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {(ordersPage?.totalPages ?? 0) > 1 && (
        <div className="flex justify-end gap-2">
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
        open={confirmCancelOrder !== null}
        title={t('admin.confirmCancelOrderTitle')}
        description={t('admin.confirmCancelOrderDesc', {
          number: confirmCancelOrder?.orderNumber ?? '',
        })}
        loading={updateStatusMutation.isPending}
        onCancel={() => setConfirmCancelOrder(null)}
        onConfirm={() => {
          if (confirmCancelOrder) {
            updateStatusMutation.mutate({ id: confirmCancelOrder.id, status: 'CANCELLED' });
          }
        }}
      />
    </div>
  );
}