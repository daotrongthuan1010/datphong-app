export function formatPrice(vnd) {
  if (vnd == null) return '-'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(vnd)
}

export function roomTypeLabel(t) {
  const m = { SINGLE: 'Phòng đơn', DOUBLE: 'Phòng đôi', SUITE: 'Suite', DELUXE: 'Deluxe', FAMILY: 'Gia đình' }
  return m[t] || t
}

export function roomStatusColor(s) {
  const m = { AVAILABLE: 'green', OCCUPIED: 'red', MAINTENANCE: 'orange', OUT_OF_SERVICE: 'default' }
  return m[s] || 'default'
}
