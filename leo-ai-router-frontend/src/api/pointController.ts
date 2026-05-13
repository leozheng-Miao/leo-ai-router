// @ts-ignore
/* eslint-disable */
import request from '@/request'

export async function listMyPointTransactions(
  params: { pageNum?: number; pageSize?: number },
  options?: { [key: string]: any },
) {
  return request<any>('/points/transactions/my', {
    method: 'GET',
    params: { pageNum: 1, pageSize: 10, ...params },
    ...(options || {}),
  })
}
