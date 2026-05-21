import axios from 'axios'
import { message } from 'ant-design-vue'
import { clearAuthTokens, getAccessToken, getRefreshToken, saveAuthTokens } from '@/utils/authToken'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    const token = getAccessToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  function (error) {
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  async function (response) {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !response.request.responseURL.includes('user/token/refresh') &&
        !window.location.pathname.includes('/user/login')
      ) {
        const refreshToken = getRefreshToken()
        if (refreshToken && !(response.config as any).__isRetryRequest) {
          try {
            ;(response.config as any).__isRetryRequest = true
            const refreshRes = await axios.post(
              `${API_BASE_URL}/user/token/refresh`,
              {
                refreshToken,
              },
              {
                withCredentials: true,
              },
            )
            if (refreshRes.data.code === 0 && refreshRes.data.data?.accessToken) {
              saveAuthTokens(refreshRes.data.data.accessToken, refreshRes.data.data.refreshToken)
              response.config.headers = response.config.headers || {}
              response.config.headers.Authorization = `Bearer ${refreshRes.data.data.accessToken}`
              return myAxios.request(response.config)
            }
          } catch {
            clearAuthTokens()
          }
        }
        clearAuthTokens()
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${encodeURIComponent(window.location.href)}`
      }
    }
    return response
  },
  function (error) {
    return Promise.reject(error)
  },
)

export default myAxios
