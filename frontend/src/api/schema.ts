import type { components } from './generated'

export type ErrorResponse = components['schemas']['ErrorResponse']
export type UserType = components['schemas']['UserType']
export type RoleCode = components['schemas']['RoleCode']

export type TokenPair = components['schemas']['TokenPair']
export type UserMe = components['schemas']['UserMe']

export type ImportUsersResult = components['schemas']['ImportUsersResult']
export type ImportFailure = components['schemas']['ImportFailure']

export type ActivityStatus = components['schemas']['ActivityStatus']
export type SignupStatus = components['schemas']['SignupStatus']
export type Activity = components['schemas']['Activity']
export type ActivityCreateRequest = components['schemas']['ActivityCreateRequest']
export type ActivitySignup = components['schemas']['ActivitySignup']
export type PageActivity = components['schemas']['PageActivity']
export type PageActivitySignup = components['schemas']['PageActivitySignup']

export type PlanOwnerType = components['schemas']['PlanOwnerType']
export type Plan = components['schemas']['Plan']
export type PlanCreateRequest = components['schemas']['PlanCreateRequest']
export type PlanItemStatus = components['schemas']['PlanItemStatus']
export type PlanItem = components['schemas']['PlanItem']
export type PlanItemCreateRequest = components['schemas']['PlanItemCreateRequest']
export type PlanItemUpdateRequest = components['schemas']['PlanItemUpdateRequest']
export type PlanDetail = components['schemas']['PlanDetail']
export type PlanItemProgress = components['schemas']['PlanItemProgress']
export type PlanItemProgressCreateRequest = components['schemas']['PlanItemProgressCreateRequest']
export type PagePlan = components['schemas']['PagePlan']

export type Evaluation = components['schemas']['Evaluation']
export type EvaluationCreateRequest = components['schemas']['EvaluationCreateRequest']
export type EvaluationDetailItem = components['schemas']['EvaluationDetailItem']
export type EvaluationDetail = components['schemas']['EvaluationDetail']
export type PageEvaluation = components['schemas']['PageEvaluation']

export type ReportPlanCompletion = components['schemas']['ReportPlanCompletion']
export type ReportActivityStats = components['schemas']['ReportActivityStats']
