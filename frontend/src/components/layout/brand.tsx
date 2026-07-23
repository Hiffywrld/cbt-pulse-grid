export const Brand = ({ compact = false }: { compact?: boolean }) => (
  <span className={`brand brand--light ${compact ? 'brand--compact' : ''}`}>
    <span className="brand-mark" aria-hidden="true"><span /></span>
    <span className="brand__text"><strong>CBT-Pulse</strong><small>GRID</small></span>
  </span>
)
