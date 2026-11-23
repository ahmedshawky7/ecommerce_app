import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { FileClock } from 'lucide-react';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { TableRowsSkeleton } from '../../components/ui/Skeletons';
import { Button } from '../../components/ui/Button';
import { EmptyState } from '../../components/ui/EmptyState';
import { fieldBaseClass } from '../../components/ui/Input';
import { formatDate, formatNumber } from '../../lib/utils';
import { AuditLogResponse, PagedResponse } from '../../types/api';

export function AdminAuditLogsPage() {
  const { t, locale } = useI18n();
  const [entityType, setEntityType] = useState('');
  const [actorEmail, setActorEmail] = useState('');
  const [page, setPage] = useState(0);

  const { data: logsPage, isLoading } = useQuery({
    queryKey: ['admin-audit-logs', entityType, actorEmail, page],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (entityType) params.set('entityType', entityType);
      if (actorEmail.trim()) params.set('actorEmail', actorEmail.trim());
      params.set('page', String(page));
      params.set('size', '20');
      return (await api.get<PagedResponse<AuditLogResponse>>(`/api/admin/audit-logs?${params}`))
        .data;
    },
  });

  const logs = logsPage?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-ink">{t('admin.auditTitle')}</h1>
          <p className="mt-1 text-sm text-ink-secondary">{t('admin.auditSubtitle')}</p>
        </div>

        <div className="flex flex-wrap gap-2">
          <input
            type="search"
            dir="ltr"
            placeholder={t('admin.filterActorPlaceholder')}
            value={actorEmail}
            onChange={(e) => {
              setActorEmail(e.target.value);
              setPage(0);
            }}
            className={`${fieldBaseClass} w-56`}
          />
          <select
            aria-label={t('admin.entityCol')}
            value={entityType}
            onChange={(e) => {
              setEntityType(e.target.value);
              setPage(0);
            }}
            className={`${fieldBaseClass} w-auto`}
          >
            <option value="">{t('admin.allEntities')}</option>
            <option value="PRODUCT">PRODUCT</option>
            <option value="ORDER">ORDER</option>
            <option value="COUPON">COUPON</option>
            <option value="USER">USER</option>
          </select>
        </div>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-line bg-surface shadow-card">
        <table className="w-full text-start text-xs">
          <thead className="border-b border-line bg-subtle/70 uppercase text-ink-secondary">
            <tr>
              <th className="px-4 py-3.5 text-start">{t('admin.timestampCol')}</th>
              <th className="px-4 py-3.5 text-start">{t('admin.actorCol')}</th>
              <th className="px-4 py-3.5 text-start">{t('admin.actionEventCol')}</th>
              <th className="px-4 py-3.5 text-start">{t('admin.entityCol')}</th>
              <th className="px-4 py-3.5 text-start">{t('admin.oldNewCol')}</th>
              <th className="px-4 py-3.5 text-start">{t('admin.detailsCol')}</th>
              <th className="px-4 py-3.5 text-start">{t('admin.ipCol')}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {isLoading ? (
              <TableRowsSkeleton cols={7} rows={8} />
            ) : logs.length === 0 ? (
              <tr>
                <td colSpan={7} className="p-6">
                  <EmptyState compact icon={FileClock} title={t('admin.noLogsMatch')} />
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="transition-colors hover:bg-subtle/50">
                  <td dir="ltr" className="whitespace-nowrap px-4 py-3 text-ink-muted">
                    {formatDate(log.createdAt, locale)}
                  </td>
                  <td dir="ltr" className="px-4 py-3 font-medium text-ink">
                    {log.actorEmail}
                  </td>
                  <td className="px-4 py-3">
                    <span dir="ltr" className="inline-block rounded bg-subtle px-2 py-0.5 font-mono font-semibold text-ink">
                      {log.action}
                    </span>
                  </td>
                  <td dir="ltr" className="px-4 py-3 text-ink-secondary">
                    {log.entityType} #{log.entityId ? formatNumber(log.entityId) : '-'}
                  </td>
                  <td dir="ltr" className="px-4 py-3 font-mono text-ink-secondary">
                    {log.oldValue || '—'} → {log.newValue || '—'}
                  </td>
                  <td dir="auto" className="px-4 py-3 text-ink-secondary">
                    {log.details}
                  </td>
                  <td dir="ltr" className="px-4 py-3 font-mono text-ink-muted">
                    {log.ipAddress || '-'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {(logsPage?.totalPages ?? 0) > 1 && (
        <div className="flex justify-end gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            {t('common.previous')}
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= (logsPage?.totalPages ?? 0)}
            onClick={() => setPage((p) => p + 1)}
          >
            {t('common.next')}
          </Button>
        </div>
      )}
    </div>
  );
}