import client from './client'
import { ENDPOINTS } from '../utils/constants'

// list params: type (VoucherOwnerType), usage (DiscountTypeEnum), q, page, size
export const voucherApi = {
  list: (params = {}) => client.get(ENDPOINTS.vouchers, { params }).then((r) => r.data.data),
  get: (id) => client.get(ENDPOINTS.voucher(id)).then((r) => r.data.data),
  create: (data) => client.post(ENDPOINTS.vouchers, data).then((r) => r.data.data),
  update: (id, data) => client.put(ENDPOINTS.voucher(id), data).then((r) => r.data.data),
  remove: (id) => client.delete(ENDPOINTS.voucher(id)).then((r) => r.data.data),
}

export const hostApi = {
  // list params: status (HostStatus), q, page, size
  list: (params = {}) => client.get(ENDPOINTS.hosts, { params }).then((r) => r.data.data),
  get: (id) => client.get(ENDPOINTS.host(id)).then((r) => r.data.data),
  create: (data) => client.post(ENDPOINTS.hosts, data).then((r) => r.data.data),
  update: (id, data) => client.put(ENDPOINTS.host(id), data).then((r) => r.data.data),
  remove: (id) => client.delete(ENDPOINTS.host(id)).then((r) => r.data.data),
}
