import { getApiClient } from './client'
import type {
  PagePlan,
  Plan,
  PlanCreateRequest,
  PlanDetail,
  PlanItem,
  PlanItemCreateRequest,
  PlanItemProgress,
  PlanItemProgressCreateRequest,
  PlanItemUpdateRequest,
} from './schema'

type PageQuery = {
  page?: number
  size?: number
}

export async function listPlans(params: PageQuery & { term?: string }): Promise<PagePlan> {
  const api = getApiClient()
  const res = await api.get<PagePlan>('plans', { params })
  return res.data
}

export async function createPlan(body: PlanCreateRequest): Promise<Plan> {
  const api = getApiClient()
  const res = await api.post<Plan>('plans', body)
  return res.data
}

export async function getPlan(planId: number): Promise<PlanDetail> {
  const api = getApiClient()
  const res = await api.get<PlanDetail>(`plans/${planId}`)
  return res.data
}

export async function addPlanItem(planId: number, body: PlanItemCreateRequest): Promise<PlanItem> {
  const api = getApiClient()
  const res = await api.post<PlanItem>(`plans/${planId}/items`, body)
  return res.data
}

export async function updatePlanItem(planId: number, itemId: number, body: PlanItemUpdateRequest): Promise<void> {
  const api = getApiClient()
  await api.patch(`plans/${planId}/items/${itemId}`, body)
}

export async function deletePlanItem(planId: number, itemId: number): Promise<void> {
  const api = getApiClient()
  await api.delete(`plans/${planId}/items/${itemId}`)
}

export async function listPlanItemProgress(itemId: number): Promise<PlanItemProgress[]> {
  const api = getApiClient()
  const res = await api.get<PlanItemProgress[]>(`plan-items/${itemId}/progress`)
  return res.data
}

export async function addPlanItemProgress(itemId: number, body: PlanItemProgressCreateRequest): Promise<PlanItemProgress> {
  const api = getApiClient()
  const res = await api.post<PlanItemProgress>(`plan-items/${itemId}/progress`, body)
  return res.data
}
