export function Skeleton({ className = "" }: { className?: string }) {
  return (
    <div
      className={`rounded-lg animate-shimmer ${className}`}
      aria-hidden="true"
    />
  );
}
