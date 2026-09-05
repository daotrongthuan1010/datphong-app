import client from './client'
import { ENDPOINTS } from '../utils/constants'

// Tạo/sửa user gửi dạng multipart: file ảnh + trường "user" chứa JSON
function toFormData(user, file) {
  const fd = new FormData()
  fd.append('user', JSON.stringify(user))
  if (file) fd.append('file', file)
  return fd
}

export const userApi = {
  list: (params = {}) => client.get(ENDPOINTS.users, { params }).then((r) => r.data.data),
  get: (id) => client.get(ENDPOINTS.user(id)).then((r) => r.data.data),
  create: (user, file) => client.post(ENDPOINTS.users, toFormData(user, file), { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data.data),
  update: (id, user, file) => client.put(ENDPOINTS.user(id), toFormData(user, file), { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data.data),
  remove: (id) => client.delete(ENDPOINTS.user(id)).then((r) => r.data.data),
  exportExcel: (params = {}) => client.get(ENDPOINTS.usersExcel, { params }).then((r) => r.data),
  importExcel: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return client.post(ENDPOINTS.usersImport, fd, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data)
  },
}
