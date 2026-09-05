import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { authApi } from '../api/auth'
import { STORAGE_KEYS } from '../utils/constants'
import { getStoredAuth, storeAuth, clearAuth } from '../api/client'

export const loginThunk = createAsyncThunk('auth/login', async ({ username, password, totpCode }, { rejectWithValue }) => {
  try {
    const data = await authApi.login({ username, password, totpCode: totpCode || undefined })
    // data: { accessToken, refreshToken, tokenType, expiresIn, user }
    const payload = { user: data.user, token: data.accessToken, refreshToken: data.refreshToken, expiresIn: data.expiresIn }
    storeAuth(payload)
    return payload
  } catch (e) {
    return rejectWithValue(e.message)
  }
})

export const logoutThunk = createAsyncThunk('auth/logout', async (_, { getState }) => {
  const refresh = getState().auth.refreshToken || localStorage.getItem('vivu_refresh')
  try {
    if (refresh) await authApi.logout(refresh)
  } finally {
    clearAuth()
  }
})

const stored = getStoredAuth()
const initialState = {
  user: stored?.user || null,
  token: stored?.token || null,
  refreshToken: stored?.refreshToken || localStorage.getItem('vivu_refresh') || null,
  isAuthenticated: !!stored?.token,
  loading: false,
  error: null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setAuth(state, action) {
      const p = action.payload
      state.user = p.user
      state.token = p.token
      state.refreshToken = p.refreshToken || null
      state.isAuthenticated = !!p.token
    },
    logout(state) {
      state.user = null
      state.token = null
      state.refreshToken = null
      state.isAuthenticated = false
      state.error = null
      clearAuth()
    },
    clearError(state) {
      state.error = null
    },
  },
  extraReducers: (b) => {
    b.addCase(loginThunk.pending, (s) => {
      s.loading = true
      s.error = null
    })
      .addCase(loginThunk.fulfilled, (s, a) => {
        s.loading = false
        s.user = a.payload.user
        s.token = a.payload.token
        s.refreshToken = a.payload.refreshToken
        s.isAuthenticated = true
      })
      .addCase(loginThunk.rejected, (s, a) => {
        s.loading = false
        s.error = a.payload
      })
      .addCase(logoutThunk.fulfilled, (s) => {
        s.user = null
        s.token = null
        s.refreshToken = null
        s.isAuthenticated = false
      })
  },
})

export const { logout, setAuth, clearError } = authSlice.actions
export default authSlice.reducer

export function hasRole(user, role) {
  if (!user?.roles) return false
  const roles = Array.isArray(user.roles) ? user.roles : [...user.roles]
  return roles.map((r) => String(r).toUpperCase()).includes(role.toUpperCase())
}
export function isAdmin(user) {
  return hasRole(user, 'ADMIN')
}
