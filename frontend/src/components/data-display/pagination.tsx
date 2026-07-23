import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '../ui/button'

export const Pagination = ({ page, totalPages, totalElements, onPage }: { page: number; totalPages: number; totalElements: number; onPage(page: number): void }) => (
  <nav className="pagination" aria-label="Pagination">
    <p>{totalElements.toLocaleString()} record{totalElements === 1 ? '' : 's'} · Page {totalPages === 0 ? 0 : page + 1} of {totalPages}</p>
    <div>
      <Button variant="secondary" size="sm" icon={<ChevronLeft size={16} />} disabled={page <= 0} onClick={() => onPage(page - 1)}>Previous</Button>
      <Button variant="secondary" size="sm" icon={<ChevronRight size={16} />} disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</Button>
    </div>
  </nav>
)
