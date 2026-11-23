export function ProductCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl border border-line bg-surface p-4 shadow-card">
      <div className="aspect-[4/3] animate-pulse rounded-xl bg-subtle" />
      <div className="mt-4 flex items-center justify-between">
        <div className="h-5 w-20 animate-pulse rounded-md bg-subtle" />
        <div className="h-4 w-16 animate-pulse rounded-md bg-subtle" />
      </div>
      <div className="mt-3 h-5 w-4/5 animate-pulse rounded-md bg-subtle" />
      <div className="mt-2 h-3.5 w-full animate-pulse rounded-md bg-subtle/70" />
      <div className="mt-4 border-t border-line pt-3">
        <div className="h-6 w-24 animate-pulse rounded-md bg-subtle" />
        <div className="mt-3 h-9 w-full animate-pulse rounded-xl bg-subtle" />
      </div>
    </div>
  );
}

export function TableRowsSkeleton({ cols = 6, rows = 5 }: { cols?: number; rows?: number }) {
  return (
    <>
      {Array.from({ length: rows }).map((_, rIdx) => (
        <tr key={rIdx} className="animate-pulse">
          {Array.from({ length: cols }).map((__, cIdx) => (
            <td key={cIdx} className="px-5 py-4">
              <div className="h-4 w-full max-w-[120px] rounded bg-subtle" />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}