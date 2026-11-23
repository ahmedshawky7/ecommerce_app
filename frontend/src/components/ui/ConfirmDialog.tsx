import { useEffect } from 'react';
import { AlertTriangle, X } from 'lucide-react';
import { Button } from './Button';
import { useI18n } from '../../context/I18nContext';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  danger?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  danger = true,
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const { t } = useI18n();

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onCancel]);

  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm"
      onClick={onCancel}
    >
      <div
        className="w-full max-w-md rounded-2xl border border-line bg-surface p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <div
              className={`mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
                danger ? 'bg-danger-soft text-danger' : 'bg-warning-soft text-warning'
              }`}
            >
              <AlertTriangle className="h-5 w-5" />
            </div>
            <div>
              <h2 id="confirm-dialog-title" className="text-base font-bold text-ink">
                {title}
              </h2>
              <p className="mt-1.5 text-sm leading-relaxed text-ink-secondary">{description}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onCancel}
            aria-label={t('common.close')}
            className="rounded-lg p-1 text-ink-muted hover:bg-subtle hover:text-ink"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="mt-6 flex justify-end gap-2.5">
          <Button variant="outline" size="sm" onClick={onCancel} disabled={loading}>
            {t('common.cancel')}
          </Button>
          <Button
            variant={danger ? 'danger' : 'primary'}
            size="sm"
            onClick={onConfirm}
            disabled={loading}
          >
            {confirmLabel ?? t('common.confirm')}
          </Button>
        </div>
      </div>
    </div>
  );
}