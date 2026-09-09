import client from './client'
import { ENDPOINTS } from '../utils/constants'

export const loyaltyApi = {
  listRanks: (params = {}) => client.get(ENDPOINTS.loyaltyRanks, { params }).then((r) => r.data.data),
  getRank: (name) => client.get(ENDPOINTS.loyaltyRank(name)).then((r) => r.data.data),
  myProfile: () => client.get(ENDPOINTS.loyaltyMe).then((r) => r.data.data),
  myHistory: (params = {}) => client.get(ENDPOINTS.loyaltyHistory, { params }).then((r) => r.data.data),
}
