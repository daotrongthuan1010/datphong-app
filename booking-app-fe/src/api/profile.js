import client from './client'
import { ENDPOINTS } from '../utils/constants'

export const profileApi = {
  getMe: () => client.get(ENDPOINTS.profileMe).then((r) => r.data.data),
  updateMe: (profile, file) => {
    if (file) {
      const fd = new FormData()
      fd.append('profile', JSON.stringify(profile))
      fd.append('file', file)
      return client.put(ENDPOINTS.profileMe, fd, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data.data)
    }
    return client.put(ENDPOINTS.profileMe, profile).then((r) => r.data.data)
  },
}
