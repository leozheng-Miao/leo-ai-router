// @ts-ignore
/* eslint-disable */
import request from '@/request'

export interface PaymentOrderVO {
  id?: number
  orderNo?: string
  orderType?: string
  productCode?: string
  productName?: string
  amount?: number
  paymentMethod?: string
  paymentId?: string
  status?: string
  displayType?: string
  redirectUrl?: string
  formHtml?: string
  checkoutUrl?: string
  sessionId?: string
  createTime?: string
  updateTime?: string
}

export async function createSubscriptionOrder(
  body: { planCode?: string; paymentMethod?: string },
  options?: { [key: string]: any },
) {
  return request<{ code: number; data?: PaymentOrderVO; message?: string }>('/payment/orders/subscription', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

export async function createPointsOrder(
  body: { packageCode?: string; paymentMethod?: string },
  options?: { [key: string]: any },
) {
  return request<{ code: number; data?: PaymentOrderVO; message?: string }>('/payment/orders/points', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

export async function listMyPaymentOrders(
  params: { pageNum?: number; pageSize?: number },
  options?: { [key: string]: any },
) {
  return request<any>('/payment/orders/my', {
    method: 'GET',
    params: { pageNum: 1, pageSize: 10, ...params },
    ...(options || {}),
  })
}
