import React from 'react';

export const fieldBaseClass =
  'w-full rounded-xl border border-line bg-surface px-3.5 py-2.5 text-sm text-ink placeholder:text-ink-muted transition-colors focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 disabled:opacity-50';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
}

export function Input({ label, className = '', id, ...props }: InputProps) {
  return (
    <div>
      {label && (
        <label htmlFor={id} className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-ink-secondary">
          {label}
        </label>
      )}
      <input id={id} className={`${fieldBaseClass} ${className}`} {...props} />
    </div>
  );
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
}

export function Select({ label, className = '', id, children, ...props }: SelectProps) {
  return (
    <div>
      {label && (
        <label htmlFor={id} className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-ink-secondary">
          {label}
        </label>
      )}
      <select id={id} className={`${fieldBaseClass} ${className}`} {...props}>
        {children}
      </select>
    </div>
  );
}