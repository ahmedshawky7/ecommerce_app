import React from 'react';

type Variant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
type Size = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  fullWidth?: boolean;
}

const variantClasses: Record<Variant, string> = {
  primary:
    'bg-accent text-accent-fg shadow-sm hover:bg-accent-hover disabled:opacity-50',
  secondary:
    'bg-ink text-surface hover:opacity-90 disabled:opacity-50',
  outline:
    'border border-line bg-surface text-ink hover:bg-subtle hover:border-line-strong disabled:opacity-50',
  ghost:
    'bg-transparent text-ink-secondary hover:bg-subtle hover:text-ink disabled:opacity-50',
  danger:
    'border border-danger/25 bg-danger-soft/60 text-danger hover:bg-danger-soft disabled:opacity-50 dark:bg-danger-soft/30',
};

const sizeClasses: Record<Size, string> = {
  sm: 'px-3 py-1.5 text-xs rounded-lg',
  md: 'px-4 py-2.5 text-sm rounded-xl',
  lg: 'px-6 py-3.5 text-sm rounded-xl',
};

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  className = '',
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={`inline-flex items-center justify-center gap-2 font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-canvas disabled:cursor-not-allowed ${variantClasses[variant]} ${sizeClasses[size]} ${
        fullWidth ? 'w-full' : ''
      } ${className}`}
      {...props}
    />
  );
}