// @ts-ignore
/* eslint-disable */
import request from '@/request'

export async function addProvider(
  body: API.ProviderAddRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong>('/provider/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

export async function updateProvider(
  body: API.ProviderUpdateRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/provider/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

export async function deleteProvider(
  body: API.DeleteRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/provider/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

export async function getProviderVoById(
  params: { id: number },
  options?: { [key: string]: any },
) {
  const { id } = params
  return request<API.BaseResponseProviderVO>(`/provider/get/vo/${id}`, {
    method: 'GET',
    ...(options || {}),
  })
}

export async function listProviderVo(options?: { [key: string]: any }) {
  return request<API.BaseResponseListProviderVO>('/provider/list/vo', {
    method: 'GET',
    ...(options || {}),
  })
}

export async function listProviderVoByPage(
  body: API.ProviderQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageProviderVO>('/provider/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
