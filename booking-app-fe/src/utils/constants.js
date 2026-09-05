export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const ENDPOINTS = {
  health: '/api/health',
  // auth
  login: '/api/auth/login',
  register: '/api/auth/register',
  refreshToken: '/api/auth/refresh-token',
  logout: '/api/auth/logout',
  forgotPassword: '/api/auth/forgot-password',
  resetPassword: '/api/auth/reset-password',
  otpSend: '/api/auth/otp/send',
  otpVerify: '/api/auth/otp/verify',
  twoFactorSetup: '/api/auth/2fa/setup',
  twoFactorConfirm: '/api/auth/2fa/confirm',
  twoFactorDisable: '/api/auth/2fa/disable',
  // resources
  rooms: '/api/rooms',
  room: (id) => `/api/rooms/${id}`,
  users: '/api/users',
  user: (id) => `/api/users/${id}`,
  usersExcel: '/api/users/excel',
  usersImport: '/api/users/import-excel',
  vouchers: '/api/voucher',
  voucher: (id) => `/api/voucher/${id}`,
  hosts: '/api/HostProfile',
  host: (id) => `/api/HostProfile/${id}`,
  roles: '/api/roles',
  bookings: '/api/bookings',
  booking: (id) => `/api/bookings/${id}`,
  bookingCancel: (id) => `/api/bookings/${id}/cancel`,
  reviews: '/api/reviews',
  roomImages: (id) => `/api/rooms/${id}/images`,
  roomMedia: (id) => `/api/rooms/${id}/media`,
  roomMediaDelete: (id, mediaId) => `/api/rooms/${id}/media/${mediaId}`,
}

// Tiền tố nhóm auth — client.js dùng để bỏ qua auto-refresh cho chính các endpoint auth
export const AUTH_PREFIX = '/api/auth/'

// ---- Enum mirror tu BE (giu trung ten de FE render Tag/Select) ----
export const ROOM_TYPES = ['SINGLE', 'DOUBLE', 'SUITE', 'DELUXE', 'FAMILY']
export const ROOM_STATUS = ['AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'OUT_OF_SERVICE']
export const USER_TYPES = ['USER', 'HOST', 'ADMIN']
export const USER_STATUS = ['ACTIVE', 'IN_ACTIVE', 'HOAT_DONG', 'KHONG_HOAT_DONG']
export const HOST_STATUS = ['PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED']
export const DISCOUNT_TYPES = ['PERCENT', 'FIXED_AMOUNT']
export const VOUCHER_OWNER_TYPES = ['SYSTEM', 'HOST']
export const OTP_PURPOSES = ['REGISTER', 'FORGOT_PASSWORD']

export const STORAGE_KEYS = { auth: 'vivu_auth', token: 'vivu_token' }
