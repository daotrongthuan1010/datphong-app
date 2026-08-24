import { Routes, Route, Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import AppLayout from './components/AppLayout'
import Login from './pages/Login'
import Home from './pages/Home'

function RequireAuth({ children }) {
  const isAuth = useSelector((s) => s.auth.isAuthenticated)
  if (!isAuth) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <AppLayout>
              <Home />
            </AppLayout>
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
