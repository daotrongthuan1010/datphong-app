import client from './client'
import { ENDPOINTS } from '../utils/constants'

// GET ?roomId=&page=&size= -> { content, page, size, totalElements, avgRating }
// POST multipart: field "review" (JSON) + files (ảnh/video)
export const reviewApi = {
  listByRoom: (roomId, params = {}) =>
    client.get(ENDPOINTS.reviews, { params: { roomId, ...params } }).then((r) => r.data.data),
  create: (review, files = []) => {
    const fd = new FormData()
    fd.append('review', JSON.stringify(review))
    files.forEach((f) => fd.append('files', f))
    return client.post(ENDPOINTS.reviews, fd, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 }).then((r) => r.data.data)
  },
}
