import client from './client'
import { ENDPOINTS } from '../utils/constants'

export const roomApi = {
  list: (params = {}) => client.get(ENDPOINTS.rooms, { params }).then((r) => r.data),
  get: (id) => client.get(ENDPOINTS.room(id)).then((r) => r.data),
  create: (data) => client.post(ENDPOINTS.rooms, data).then((r) => r.data),
  update: (id, data) => client.put(ENDPOINTS.room(id), data).then((r) => r.data),
  remove: (id) => client.delete(ENDPOINTS.room(id)).then((r) => r.data),
}

export const healthApi = {
  check: () => client.get(ENDPOINTS.health).then((r) => r.data),
}
