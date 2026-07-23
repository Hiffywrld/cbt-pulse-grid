export const LoadingSkeleton = ({ rows = 5 }: { rows?: number }) => (
  <div className="skeleton-list" role="status" aria-label="Loading records">
    {Array.from({ length: rows }, (_, index) => <div className="skeleton-row" key={index}><span /><span /><span /></div>)}
  </div>
)
