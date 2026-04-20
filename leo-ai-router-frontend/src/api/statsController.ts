// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取调用历史详情 GET /stats/history/detail */
export async function getHistoryDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getHistoryDetailParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRequestLog>('/stats/history/detail', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 分页查询我的调用历史 POST /stats/history/my/page */
export async function pageMyHistory(
  body: API.RequestLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageRequestLog>('/stats/history/my/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 分页查询所有调用历史（仅管理员） POST /stats/history/page */
export async function pageHistory(
  body: API.RequestLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageRequestLog>('/stats/history/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取我的费用统计 GET /stats/my/cost */
export async function getMyCostStats(options?: { [key: string]: any }) {
  return request<API.BaseResponseCostStatsVO>('/stats/my/cost', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 获取我的每日统计数据 GET /stats/my/daily */
export async function getMyDailyStats(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMyDailyStatsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListMapStringObject>('/stats/my/daily', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取我的综合统计数据 GET /stats/my/summary */
export async function getMySummaryStats(options?: { [key: string]: any }) {
  return request<API.BaseResponseUserSummaryStatsVO>('/stats/my/summary', {
    method: 'GET',
    ...(options || {}),
  })
}
