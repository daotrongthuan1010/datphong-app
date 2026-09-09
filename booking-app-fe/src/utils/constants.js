// Mac dinh la chuoi rong = goi tuong doi /api/... cung origin:
//   - Dev: vite server proxy /api -> localhost:8080 (xem vite.config.js)
//   - Docker: nginx FE proxy /api -> be:8080 (xem nginx.conf)
// Khong can hardcode IP may nao -> khong con loi CORS khi chay o may khac.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export const ENDPOINTS = {
  health: '/api/health',
  // auth (đăng nhập 2 bước bắt buộc OTP: /login -> {loginToken, qr?}, rồi /login/2fa -> token)
  login: '/api/auth/login',
  loginTotp: '/api/auth/login/2fa',
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
  // Danh muc tien nghi that (khong hardcode trong FE)
  amenities: '/api/amenities',
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
  // thanh toan cho booking HOLD -> checkoutUrl (fake-bank / VNPay)
  payments: '/api/payments',
  payment: (id) => `/api/payments/${id}`,
  paymentByBooking: (bookingId) => `/api/payments/by-booking/${bookingId}`,
  paymentReconcile: (bookingId) => `/api/payments/reconcile/${bookingId}`,
  reviews: '/api/reviews',
  roomImages: (id) => `/api/rooms/${id}/images`,
  roomMedia: (id) => `/api/rooms/${id}/media`,
  roomMediaDelete: (id, mediaId) => `/api/rooms/${id}/media/${mediaId}`,
  roomCalendar: (id) => `/api/rooms/${id}/calendar`,
  // ho so ca nhan (self-service, chi can dang nhap)
  profileMe: '/api/profile/me',
  // loyalty hang + lich su diem
  loyaltyRanks: '/api/loyalty/ranks',
  loyaltyRank: (name) => `/api/loyalty/ranks/${name}`,
  loyaltyMe: '/api/loyalty/me',
  loyaltyHistory: '/api/loyalty/me/history',
  // chat khach - chu nha
  conversations: '/api/conversations',
  conversation: (id) => `/api/conversations/${id}`,
  conversationMessages: (id) => `/api/conversations/${id}/messages`,
  conversationRead: (id) => `/api/conversations/${id}/read`,
  // Bao cao doanh thu (VIEW + MATERIALIZED VIEW + FUNCTION o PostgreSQL)
  statsOverview: '/api/admin/stats/overview',
  statsSeries: '/api/admin/stats/series',
  statsTopRooms: '/api/admin/stats/top-rooms',
  statsOccupancy: '/api/admin/stats/occupancy',
  statsSources: '/api/admin/stats/sources',
  statsRefresh: '/api/admin/stats/refresh',
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
