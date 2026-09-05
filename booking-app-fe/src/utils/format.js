// Mapping enum BE -> nhãn tiếng Việt + màu Tag dùng chung toàn app.
// 1 chỗ duy nhất: DataTable / FilterBar / Form / Detail đều lấy từ đây.

export function formatPrice(vnd) {
  if (vnd == null) return '-'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(vnd)
}

export function formatDateTime(s) {
  if (!s) return '-'
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) return s
  return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export function formatDate(s) {
  if (!s) return '-'
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) return s
  return d.toLocaleDateString('vi-VN')
}

// ---- Room ----
export function roomTypeLabel(t) {
  const m = { SINGLE: 'Phòng đơn', DOUBLE: 'Phòng đôi', SUITE: 'Suite', DELUXE: 'Deluxe', FAMILY: 'Gia đình' }
  return m[t] || t
}
export const roomTypeColor = { SINGLE: 'blue', DOUBLE: 'cyan', SUITE: 'purple', DELUXE: 'gold', FAMILY: 'magenta' }

export function roomStatusLabel(s) {
  const m = { AVAILABLE: 'Trống', OCCUPIED: 'Đang ở', MAINTENANCE: 'Bảo trì', OUT_OF_SERVICE: 'Ngừng phục vụ' }
  return m[s] || s
}
export const roomStatusColor = { AVAILABLE: 'green', OCCUPIED: 'red', MAINTENANCE: 'orange', OUT_OF_SERVICE: 'default' }

// ---- User ----
export function userStatusLabel(s) {
  const m = { ACTIVE: 'Hoạt động', IN_ACTIVE: 'Không hoạt động', HOAT_DONG: 'Hoạt động', KHONG_HOAT_DONG: 'Không hoạt động' }
  return m[s] || s
}
export const userStatusColor = { ACTIVE: 'green', IN_ACTIVE: 'red', HOAT_DONG: 'green', KHONG_HOAT_DONG: 'red' }

export function userTypeLabel(t) {
  const m = { USER: 'Khách', HOST: 'Chủ nhà', ADMIN: 'Quản trị' }
  return m[t] || t
}
export const userTypeColor = { USER: 'blue', HOST: 'gold', ADMIN: 'red' }

// ---- Host ----
export function hostStatusLabel(s) {
  const m = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', SUSPENDED: 'Khóa' }
  return m[s] || s
}
export const hostStatusColor = { PENDING: 'orange', APPROVED: 'green', REJECTED: 'red', SUSPENDED: 'default' }

// ---- Voucher ----
export function discountTypeLabel(t) {
  const m = { PERCENT: '% Giảm giá', FIXED_AMOUNT: 'Giảm tiền' }
  return m[t] || t
}
export const discountTypeColor = { PERCENT: 'volcano', FIXED_AMOUNT: 'geekblue' }

export function formatDiscountValue(type, value) {
  if (value == null) return '-'
  return type === 'PERCENT' ? `${Number(value)}%` : formatPrice(Number(value))
}

export function voucherOwnerLabel(t) {
  const m = { SYSTEM: 'Hệ thống', HOST: 'Chủ nhà' }
  return m[t] || t
}
export const voucherOwnerColor = { SYSTEM: 'purple', HOST: 'gold' }

export function genderLabel(g) {
  return g === true ? 'Nam' : g === false ? 'Nữ' : '-'
}

// ---- Booking ----
export function bookingStatusLabel(s) {
  const m = {
    HOLD: 'Giữ chỗ',
    PENDING_PAYMENT: 'Chờ thanh toán',
    CONFIRMED: 'Đã xác nhận',
    COMPLETED: 'Hoàn thành',
    CANCELLED: 'Đã hủy',
    REFUNDED: 'Đã hoàn tiền',
  }
  return m[s] || s
}
export const bookingStatusColor = {
  HOLD: 'orange',
  PENDING_PAYMENT: 'gold',
  CONFIRMED: 'green',
  COMPLETED: 'blue',
  CANCELLED: 'red',
  REFUNDED: 'purple',
}
