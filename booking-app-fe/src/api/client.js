import axios from 'axios'
import { API_BASE_URL } from '../utils/constants'

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('vivu_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  const rid = crypto.randomUUID?.() || `${Date.now()}`
  config.headers['X-Request-Id'] = rid
  return config
})

client.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg = err.response?.data?.message || err.message || 'Lỗi kết nối'
    return Promise.reject(new Error(msg))
  },
)

export default client
