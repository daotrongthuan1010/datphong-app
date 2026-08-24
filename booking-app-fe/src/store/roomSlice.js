import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { roomApi } from '../api/rooms'

export const fetchRooms = createAsyncThunk('room/fetchRooms', async (params, { rejectWithValue }) => {
  try {
    const res = await roomApi.list(params)
    return res.data
  } catch (e) {
    return rejectWithValue(e.message)
  }
})

export const fetchRoom = createAsyncThunk('room/fetchRoom', async (id, { rejectWithValue }) => {
  try {
    const res = await roomApi.get(id)
    return res.data
  } catch (e) {
    return rejectWithValue(e.message)
  }
})

const roomSlice = createSlice({
  name: 'room',
  initialState: {
    list: [],
    page: 0,
    size: 12,
    totalElements: 0,
    totalPages: 0,
    current: null,
    loading: false,
    error: null,
  },
  reducers: {
    clearCurrent(state) {
      state.current = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRooms.pending, (s) => {
        s.loading = true
        s.error = null
      })
      .addCase(fetchRooms.fulfilled, (s, a) => {
        s.loading = false
        s.list = a.payload.content || []
        s.page = a.payload.page ?? 0
        s.size = a.payload.size ?? 12
        s.totalElements = a.payload.totalElements ?? 0
        s.totalPages = a.payload.totalPages ?? 0
      })
      .addCase(fetchRooms.rejected, (s, a) => {
        s.loading = false
        s.error = a.payload
      })
      .addCase(fetchRoom.fulfilled, (s, a) => {
        s.current = a.payload
      })
  },
})

export const { clearCurrent } = roomSlice.actions
export default roomSlice.reducer
