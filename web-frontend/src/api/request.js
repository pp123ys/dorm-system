import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

export const TOKEN_KEY = 'dorm_token'
export const USER_KEY = 'dorm_user'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：带上 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['X-Token'] = token
  }
  return config
})

// 响应拦截：统一处理业务错误与登录失效
request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端约定：code=0 成功；code=1 业务校验失败，message 为中文原因
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return body
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      // token 失效：清干净并跳回登录页
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      ElMessage.error('登录已失效，请重新登录')
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
      return Promise.reject(error)
    }
    const msg =
      (error.response && error.response.data && error.response.data.message) ||
      error.message ||
      '网络异常，请确认后端服务已启动'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
