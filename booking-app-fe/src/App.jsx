import { Routes, Route, Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import AppLayout from './components/AppLayout'
import Login from './pages/Login'
import Register from './pages/Register'
import ForgotPassword from './pages/ForgotPassword'
import Home from './pages/Home'
import RoomDetail from './pages/RoomDetail'
import MyBookings from './pages/MyBookings'
import Checkout from './pages/Checkout'
import Profile from './pages/Profile'
import LoyaltyHistory from './pages/LoyaltyHistory'
import Chat from './pages/Chat'
import Dashboard from './pages/admin/Dashboard'
import AdminRooms from './pages/admin/rooms/RoomsList'
import AdminUsers from './pages/admin/users/UsersList'
import AdminVouchers from './pages/admin/vouchers/VouchersList'
import AdminHosts from './pages/admin/hosts/HostsList'
import RevenueDashboard from './pages/admin/revenue/RevenueDashboard'
import { isAdmin } from './store/authSlice'

function RequireAuth({ children }) {
  const { isAuthenticated } = useSelector((s) => s.auth)
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return children
}

function RequireAdmin({ children }) {
  const { isAuthenticated, user } = useSelector((s) => s.auth)
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (!isAdmin(user)) return <Navigate to="/" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />

      {/* Khách có thể xem danh sách + chi tiết phòng không cần đăng nhập */}
      <Route
        path="/"
        element={
          <AppLayout>
            <Home />
          </AppLayout>
        }
      />
      <Route
        path="/rooms/:id"
        element={
          <AppLayout>
            <RoomDetail />
          </AppLayout>
        }
      />

      {/* Đặt phòng của tôi — cần đăng nhập (JWT/Bearer hoặc session cookie) */}
      <Route
        path="/checkout/:id"
        element={
          <RequireAuth>
            <AppLayout>
              <Checkout />
            </AppLayout>
          </RequireAuth>
        }
      />
      <Route
        path="/bookings"
        element={
          <RequireAuth>
            <AppLayout>
              <MyBookings />
            </AppLayout>
          </RequireAuth>
        }
      />

      <Route path="/profile" element={<RequireAuth><AppLayout><Profile /></AppLayout></RequireAuth>} />
      <Route path="/loyalty" element={<RequireAuth><AppLayout><LoyaltyHistory /></AppLayout></RequireAuth>} />
      <Route path="/messages" element={<RequireAuth><AppLayout><Chat /></AppLayout></RequireAuth>} />
      <Route path="/messages/:id" element={<RequireAuth><AppLayout><Chat /></AppLayout></RequireAuth>} />

      {/* Quản trị (yêu cầu ADMIN) */}
      <Route
        path="/admin"
        element={
          <RequireAdmin>
            <AppLayout>
              <Dashboard />
            </AppLayout>
          </RequireAdmin>
        }
      />
      <Route
        path="/admin/rooms"
        element={
          <RequireAdmin>
            <AppLayout>
              <AdminRooms />
            </AppLayout>
          </RequireAdmin>
        }
      />
      <Route
        path="/admin/users"
        element={
          <RequireAdmin>
            <AppLayout>
              <AdminUsers />
            </AppLayout>
          </RequireAdmin>
        }
      />
      <Route
        path="/admin/vouchers"
        element={
          <RequireAdmin>
            <AppLayout>
              <AdminVouchers />
            </AppLayout>
          </RequireAdmin>
        }
      />
      <Route
        path="/admin/hosts"
        element={
          <RequireAdmin>
            <AppLayout>
              <AdminHosts />
            </AppLayout>
          </RequireAdmin>
        }
      />
      <Route
        path="/admin/revenue"
        element={
          <RequireAdmin>
            <AppLayout>
              <RevenueDashboard />
            </AppLayout>
          </RequireAdmin>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
