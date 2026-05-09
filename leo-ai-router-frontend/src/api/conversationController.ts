// @ts-ignore
/* eslint-disable */
import request from '@/request'

export type ConversationVO = {
  id?: number
  title?: string
  convType?: number
  lastMessageAt?: string
  lastMessagePreview?: string
}

export type MessageVO = {
  id?: number
  role?: 'user' | 'assistant' | 'system' | 'tool'
  content?: string
  mode?: number
  createdAt?: string
  seq?: number
}

export type PageResult<T> = {
  records?: T[]
  totalRow?: number
  pageNumber?: number
  pageSize?: number
}

export type BaseResponse<T> = {
  code?: number
  data?: T
  message?: string
}

export type CreateConversationRequest = {
  convType?: number
}

export type SendMessageRequest = {
  content: string
  mode: number
}

const buildUserHeader = (userId: number) => ({
  'X-User-Id': String(userId),
})

export async function createConversation(
  userId: number,
  body: CreateConversationRequest = { convType: 1 },
  options?: { [key: string]: any },
) {
  return request<BaseResponse<{ conversationId: number }>>('/conversations', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...buildUserHeader(userId),
    },
    data: body,
    ...(options || {}),
  })
}

export async function listConversations(
  userId: number,
  params: { page?: number; size?: number } = {},
  options?: { [key: string]: any },
) {
  return request<BaseResponse<PageResult<ConversationVO>>>('/conversations', {
    method: 'GET',
    headers: buildUserHeader(userId),
    params,
    ...(options || {}),
  })
}

export async function listConversationMessages(
  userId: number,
  conversationId: number,
  params: { page?: number; size?: number } = {},
  options?: { [key: string]: any },
) {
  return request<BaseResponse<PageResult<MessageVO>>>(`/conversations/${conversationId}/messages`, {
    method: 'GET',
    headers: buildUserHeader(userId),
    params,
    ...(options || {}),
  })
}

export async function sendConversationMessage(
  userId: number,
  conversationId: number,
  body: SendMessageRequest,
  options?: { [key: string]: any },
) {
  return request<BaseResponse<MessageVO>>(`/conversations/${conversationId}/messages/send`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...buildUserHeader(userId),
    },
    data: body,
    ...(options || {}),
  })
}

export async function streamConversationMessage(
  userId: number,
  conversationId: number,
  body: SendMessageRequest,
) {
  const baseURL = String(request.defaults.baseURL ?? '')
  return fetch(`${baseURL}/conversations/${conversationId}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...buildUserHeader(userId),
    },
    credentials: 'include',
    body: JSON.stringify(body),
  })
}

export async function deleteConversation(
  userId: number,
  conversationId: number,
  options?: { [key: string]: any },
) {
  return request<BaseResponse<boolean>>(`/conversations/${conversationId}`, {
    method: 'DELETE',
    headers: buildUserHeader(userId),
    ...(options || {}),
  })
}
