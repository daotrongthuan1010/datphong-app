import client from './client'
import { ENDPOINTS } from '../utils/constants'

// Đăng nhập trả về { accessToken, refreshToken, tokenType, expiresIn, user }
export const authApi = {
  login: (data) => client.post(ENDPOINTS.login, data).then((r) => r.data.data),
  register: (data) => client.post(ENDPOINTS.register, data).then((r) => r.data.data),
  sendOtp: (data) => client.post(ENDPOINTS.otpSend, data).then((r) => r.data),
  verifyOtp: (data) => client.post(ENDPOINTS.otpVerify, data).then((r) => r.data),
  forgotPassword: (data) => client.post(ENDPOINTS.forgotPassword, data).then((r) => r.data),
  resetPassword: (data) => client.post(ENDPOINTS.resetPassword, data).then((r) => r.data),
  logout: (refreshToken) => client.post(ENDPOINTS.logout, { refreshToken }).then((r) => r.data),
  twoFactorSetup: () => client.post(ENDPOINTS.twoFactorSetup).then((r) => r.data.data),
  twoFactorConfirm: (code) => client.post(ENDPOINTS.twoFactorConfirm, { code }).then((r) => r.data),
  twoFactorDisable: (code) => client.post(ENDPOINTS.twoFactorDisable, { code }).then((r) => r.data),
}
