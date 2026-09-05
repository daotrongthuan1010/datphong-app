import axios from 'axios'
import { API_BASE_URL, ENDPOINTS, STORAGE_KEYS, AUTH_PREFIX } from '../utils/constants'

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true, // gửi cookie phiên đăng nhập cho các BE còn dùng HttpSession
})

export function getStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.auth) || 'null')
  } catch {
    return null
  }
}

export function storeAuth(auth) {
  localStorage.setItem(STORAGE_KEYS.auth, JSON.stringify(auth))
  localStorage.setItem(STORAGE_KEYS.token, auth.token)
  if (auth.refreshToken) localStorage.setItem('vivu_refresh', auth.refreshToken)
}

export function clearAuth() {
  localStorage.removeItem(STORAGE_KEYS.auth)
  localStorage.removeItem(STORAGE_KEYS.token)
  localStorage.removeItem('vivu_refresh')
}

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEYS.token)
  if (token) config.headers.Authorization = `Bearer ${token}`
  const rid = crypto.randomUUID?.() || `${Date.now()}`
  config.headers['X-Request-Id'] = rid
  return config
})

// Refresh token tu dong khi 401
let refreshing = null
client.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config || {}
    const isAuthPublic = (original.url || '').startsWith(AUTH_PREFIX)
    if (err.response?.status === 401 && !original._retry && !isAuthPublic) {
      original._retry = true
      const refresh = localStorage.getItem('vivu_refresh')
      if (refresh) {
        try {
          refreshing =
            refreshing ||
            axios.post(`${API_BASE_URL}${ENDPOINTS.refreshToken}`, { refreshToken: refresh }).then((r) => r.data.data)
          const tokens = await refreshing
          refreshing = null
          const stored = getStoredAuth() || {}
          storeAuth({ ...stored, token: tokens.accessToken, refreshToken: tokens.refreshToken })
          original.headers.Authorization = `Bearer ${tokens.accessToken}`
          return client(original)
        } catch (e) {
          refreshing = null
          clearAuth()
          window.location.href = '/login'
          return Promise.reject(err)
        }
      }
    }
    const data = err.response?.data
    const msg = Array.isArray(data?.errors) && data.errors.length ? data.errors.join('; ') : data?.message || err.message || 'Lỗi kết nối'
    const normalized = new Error(msg)
    normalized.status = err.response?.status
    normalized.errors = data?.errors
    return Promise.reject(normalized)
  },
)

export default client
