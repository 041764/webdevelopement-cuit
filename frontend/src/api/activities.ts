import { getApiClient } from './client'
import type {
  Activity,
  ActivityCreateRequest,
  ActivitySignup,
  ActivityStatus,
  PageActivity,
  PageActivitySignup,
  SignupStatus,
} from './schema'

type PageQuery = {
  page?: number
  size?: number
}

export async function listActivities(params: PageQuery & { term?: string; status?: ActivityStatus }): Promise<PageActivity> {
  const api = getApiClient()
  const res = await api.get<PageActivity>('activities', { params })
  return res.data
}

export async function createActivity(body: ActivityCreateRequest): Promise<Activity> {
  const api = getApiClient()
  const res = await api.post<Activity>('activities', body)
  return res.data
}

export async function getActivity(activityId: number): Promise<Activity> {
  const api = getApiClient()
  const res = await api.get<Activity>(`activities/${activityId}`)
  return res.data
}

export async function publishActivity(activityId: number): Promise<void> {
  const api = getApiClient()
  await api.post(`activities/${activityId}/publish`)
}

export async function signupActivity(activityId: number, note?: string): Promise<ActivitySignup> {
  const api = getApiClient()
  const body = note ? { note } : undefined
  const res = await api.post<ActivitySignup>(`activities/${activityId}/signups`, body)
  return res.data
}

export async function cancelMySignup(activityId: number): Promise<void> {
  const api = getApiClient()
  await api.delete(`activities/${activityId}/signups/me`)
}

export async function listActivitySignups(
  activityId: number,
  params: PageQuery & { status?: SignupStatus },
): Promise<PageActivitySignup> {
  const api = getApiClient()
  const res = await api.get<PageActivitySignup>(`activities/${activityId}/signups`, { params })
  return res.data
}

export async function approveSignup(activityId: number, signupId: number): Promise<void> {
  const api = getApiClient()
  await api.post(`activities/${activityId}/signups/${signupId}/approve`)
}

export async function rejectSignup(activityId: number, signupId: number, reason?: string): Promise<void> {
  const api = getApiClient()
  const body = reason ? { reason } : undefined
  await api.post(`activities/${activityId}/signups/${signupId}/reject`, body)
}
