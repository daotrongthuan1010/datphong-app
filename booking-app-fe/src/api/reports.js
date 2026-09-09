import client from './client'
import { ENDPOINTS } from '../utils/constants'

export const reportApi = {
  overview: (params = {}) =>
    client.get(ENDPOINTS.statsOverview, { params }).then((r) => r.data.data),
  series: (params = {}) =>
    client.get(ENDPOINTS.statsSeries, { params }).then((r) => r.data.data),
  topRooms: (params = {}) =>
    client.get(ENDPOINTS.statsTopRooms, { params }).then((r) => r.data.data),
  occupancy: (params = {}) =>
    client.get(ENDPOINTS.statsOccupancy, { params }).then((r) => r.data.data),
  sources: () =>
    client.get(ENDPOINTS.statsSources).then((r) => r.data.data),
  refresh: () =>
    client.post(ENDPOINTS.statsRefresh).then((r) => r.data.data),
}
