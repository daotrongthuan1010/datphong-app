import client from './client'
import { ENDPOINTS } from '../utils/constants'

// Đặt phòng: tạo, xem lịch sử của tôi, xem chi tiết, hủy
export const bookingApi = {
  create: (data) => client.post(ENDPOINTS.bookings, data).then((r) => r.data.data),
  myList: (params = {}) => client.get(ENDPOINTS.bookings, { params }).then((r) => r.data.data),
  get: (id) => client.get(ENDPOINTS.booking(id)).then((r) => r.data.data),
  cancel: (id) => client.post(ENDPOINTS.bookingCancel(id)).then((r) => r.data.data),
}
