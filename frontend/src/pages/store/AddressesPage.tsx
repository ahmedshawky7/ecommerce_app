import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MapPin, Plus, Star, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { fieldBaseClass } from '../../components/ui/Input';
import { getApiErrorMessage } from '../../lib/utils';
import { AddressResponse } from '../../types/api';

const emptyForm = {
  label: 'Home',
  fullName: '',
  phone: '',
  streetAddress: '',
  city: '',
  state: '',
  postalCode: '',
  country: '',
  isDefault: false,
};

export function AddressesPage() {
  const queryClient = useQueryClient();
  const { t } = useI18n();
  const [form, setForm] = useState(emptyForm);
  const [showForm, setShowForm] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const { data: addresses = [], isLoading } = useQuery({
    queryKey: ['addresses'],
    queryFn: async () => (await api.get<AddressResponse[]>('/api/addresses')).data,
  });

  const createMutation = useMutation({
    mutationFn: async () => (await api.post<AddressResponse>('/api/addresses', form)).data,
    onSuccess: () => {
      toast.success(t('addresses.savedMsg'));
      setForm(emptyForm);
      setShowForm(false);
      queryClient.invalidateQueries({ queryKey: ['addresses'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const setDefaultMutation = useMutation({
    mutationFn: async (id: number) => api.put(`/api/addresses/${id}/default`),
    onSuccess: () => {
      toast.success(t('addresses.defaultUpdated'));
      queryClient.invalidateQueries({ queryKey: ['addresses'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/api/addresses/${id}`),
    onSuccess: () => {
      toast.success(t('addresses.deletedMsg'));
      setDeletingId(null);
      queryClient.invalidateQueries({ queryKey: ['addresses'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 md:px-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-extrabold text-ink">{t('addresses.title')}</h1>
        <Button onClick={() => setShowForm((s) => !s)}>
          <Plus className="h-4 w-4" />
          <span>{t('addresses.addAddress')}</span>
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="mt-6 grid grid-cols-1 gap-4 rounded-2xl border border-line bg-surface p-6 shadow-card sm:grid-cols-2"
        >
          <input
            required
            placeholder={t('addresses.labelPlaceholder')}
            value={form.label}
            onChange={(e) => setForm({ ...form, label: e.target.value })}
            className={fieldBaseClass}
          />
          <input
            required
            placeholder={t('addresses.fullNamePlaceholder')}
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            className={fieldBaseClass}
          />
          <input
            required
            dir="ltr"
            placeholder={t('addresses.phonePlaceholder')}
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
            className={fieldBaseClass}
          />
          <input
            required
            placeholder={t('addresses.streetPlaceholder')}
            value={form.streetAddress}
            onChange={(e) => setForm({ ...form, streetAddress: e.target.value })}
            className={fieldBaseClass}
          />
          <input
            required
            placeholder={t('addresses.cityPlaceholder')}
            value={form.city}
            onChange={(e) => setForm({ ...form, city: e.target.value })}
            className={fieldBaseClass}
          />
          <input
            placeholder={t('addresses.statePlaceholder')}
            value={form.state}
            onChange={(e) => setForm({ ...form, state: e.target.value })}
            className={fieldBaseClass}
          />
          <input
            dir="ltr"
            placeholder={t('addresses.postalPlaceholder')}
            value={form.postalCode}
            onChange={(e) => setForm({ ...form, postalCode: e.target.value })}
            className={fieldBaseClass}
          />
          <input
            required
            placeholder={t('addresses.countryPlaceholder')}
            value={form.country}
            onChange={(e) => setForm({ ...form, country: e.target.value })}
            className={fieldBaseClass}
          />
          <label className="flex items-center gap-2 text-sm text-ink sm:col-span-2">
            <input
              type="checkbox"
              checked={form.isDefault}
              onChange={(e) => setForm({ ...form, isDefault: e.target.checked })}
            />
            {t('addresses.setDefaultCheck')}
          </label>
          <div className="flex gap-2 sm:col-span-2">
            <Button type="submit" disabled={createMutation.isPending}>
              {t('addresses.saveAddress')}
            </Button>
            <Button variant="outline" onClick={() => setShowForm(false)}>
              {t('common.cancel')}
            </Button>
          </div>
        </form>
      )}

      {isLoading ? (
        <div className="mt-6 h-40 animate-pulse rounded-2xl bg-subtle" />
      ) : addresses.length === 0 ? (
        <div className="mt-8">
          <EmptyState icon={MapPin} title={t('addresses.noAddresses')} />
        </div>
      ) : (
        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
          {addresses.map((addr) => (
            <div
              key={addr.id}
              className="rounded-2xl border border-line bg-surface p-5 shadow-card"
            >
              <div className="flex items-center justify-between">
                <span className="font-bold text-ink">{addr.label}</span>
                {addr.isDefault ? (
                  <Badge variant="success">{t('addresses.defaultBadge')}</Badge>
                ) : (
                  <button
                    type="button"
                    onClick={() => setDefaultMutation.mutate(addr.id)}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-accent hover:underline"
                  >
                    <Star className="h-3 w-3" />
                    {t('addresses.setDefaultBtn')}
                  </button>
                )}
              </div>
              <p dir="auto" className="mt-2 text-sm font-semibold text-ink">
                {addr.fullName}
              </p>
              <p dir="auto" className="mt-1 text-sm text-ink-secondary">
                {addr.streetAddress}, {addr.city}
                {addr.state ? `, ${addr.state}` : ''} {addr.postalCode || ''}
              </p>
              <p className="text-sm text-ink-secondary">{addr.country}</p>
              <p dir="ltr" className="mt-1 inline-block text-xs text-ink-muted">
                {addr.phone}
              </p>
              <div className="mt-4">
                <button
                  type="button"
                  onClick={() => setDeletingId(addr.id)}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-danger hover:underline"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  {t('common.delete')}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={deletingId !== null}
        title={t('addresses.confirmDeleteTitle')}
        description={t('addresses.confirmDeleteDesc')}
        loading={deleteMutation.isPending}
        onCancel={() => setDeletingId(null)}
        onConfirm={() => {
          if (deletingId !== null) deleteMutation.mutate(deletingId);
        }}
      />
    </div>
  );
}