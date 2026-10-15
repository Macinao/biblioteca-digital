export default function Logo({ className = "h-8" }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 font-bold tracking-tight ${className}`}>
      <svg viewBox="0 0 32 32" className="h-full w-auto" aria-hidden="true">
        <rect x="3" y="4" width="7" height="24" rx="1.5" fill="#f5c04d" />
        <rect x="12" y="4" width="7" height="24" rx="1.5" fill="currentColor" opacity="0.85" />
        <rect x="21.2" y="6.5" width="7" height="21.5" rx="1.5" transform="rotate(8 24.7 17)" fill="currentColor" />
      </svg>
      <span>
        Smart<span className="text-destaque">Teca</span>
      </span>
    </span>
  );
}
