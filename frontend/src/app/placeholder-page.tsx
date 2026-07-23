import { Construction } from 'lucide-react'
import { EmptyState } from '../components/feedback/empty-state'
import { PageHeader } from '../components/layout/page-header'

export const PlaceholderPage = ({ title, description }: { title: string; description: string }) => (
  <div><PageHeader eyebrow="Planned integration" title={title} description={description} /><EmptyState title="Coming in a later frontend phase" description="The secure application foundation is ready. This business workflow has intentionally not been integrated yet." action={<Construction aria-hidden="true" />} /></div>
)
