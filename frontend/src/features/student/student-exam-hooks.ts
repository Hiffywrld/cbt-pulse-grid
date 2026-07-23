import { useQuery } from '@tanstack/react-query'
import { studentExamsApi } from './student-exams-api'

export const studentExamKeys = { all: ['student-exams'] as const, detail: (id: string) => ['student-exams', id] as const }
export const useStudentExams = (enabled = true) => useQuery({ queryKey: studentExamKeys.all, queryFn: studentExamsApi.list, enabled })
export const useStudentExam = (id: string) => useQuery({ queryKey: studentExamKeys.detail(id), queryFn: () => studentExamsApi.get(id), enabled: Boolean(id) })
