import client from './client'
import { ENDPOINTS } from '../utils/constants'

export const roomApi = {
  list: (params = {}) => client.get(ENDPOINTS.rooms, { params }).then((r) => r.data.data),
  get: (id) => client.get(ENDPOINTS.room(id)).then((r) => r.data.data),
  create: (data) => client.post(ENDPOINTS.rooms, data).then((r) => r.data.data),
  update: (id, data) => client.put(ENDPOINTS.room(id), data).then((r) => r.data.data),
  remove: (id) => client.delete(ENDPOINTS.room(id)).then((r) => r.data.data),
  // Upload 1 ảnh/video lên MinIO (trả về { url })
  uploadMedia: (id, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return client.post(ENDPOINTS.roomMedia(id), fd, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data.data)
  },
  // Upload nhiều ảnh/video cùng lúc (trả về { urls: [...] })
  uploadMediaMany: (id, files) => {
    const fd = new FormData()
    files.forEach((f) => fd.append('files', f))
    return client.post(ENDPOINTS.roomMedia(id), fd, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data.data)
  },
  // Danh sách media của phòng: [{ id, url, mediaType }]
  mediaList: (id) => client.get(ENDPOINTS.roomMedia(id)).then((r) => r.data.data),
  // Xoá một media (ảnh/video) của phòng
  deleteMedia: (id, mediaId) => client.delete(ENDPOINTS.roomMediaDelete(id, mediaId)).then((r) => r.data.data),
}

export const healthApi = {
  check: () => client.get(ENDPOINTS.health).then((r) => r.data.data),
}
