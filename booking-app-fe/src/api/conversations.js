import client from './client'
import { ENDPOINTS } from '../utils/constants'

export const conversationApi = {
  list: (params = {}) => client.get(ENDPOINTS.conversations, { params }).then((r) => r.data.data),
  get: (id) => client.get(ENDPOINTS.conversation(id)).then((r) => r.data.data),
  getOrCreate: ({ hostId, roomId }) => client.post(ENDPOINTS.conversations, { hostId, roomId }).then((r) => r.data.data),
  listMessages: (conversationId, params = {}) => client.get(ENDPOINTS.conversationMessages(conversationId), { params }).then((r) => r.data.data),
  sendMessage: (conversationId, content) => client.post(ENDPOINTS.conversationMessages(conversationId), { content }).then((r) => r.data.data),
  markRead: (conversationId) => client.post(ENDPOINTS.conversationRead(conversationId)).then((r) => r.data),
}
