import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { roomApi } from '../api/rooms'

export const fetchRooms = createAsyncThunk('room/fetchRooms', async (params, { rejectWithValue }) => {
  try {
    const data = await roomApi.list(params)
    // BE tra PageResponse { content, page, size, totalElements, totalPages }
    // client da unwrap r.data.data
    return data
  } catch (e) {
    return rejectWithValue(e.message)
  }
})

export const fetchRoom = createAsyncThunk('room/fetchRoom', async (id, { rejectWithValue }) => {
  try {
    return await roomApi.get(id)
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
        const p = a.payload || {}
        s.list = p.content || []
        s.page = p.page ?? 0
        s.size = p.size ?? 12
        s.totalElements = p.totalElements ?? 0
        s.totalPages = p.totalPages ?? 0
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
