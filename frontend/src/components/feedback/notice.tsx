import { Alert } from './alert'

export type NoticeValue = { tone: 'success' | 'error'; message: string } | null

export const Notice = ({ notice }: { notice: NoticeValue }) => notice
  ? <div className="page-notice"><Alert tone={notice.tone}>{notice.message}</Alert></div>
  : null
