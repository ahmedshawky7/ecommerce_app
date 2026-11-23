import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Power, TicketPercent } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { TableRowsSkeleton } from '../../components/ui/Skeletons';
import { fieldBaseClass } from '../../components/ui/Input';
import { formatDate, formatNumber, getApiErrorMessage } from '../../lib/utils';
import { CouponResponse, DiscountType, PagedResponse } from '../../types/api';

export function AdminCouponsPage() {
  const queryClient = useQueryClient();
  const { t, locale } = useI18n();
  const { formatBasePrice } = useCurrency();
  const [showForm, setShowForm] = useState(false);

  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
  const [discountType, setDiscountType] = useState<DiscountType>('PERCENTAGE');
  const [discountValue, setDiscountValue] = useState('15');
  const [minOrderAmount, setMinOrderAmount] = useState('0');
  const [maxDiscountAmount, setMaxDiscountAmount] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [usageLimit, setUsageLimit] = useState('100');
  const [perUserLimit, setPerUserLimit] = useState('1');

  const { data: couponsPage, isLoading } = useQuery({
    queryKey: ['admin-coupons'],
    queryFn: async () =>
      (await api.get<PagedResponse<CouponResponse>>('/api/admin/coupons?size=50')).data,
  });

  const createMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post('/api/admin/coupons', {
          code,
          description,
          discountType,
          discountValue: Number(discountValue),
          minOrderAmount: Number(minOrderAmount || 0),
          maxDiscountAmount: maxDiscountAmount ? Number(maxDiscountAmount) : null,
          expiresAt: expiresAt.length === 16 ? `${expiresAt}:00` : expiresAt,
          usageLimit: usageLimit ? Number(usageLimit) : null,
          perUserLimit: Number(perUserLimit || 1),
          isActive: true,
        })
      ).data,
    onSuccess: () => {
      toast.success(t('admin.couponCreated'));
      setShowForm(false);
      setCode('');
      setDescription('');
      queryClient.invalidateQueries({ queryKey: ['admin-coupons'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const toggleMutation = useMutation({
    mutationFn: async (id: number) => api.put(`/api/admin/coupons/${id}/toggle-status`),
    onSuccess: () => {
      toast.success(t('admin.couponToggled'));
      queryClient.invalidateQueries({ queryKey: ['admin-coupons'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const coupons = couponsPage?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-ink">{t('admin.couponsTitle')}</h1>
          <p className="mt-1 text-sm text-ink-secondary">{t('admin.couponsSubtitle')}</p>
        </div>
        <Button onClick={() => setShowForm((s) => !s)}>
          <Plus className="h-4 w-4" />
          <span>{t('admin.newCoupon')}</span>
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="grid grid-cols-1 gap-4 rounded-2xl border border-line bg-surface p-6 shadow-card sm:grid-cols-3"
        >
          <input
            required
            dir="ltr"
            placeholder={t('admin.codePlaceholder')}
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            className={`${fieldBaseClass} uppercase`}
          />
          <select
            value={discountType}
            onChange={(e) => setDiscountType(e.target.value as DiscountType)}
            className={fieldBaseClass}
          >
            <option value="PERCENTAGE">{t('admin.percentage')}</option>
            <option value="FIXED_AMOUNT">{t('admin.fixedAmount')}</option>
          </select>
          <input
            type="number"
            step="0.01"
            dir="ltr"
            required
            placeholder={t('admin.discountValue')}
            value={discountValue}
            onChange={(e) => setDiscountValue(e.target.value)}
            className={fieldBaseClass}
          />
          <input
            type="number"
            step="0.01"
            dir="ltr"
            placeholder={t('admin.minOrder')}
            value={minOrderAmount}
            onChange={(e) => setMinOrderAmount(e.target.value)}
            className={fieldBaseClass}
          />
          <input
            type="number"
            step="0.01"
            dir="ltr"
            placeholder={t('admin.maxCap')}
            value={maxDiscountAmount}
            onChange={(e) => setMaxDiscountAmount(e.target.value)}
            className={fieldBaseClass}
          />
          <input
            type="datetime-local"
            dir="ltr"
            required
            value={expiresAt}
            onChange={(e) => setExpiresAt(e.target.value)}
            className={fieldBaseClass}
          />
          <input
            type="number"
            dir="ltr"
            placeholder={t('admin.totalUsageLimit')}
            value={usageLimit}
            onChange={(e) => setUsageLimit(e.target.value)}
            className={fieldBaseClass}
          />
          <input
            type="number"
            dir="ltr"
            placeholder={t('admin.perUserLimit')}
            value={perUserLimit}
            onChange={(e) => setPerUserLimit(e.target.value)}
            className={fieldBaseClass}
          />
          <input
            dir="auto"
            placeholder={t('admin.descPlaceholder')}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className={fieldBaseClass}
          />
          <div className="flex gap-2 sm:col-span-3">
            <Button type="submit" disabled={createMutation.isPending}>
              {t('admin.createCoupon')}
            </Button>
            <Button variant="outline" onClick={() => setShowForm(false)}>
              {t('common.cancel')}
            </Button>
          </div>
        </form>
      )}

      <div className="overflow-x-auto rounded-2xl border border-line bg-surface shadow-card">
        <table className="w-full text-start text-sm">
          <thead className="border-b border-line bg-subtle/70 text-xs uppercase text-ink-secondary">
            <tr>
              <th className="px-5 py-3.5 text-start">{t('admin.codeCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.discountCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.minOrderCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.usageCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.expiresCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('common.status')}</th>
              <th className="px-5 py-3.5 text-end">{t('admin.toggleCol')}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {isLoading ? (
              <TableRowsSkeleton cols={7} rows={4} />
            ) : coupons.length === 0 ? (
              <tr>
                <td colSpan={7} className="p-6">
                  <EmptyState compact icon={TicketPercent} title={t('admin.noCoupons')} />
                </td>
              </tr>
            ) : (
              coupons.map((c) => (
                <tr key={c.id} className="transition-colors hover:bg-subtle/50">
                  <td dir="ltr" className="px-5 py-3.5 font-mono font-bold text-ink">
                    {c.code}
                  </td>
                  <td dir="ltr" className="px-5 py-3.5 font-semibold text-ink">
                    {c.discountType === 'PERCENTAGE'
                      ? `${formatNumber(c.discountValue)}%`
                      : formatBasePrice(c.discountValue)}
                  </td>
                  <td dir="ltr" className="px-5 py-3.5 text-ink-secondary">
                    {formatBasePrice(c.minOrderAmount)}
                  </td>
                  <td dir="ltr" className="px-5 py-3.5 text-ink-secondary">
                    {formatNumber(c.usageCount)} /{' '}
                    {c.usageLimit ? formatNumber(c.usageLimit) : '∞'}
                  </td>
                  <td dir="ltr" className="px-5 py-3.5 text-xs text-ink-muted">
                    {formatDate(c.expiresAt, locale)}
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge variant={c.isActive ? 'success' : 'neutral'}>
                      {c.isActive ? t('admin.active') : t('admin.disabled')}
                    </Badge>
                  </td>
                  <td className="px-5 py-3.5 text-end">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => toggleMutation.mutate(c.id)}
                      disabled={toggleMutation.isPending}
                    >
                      <Power className="h-3.5 w-3.5" />
                      <span>{t('admin.toggleBtn')}</span>
                    </Button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}