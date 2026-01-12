import { getApiClient } from './client'
import type { components } from './generated'

export type StudentOption = components['schemas']['StudentOption']
export type ClassOption = components['schemas']['ClassOption']
export type CollegeOption = components['schemas']['CollegeOption']

/**
 * 获取学生列表（用于下拉选择）
 */
export async function fetchStudents(term?: string): Promise<StudentOption[]> {
  const api = getApiClient()
  const res = await api.get<StudentOption[]>('lookup/students', { params: { term } })
  return res.data ?? []
}

/**
 * 获取班级列表（用于下拉选择）
 */
export async function fetchClasses(term?: string): Promise<ClassOption[]> {
  const api = getApiClient()
  const res = await api.get<ClassOption[]>('lookup/classes', { params: { term } })
  return res.data ?? []
}

/**
 * 获取学院列表（用于下拉选择）
 */
export async function fetchColleges(): Promise<CollegeOption[]> {
  const api = getApiClient()
  const res = await api.get<CollegeOption[]>('lookup/colleges')
  return res.data ?? []
}
