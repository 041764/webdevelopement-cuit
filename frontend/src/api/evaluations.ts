import { getApiClient } from './client'
import type { Evaluation, EvaluationCreateRequest, EvaluationDetail, PageEvaluation } from './schema'

type PageQuery = {
  page?: number
  size?: number
}

export async function listEvaluations(params: PageQuery & { term?: string }): Promise<PageEvaluation> {
  const api = getApiClient()
  const res = await api.get<PageEvaluation>('evaluations', { params })
  return res.data
}

export async function createEvaluation(body: EvaluationCreateRequest): Promise<Evaluation> {
  const api = getApiClient()
  const res = await api.post<Evaluation>('evaluations', body)
  return res.data
}

export async function getEvaluation(evaluationId: number): Promise<EvaluationDetail> {
  const api = getApiClient()
  const res = await api.get<EvaluationDetail>(`evaluations/${evaluationId}`)
  return res.data
}
