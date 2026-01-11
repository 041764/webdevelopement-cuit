import { getApiClient } from './client'
import type { ReportActivityStats, ReportPlanCompletion } from './schema'

export async function getReportPlanCompletion(params: { term: string; collegeId?: number }): Promise<ReportPlanCompletion> {
  const api = getApiClient()
  const res = await api.get<ReportPlanCompletion>('reports/plan-completion', { params })
  return res.data
}

export async function getReportActivityStats(params: { term: string }): Promise<ReportActivityStats> {
  const api = getApiClient()
  const res = await api.get<ReportActivityStats>('reports/activity-stats', { params })
  return res.data
}
