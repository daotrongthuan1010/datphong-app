import client from './client'
import { ENDPOINTS } from '../utils/constants'

export const paymentApi = {
  create: (data) => client.post(ENDPOINTS.payments, data).then((r) => r.data.data),
  byBooking: (bookingId) => client.get(ENDPOINTS.paymentByBooking(bookingId)).then((r) => r.data.data),
  // Doi soat chu dong voi cong — goi khi khach quay ve tu trang checkout (?paid=1),
  // de BE hoi cong trang thai that khi webhook khong goi duoc ve BE local.
  reconcile: (bookingId) => client.post(ENDPOINTS.paymentReconcile(bookingId)).then((r) => r.data.data),
  get: (id) => client.get(ENDPOINTS.payment(id)).then((r) => r.data.data),
}
