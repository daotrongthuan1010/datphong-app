import { Layout, Menu, Button, Avatar, Dropdown, Space, Badge } from 'antd'
import { HomeOutlined, LoginOutlined, LogoutOutlined, UserOutlined, ShoppingCartOutlined } from '@ant-design/icons'
import { useSelector, useDispatch } from 'react-redux'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { logout } from '../store/authSlice'

const { Header, Content, Footer } = Layout

export default function AppLayout({ children }) {
  const { isAuthenticated, user } = useSelector((s) => s.auth)
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()

  const menuItems = [
    { key: '/', icon: <HomeOutlined />, label: <Link to="/">Khách sạn</Link> },
  ]

  const userMenu = {
    items: [
      { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true },
    ],
    onClick: ({ key }) => {
      if (key === 'logout') {
        dispatch(logout())
        navigate('/login')
      }
    },
  }

  return (
    <Layout style={{ minHeight: '100vh', background: '#f5f7fb' }}>
      <Header
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#fff',
          borderBottom: '1px solid #e8e8e8',
          padding: '0 24px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 8, textDecoration: 'none' }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 10,
                background: 'linear-gradient(135deg,#1a73e8 0%,#6c5ce7 100%)',
                display: 'grid',
                placeItems: 'center',
                color: '#fff',
                fontWeight: 800,
                fontSize: 16,
              }}
            >
              V
            </div>
            <span style={{ fontWeight: 800, fontSize: 20, color: '#1a1a2e', letterSpacing: -0.5 }}>VIVU</span>
            <span style={{ fontSize: 12, color: '#888', fontWeight: 500, marginLeft: 4, borderLeft: '1px solid #e8e8e8', paddingLeft: 8 }}>
              Đặt phòng
            </span>
          </Link>
          <Menu
            mode="horizontal"
            selectedKeys={[location.pathname]}
            items={menuItems}
            style={{ borderBottom: 'none', minWidth: 160 }}
          />
        </div>

        <Space size={16}>
          <Badge count={0} showZero={false}>
            <Button icon={<ShoppingCartOutlined />} shape="circle" />
          </Badge>
          {isAuthenticated ? (
            <Dropdown menu={userMenu} placement="bottomRight">
              <Space style={{ cursor: 'pointer' }}>
                <Avatar icon={<UserOutlined />} style={{ background: '#1a73e8' }} />
                <span style={{ fontWeight: 600, maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {user?.username || 'Tài khoản'}
                </span>
              </Space>
            </Dropdown>
          ) : (
            <Button type="primary" icon={<LoginOutlined />} onClick={() => navigate('/login')}>
              Đăng nhập
            </Button>
          )}
        </Space>
      </Header>

      <Content style={{ padding: 0 }}>{children}</Content>

      <Footer style={{ textAlign: 'center', background: '#fff', borderTop: '1px solid #e8e8e8', color: '#888' }}>
        VIVU Booking — Demo Servlet + Hibernate + React &copy; {new Date().getFullYear()} · BE: <code>localhost:8080</code> · FE: <code>localhost:3000</code>
      </Footer>
    </Layout>
  )
}
