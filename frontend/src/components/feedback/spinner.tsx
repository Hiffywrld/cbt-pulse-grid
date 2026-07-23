export const Spinner = ({
  label = 'Loading',
  size = 'md',
}: {
  label?: string
  size?: 'sm' | 'md' | 'lg'
}) => (
  <span className={`spinner spinner--${size}`} role="status" aria-label={label} />
)

export const FullPageSpinner = ({ label }: { label: string }) => (
  <main className="full-page-state">
    <div className="brand-mark" aria-hidden="true"><span /></div>
    <Spinner size="lg" label={label} />
    <p>{label}</p>
  </main>
)
