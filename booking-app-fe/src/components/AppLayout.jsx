import { Layout, Menu, Button, Avatar, Dropdown, Space, Badge, Tag } from 'antd'
import {
  HomeOutlined,
  LoginOutlined,
  LogoutOutlined,
  UserOutlined,
  ShoppingCartOutlined,
  SettingOutlined,
  TeamOutlined,
  GiftOutlined,
  ApartmentOutlined,
  CrownOutlined,
  SafetyCertificateOutlined,
  CalendarOutlined,
} from '@ant-design/icons'
import { useSelector, useDispatch } from 'react-redux'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { logoutThunk, isAdmin } from '../store/authSlice'

const { Header, Content, Footer } = Layout

export default function AppLayout({ children }) {
  const { isAuthenticated, user } = useSelector((s) => s.auth)
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const admin = isAdmin(user)

  const menuItems = [
    { key: '/', icon: <HomeOutlined />, label: <Link to="/">Khách sạn</Link> },
    ...(isAuthenticated ? [{ key: '/bookings', icon: <CalendarOutlined />, label: <Link to="/bookings">Đặt phòng của tôi</Link> }] : []),
    ...(admin
      ? [
          {
            key: '/admin',
            icon: <SettingOutlined />,
            label: 'Quản trị',
            children: [
              { key: '/admin/rooms', icon: <ApartmentOutlined />, label: <Link to="/admin/rooms">Phòng</Link> },
              { key: '/admin/users', icon: <TeamOutlined />, label: <Link to="/admin/users">Người dùng</Link> },
              { key: '/admin/vouchers', icon: <GiftOutlined />, label: <Link to="/admin/vouchers">Voucher</Link> },
              { key: '/admin/hosts', icon: <CrownOutlined />, label: <Link to="/admin/hosts">Chủ nhà</Link> },
            ],
          },
        ]
      : []),
  ]

  const selectedKeys = (() => {
    const p = location.pathname
    if (p.startsWith('/bookings')) return ['/bookings']
    if (p.startsWith('/admin/rooms')) return ['/admin/rooms']
    if (p.startsWith('/admin/users')) return ['/admin/users']
    if (p.startsWith('/admin/vouchers')) return ['/admin/vouchers']
    if (p.startsWith('/admin/hosts')) return ['/admin/hosts']
    if (p === '/') return ['/']
    return [p]
  })()

  const userMenu = {
    items: [
      { key: 'profile', icon: <UserOutlined />, label: `Chào, ${user?.username || user?.fullName || 'bạn'}`, disabled: true },
      { key: 'bookings', icon: <CalendarOutlined />, label: 'Đặt phòng của tôi' },
      ...(admin ? [{ key: 'admin', icon: <SettingOutlined />, label: 'Quản trị' }] : []),
      { type: 'divider' },
      { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true },
    ],
    onClick: ({ key }) => {
      if (key === 'logout') dispatch(logoutThunk()).finally(() => navigate('/login'))
      if (key === 'admin') navigate('/admin/rooms')
      if (key === 'bookings') navigate('/bookings')
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
          padding: '0 16px 0 24px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
          gap: 16,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, minWidth: 0 }}>
          <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 8, textDecoration: 'none', flexShrink: 0 }}>
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
            <span
              style={{
                fontSize: 12,
                color: '#888',
                fontWeight: 500,
                marginLeft: 4,
                borderLeft: '1px solid #e8e8e8',
                paddingLeft: 8,
                whiteSpace: 'nowrap',
              }}
            >
              Đặt phòng
            </span>
          </Link>
          <Menu mode="horizontal" selectedKeys={selectedKeys} items={menuItems} style={{ borderBottom: 'none', flex: 1, minWidth: 160 }} overflowedIndicator={<SettingOutlined />} />
        </div>

        <Space size={12} style={{ flexShrink: 0 }}>
          {admin && (
            <Tag color="red" icon={<SafetyCertificateOutlined />} style={{ margin: 0, borderRadius: 999 }}>
              ADMIN
            </Tag>
          )}
          <Badge count={0} showZero={false}>
            <Button icon={<ShoppingCartOutlined />} shape="circle" />
          </Badge>
          {isAuthenticated ? (
            <Dropdown menu={userMenu} placement="bottomRight">
              <Space style={{ cursor: 'pointer' }}>
                <Avatar icon={<UserOutlined />} style={{ background: '#1a73e8' }} src={user?.avatar || undefined} />
                <span
                  style={{
                    fontWeight: 600,
                    maxWidth: 140,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {user?.username || user?.fullName || 'Tài khoản'}
                </span>
              </Space>
            </Dropdown>
          ) : (
            <Space>
              <Button onClick={() => navigate('/register')}>Đăng ký</Button>
              <Button type="primary" icon={<LoginOutlined />} onClick={() => navigate('/login')}>
                Đăng nhập
              </Button>
            </Space>
          )}
        </Space>
      </Header>

      <Content style={{ padding: 0 }}>{children}</Content>

      <Footer style={{ textAlign: 'center', background: '#fff', borderTop: '1px solid #e8e8e8', color: '#888', padding: '16px 24px' }}>
        <div style={{ maxWidth: 1200, margin: '0 auto', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12, fontSize: 12 }}>
          <span>
            VIVU — Đặt phòng thảnh thơi, vi vu khắp nơi · © 2026 PhanAnh · An · Việt · All rights reserved
          </span>
          <span>Hỗ trợ 24/7 · Xác nhận tức thì · Miễn phí hủy trước 24h</span>
        </div>
      </Footer>
    </Layout>
  )
}
