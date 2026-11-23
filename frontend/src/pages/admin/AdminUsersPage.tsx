import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Search, ShieldAlert, Users } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { TableRowsSkeleton } from '../../components/ui/Skeletons';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { EmptyState } from '../../components/ui/EmptyState';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { fieldBaseClass } from '../../components/ui/Input';
import { formatDate, getApiErrorMessage } from '../../lib/utils';
import { PagedResponse, UserManagementResponse } from '../../types/api';

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const { t, locale } = useI18n();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [disablingUser, setDisablingUser] = useState<UserManagementResponse | null>(null);

  const { data: usersPage, isLoading } = useQuery({
    queryKey: ['admin-users', keyword, page],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (keyword.trim()) params.set('keyword', keyword.trim());
      params.set('page', String(page));
      params.set('size', '15');
      return (await api.get<PagedResponse<UserManagementResponse>>(`/api/admin/users?${params}`))
        .data;
    },
  });

  const toggleMutation = useMutation({
    mutationFn: async (id: number) => api.put(`/api/admin/users/${id}/toggle-status`),
    onSuccess: () => {
      toast.success(t('admin.userStatusUpdated'));
      setDisablingUser(null);
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const users = usersPage?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-ink">{t('admin.usersTitle')}</h1>
          <p className="mt-1 text-sm text-ink-secondary">{t('admin.usersSubtitle')}</p>
        </div>
        <div className="relative">
          <Search className="absolute start-3 top-3 h-4 w-4 text-ink-muted" />
          <input
            type="search"
            placeholder={t('admin.searchUsersPlaceholder')}
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setPage(0);
            }}
            className={`${fieldBaseClass} w-64 ps-9`}
          />
        </div>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-line bg-surface shadow-card">
        <table className="w-full text-start text-sm">
          <thead className="border-b border-line bg-subtle/70 text-xs uppercase text-ink-secondary">
            <tr>
              <th className="px-5 py-3.5 text-start">{t('admin.userCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.roleCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.joinedCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('common.status')}</th>
              <th className="px-5 py-3.5 text-end">{t('admin.actionCol')}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {isLoading ? (
              <TableRowsSkeleton cols={5} rows={6} />
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={5} className="p-6">
                  <EmptyState compact icon={Users} title={t('admin.noUsersMatch')} />
                </td>
              </tr>
            ) : (
              users.map((u) => (
                <tr key={u.id} className="transition-colors hover:bg-subtle/50">
                  <td className="px-5 py-3.5">
                    <p dir="auto" className="font-bold text-ink">
                      {u.username}
                    </p>
                    <p dir="ltr" className="inline-block text-xs text-ink-muted">
                      {u.email}
                    </p>
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge variant={u.role === 'ADMIN' ? 'accent' : 'neutral'}>{u.role}</Badge>
                  </td>
                  <td dir="ltr" className="px-5 py-3.5 text-xs text-ink-muted">
                    {formatDate(u.createdAt, locale)}
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge variant={u.isEnabled ? 'success' : 'danger'}>
                      {u.isEnabled ? t('admin.active') : t('admin.disabled')}
                    </Badge>
                  </td>
                  <td className="px-5 py-3.5 text-end">
                    <Button
                      variant={u.isEnabled ? 'danger' : 'outline'}
                      size="sm"
                      disabled={toggleMutation.isPending}
                      onClick={() => {
                        if (u.isEnabled) {
                          setDisablingUser(u);
                        } else {
                          toggleMutation.mutate(u.id);
                        }
                      }}
                    >
                      <ShieldAlert className="h-3.5 w-3.5" />
                      <span>{u.isEnabled ? t('admin.disable') : t('admin.enable')}</span>
                    </Button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {(usersPage?.totalPages ?? 0) > 1 && (
        <div className="flex justify-end gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            {t('common.previous')}
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= (usersPage?.totalPages ?? 0)}
            onClick={() => setPage((p) => p + 1)}
          >
            {t('common.next')}
          </Button>
        </div>
      )}

      <ConfirmDialog
        open={disablingUser !== null}
        title={t('admin.confirmDisableUserTitle')}
        description={t('admin.confirmDisableUserDesc', {
          email: disablingUser?.email ?? '',
        })}
        loading={toggleMutation.isPending}
        onCancel={() => setDisablingUser(null)}
        onConfirm={() => {
          if (disablingUser) toggleMutation.mutate(disablingUser.id);
        }}
      />
    </div>
  );
}