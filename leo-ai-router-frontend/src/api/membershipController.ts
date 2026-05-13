// @ts-ignore
/* eslint-disable */
import request from '@/request'

export interface SubscriptionPlan {
  id?: number
  planCode?: string
  planName?: string
  description?: string
  price?: number
  originalPrice?: number
  durationDays?: number
  lifetime?: number
  dailyProLimit?: number
  dailyAdvancedLimit?: number
  monthlyProLimit?: number
  monthlyAdvancedLimit?: number
  bonusPoints?: number
  apiKeyLimit?: number
  allowByok?: number
  priority?: number
  visible?: number
  status?: string
}

export interface PointPackage {
  id?: number
  packageCode?: string
  packageName?: string
  points?: number
  price?: number
  originalPrice?: number
  description?: string
  badge?: string
  priority?: number
  visible?: number
  status?: string
}

export interface MembershipVO {
  planCode?: string
  planName?: string
  startTime?: string
  endTime?: string
  lifetime?: number
  status?: string
  dailyProLimit?: number
  dailyProUsed?: number
  dailyProRemaining?: number
  dailyAdvancedLimit?: number
  dailyAdvancedUsed?: number
  dailyAdvancedRemaining?: number
  pointBalance?: number
}

export async function listMembershipPlans(options?: { [key: string]: any }) {
  return request<{ code: number; data?: SubscriptionPlan[]; message?: string }>('/membership/plans', {
    method: 'GET',
    ...(options || {}),
  })
}

export async function listPointPackages(options?: { [key: string]: any }) {
  return request<{ code: number; data?: PointPackage[]; message?: string }>('/membership/point-packages', {
    method: 'GET',
    ...(options || {}),
  })
}

export async function getMyMembership(options?: { [key: string]: any }) {
  return request<{ code: number; data?: MembershipVO; message?: string }>('/membership/my', {
    method: 'GET',
    ...(options || {}),
  })
}
