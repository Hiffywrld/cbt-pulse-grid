import { Button } from './button'
import { Modal } from './modal'

export const ConfirmDialog = ({ open, title, description, confirmLabel, loading, onConfirm, onClose }: { open: boolean; title: string; description: string; confirmLabel: string; loading?: boolean; onConfirm(): void; onClose(): void }) => (
  <Modal open={open} title={title} onClose={onClose}>
    <p>{description}</p>
    <div className="modal-actions"><Button variant="secondary" onClick={onClose}>Cancel</Button><Button variant="danger" loading={loading} onClick={onConfirm}>{confirmLabel}</Button></div>
  </Modal>
)
