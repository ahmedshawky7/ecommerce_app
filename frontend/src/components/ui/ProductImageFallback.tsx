import { useState } from 'react';
import { Package } from 'lucide-react';

interface ProductImageProps {
  src?: string | null;
  alt: string;
  className?: string;
  iconClassName?: string;
}

export function ProductImage({
  src,
  alt,
  className = 'h-full w-full object-cover',
  iconClassName = 'h-8 w-8',
}: ProductImageProps) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return (
      <div className="flex h-full w-full items-center justify-center bg-subtle text-ink-muted">
        <div className="rounded-xl border border-line bg-surface p-3 shadow-sm">
          <Package className={iconClassName} />
        </div>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      onError={() => setFailed(true)}
      className={className}
      loading="lazy"
    />
  );
}