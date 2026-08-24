import { createSlice } from '@reduxjs/toolkit'

const stored = (() => {
  try {
    return JSON.parse(localStorage.getItem('vivu_auth') || 'null')
  } catch {
    return null
  }
})()

const initialState = {
  user: stored?.user || null,
  token: stored?.token || null,
  isAuthenticated: !!stored?.token,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    login(state, action) {
      const { user, token } = action.payload
      state.user = user
      state.token = token
      state.isAuthenticated = true
      localStorage.setItem('vivu_auth', JSON.stringify({ user, token }))
      if (token) localStorage.setItem('vivu_token', token)
    },
    logout(state) {
      state.user = null
      state.token = null
      state.isAuthenticated = false
      localStorage.removeItem('vivu_auth')
      localStorage.removeItem('vivu_token')
    },
  },
})

export const { login, logout } = authSlice.actions
export default authSlice.reducer
