import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { authApi } from '../api/auth'
import { STORAGE_KEYS } from '../utils/constants'
import { getStoredAuth, storeAuth, clearAuth } from '../api/client'

/** Buoc 1: username+password -> tra ve LoginTwoFactorChallengeResponse (khong cap token). */
export const loginChallengeThunk = createAsyncThunk('auth/loginChallenge', async ({ username, password }, { rejectWithValue }) => {
  try {
    const data = await authApi.login({ username, password })
    return data
  } catch (e) {
    return rejectWithValue(e.message)
  }
})

/** Buoc 2: loginToken + code 6 so -> tra ve AuthTokenResponse. */
export const completeLoginThunk = createAsyncThunk('auth/completeLogin', async ({ loginToken, code }, { rejectWithValue }) => {
  try {
    const data = await authApi.loginTotp({ loginToken, code })
    const payload = { user: data.user, token: data.accessToken, refreshToken: data.refreshToken, expiresIn: data.expiresIn }
    storeAuth(payload)
    return payload
  } catch (e) {
    return rejectWithValue(e.message)
  }
})

/** Tuong thich cu: cho noi goi 1 buoc username+password (neu BE cu tra token thang). */
export const loginThunk = createAsyncThunk('auth/login', async ({ username, password }, { rejectWithValue }) => {
  try {
    const data = await authApi.login({ username, password })
    if (data?.requiresTwoFactor || data?.loginToken) return rejectWithValue('Cần nhập OTP — hãy nhập mã 6 số từ Google/Microsoft Authenticator')
    const tokenData = data
    const payload = { user: tokenData.user, token: tokenData.accessToken, refreshToken: tokenData.refreshToken, expiresIn: tokenData.expiresIn }
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
  // Slice OTP dang nhap dang cho: { requiresTwoFactor, setupRequired, loginToken, expiresIn, qrCodeDataUri, otpAuthUri, secret, issuer, username }
  pendingChallenge: null,
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
      state.pendingChallenge = null
      state.error = null
      clearAuth()
    },
    clearError(state) {
      state.error = null
    },
    clearPendingChallenge(state) {
      state.pendingChallenge = null
    },
  },
  extraReducers: (b) => {
    b
      .addCase(loginChallengeThunk.pending, (s) => {
        s.loading = true
        s.error = null
      })
      .addCase(loginChallengeThunk.fulfilled, (s, a) => {
        s.loading = false
        s.pendingChallenge = a.payload
      })
      .addCase(loginChallengeThunk.rejected, (s, a) => {
        s.loading = false
        s.error = a.payload
        s.pendingChallenge = null
      })
      .addCase(completeLoginThunk.pending, (s) => {
        s.loading = true
        s.error = null
      })
      .addCase(completeLoginThunk.fulfilled, (s, a) => {
        s.loading = false
        s.user = a.payload.user
        s.token = a.payload.token
        s.refreshToken = a.payload.refreshToken
        s.isAuthenticated = true
        s.pendingChallenge = null
      })
      .addCase(completeLoginThunk.rejected, (s, a) => {
        s.loading = false
        s.error = a.payload
      })
      .addCase(loginThunk.pending, (s) => {
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
        s.pendingChallenge = null
      })
  },
})

export const { logout, setAuth, clearError, clearPendingChallenge } = authSlice.actions
export default authSlice.reducer

export function hasRole(user, role) {
  if (!user?.roles) return false
  const roles = Array.isArray(user.roles) ? user.roles : [...user.roles]
  return roles.map((r) => String(r).toUpperCase()).includes(role.toUpperCase())
}
export function isAdmin(user) {
  return hasRole(user, 'ADMIN')
}
