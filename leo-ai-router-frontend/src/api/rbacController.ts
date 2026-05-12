// @ts-ignore
/* eslint-disable */
import request from '@/request'

export async function listRoles(options?: { [key: string]: any }) {
  return request<API.BaseResponseListRole>('/rbac/roles', {
    method: 'GET',
    ...(options || {}),
  })
}

export async function addRole(body: API.Role, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/rbac/roles', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

export async function updateRole(body: API.Role, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/rbac/roles/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

export async function listPermissions(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPermission>('/rbac/permissions', {
    method: 'GET',
    ...(options || {}),
  })
}

export async function setRolePermissions(body: API.RolePermissionRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/rbac/roles/permissions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

export async function listRolePermissions(params: { roleCode: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseSetString>('/rbac/roles/permissions', {
    method: 'GET',
    params,
    ...(options || {}),
  })
}

export async function assignUserRoles(body: API.AssignUserRolesRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/rbac/users/roles', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}
