import React from 'react';

type BadgeVariant = 'neutral' | 'accent' | 'success' | 'warning' | 'danger';

const styles: Record<BadgeVariant, string> = {
  neutral: 'bg-subtle text-ink-secondary border-line',
  accent: 'bg-accent-soft text-accent border-accent/20 dark:bg-accent/15 dark:text-accent-hover',
  success: 'bg-success-soft text-success border-success/20 dark:bg-success/15',
  warning: 'bg-warning-soft text-warning border-warning/20 dark:bg-warning/15',
  danger: 'bg-danger-soft text-danger border-danger/20 dark:bg-danger/15',
};

export function Badge({
  variant = 'neutral',
  className = '',
  children,
}: {
  variant?: BadgeVariant;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold leading-tight ${styles[variant]} ${className}`}
    >
      {children}
    </span>
  );
}