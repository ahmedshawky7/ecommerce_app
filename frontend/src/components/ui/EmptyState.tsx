import React from 'react';
import { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: React.ReactNode;
  compact?: boolean;
}

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  compact = false,
}: EmptyStateProps) {
  return (
    <div
      className={`flex flex-col items-center justify-center rounded-2xl border border-dashed border-line-strong bg-surface text-center ${
        compact ? 'px-4 py-10' : 'px-6 py-14'
      }`}
    >
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-subtle text-ink-muted">
        <Icon className="h-6 w-6" />
      </div>
      <h3 className="mt-4 text-base font-bold text-ink">{title}</h3>
      {description && (
        <p className="mt-1.5 max-w-md text-sm text-ink-secondary">{description}</p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}