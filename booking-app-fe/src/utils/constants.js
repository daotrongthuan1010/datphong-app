export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const ENDPOINTS = {
  health: '/api/health',
  rooms: '/api/rooms',
  room: (id) => `/api/rooms/${id}`,
}

export const ROOM_TYPES = ['SINGLE', 'DOUBLE', 'SUITE', 'DELUXE', 'FAMILY']
export const ROOM_STATUS = ['AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'OUT_OF_SERVICE']
