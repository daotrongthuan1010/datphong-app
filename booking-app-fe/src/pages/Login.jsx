import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, Typography, Divider, message, Checkbox } from 'antd'
import { UserOutlined, LockOutlined, GoogleOutlined, FacebookFilled } from '@ant-design/icons'
import { login } from '../store/authSlice'

const { Title, Text } = Typography

export default function Login() {
  const [loading, setLoading] = useState(false)
  const dispatch = useDispatch()
  const navigate = useNavigate()

  const onFinish = async (values) => {
    setLoading(true)
    await new Promise((r) => setTimeout(r, 600))
    const user = { username: values.username, displayName: values.username }
    const token = `mock-token-${Date.now()}`
    dispatch(login({ user, token }))
    message.success(`Chào mừng ${values.username}!`)
    setLoading(false)
    navigate('/', { replace: true })
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'grid',
        gridTemplateColumns: '1.05fr 0.95fr',
        background: '#f5f7fb',
      }}
    >
      <div
        style={{
          background: 'linear-gradient(135deg,#1a73e8 0%,#6c5ce7 35%,#ff6b9d 100%)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          padding: '48px 56px',
          color: '#fff',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div style={{ position: 'relative', zIndex: 1, maxWidth: 520 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 32 }}>
            <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(255,255,255,0.2)', display: 'grid', placeItems: 'center', fontWeight: 800, fontSize: 20, backdropFilter: 'blur(8px)' }}>V</div>
            <span style={{ fontWeight: 800, fontSize: 24, letterSpacing: -0.5 }}>VIVU</span>
          </div>
          <Title level={1} style={{ color: '#fff', fontSize: 42, lineHeight: 1.15, marginBottom: 16 }}>
            Đặt phòng dễ dàng,
            <br />
            giá tốt mỗi ngày
          </Title>
          <Text style={{ color: 'rgba(255,255,255,0.9)', fontSize: 16, lineHeight: 1.6 }}>
            Lấy cảm hứng từ Traveloka & Agoda — tìm khách sạn, so sánh giá và đặt ngay. Giao diện tối ưu cho BE dễ đọc, dễ mở rộng booking/payment.
          </Text>
          <div style={{ display: 'flex', gap: 12, marginTop: 28, flexWrap: 'wrap' }}>
            {['Miễn phí hủy', 'Xác nhận tức thì', 'Hỗ trợ 24/7'].map((t) => (
              <span key={t} style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)', padding: '6px 12px', borderRadius: 999, fontSize: 13, backdropFilter: 'blur(8px)' }}>
                {t}
              </span>
            ))}
          </div>
        </div>
        <div
          style={{
            position: 'absolute',
            right: -60,
            bottom: -60,
            width: 320,
            height: 320,
            borderRadius: '50%',
            background: 'rgba(255,255,255,0.12)',
            filter: 'blur(2px)',
          }}
        />
        <div
          style={{
            position: 'absolute',
            left: 80,
            top: 80,
            width: 180,
            height: 180,
            borderRadius: '50%',
            background: 'rgba(255,255,255,0.08)',
          }}
        />
      </div>

      <div style={{ display: 'grid', placeItems: 'center', padding: 24, background: '#fff' }}>
        <Card
          style={{ width: '100%', maxWidth: 440, boxShadow: '0 12px 40px rgba(0,0,0,0.08)', borderRadius: 16, border: 'none' }}
          bodyStyle={{ padding: '32px 28px 24px' }}
        >
          <Title level={3} style={{ marginBottom: 4, textAlign: 'center' }}>
            Đăng nhập
          </Title>
          <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 24 }}>
            Demo — nhập bất kỳ tài khoản nào để vào trang chủ
          </Text>

          <Form name="login" onFinish={onFinish} layout="vertical" requiredMark={false} initialValues={{ username: 'demo', password: '123456', remember: true }}>
            <Form.Item name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
              <Input size="large" prefix={<UserOutlined />} placeholder="Tên đăng nhập / Email" />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
              <Input.Password size="large" prefix={<LockOutlined />} placeholder="Mật khẩu" />
            </Form.Item>
            <Form.Item>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox>Ghi nhớ</Checkbox>
                </Form.Item>
                <Button type="link" size="small" style={{ padding: 0 }}>
                  Quên mật khẩu?
                </Button>
              </div>
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" size="large" block loading={loading} style={{ height: 44, fontWeight: 600, borderRadius: 10 }}>
                Đăng nhập
              </Button>
            </Form.Item>
          </Form>

          <Divider plain style={{ fontSize: 13, color: '#999' }}>
            hoặc tiếp tục với
          </Divider>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Button icon={<GoogleOutlined />} size="large" style={{ borderRadius: 10 }}>
              Google
            </Button>
            <Button icon={<FacebookFilled />} size="large" style={{ borderRadius: 10 }}>
              Facebook
            </Button>
          </div>

          <div style={{ textAlign: 'center', marginTop: 20 }}>
            <Text type="secondary">
              Chưa có tài khoản? <Link to="/">Đăng ký</Link> ·{' '}
              <Link to="/" style={{ color: '#888' }}>
                Bỏ qua, xem phòng
              </Link>
            </Text>
          </div>
        </Card>
        <Text type="secondary" style={{ marginTop: 16, fontSize: 12 }}>
          Lấy cảm hứng UI từ{' '}
          <a href="https://www.traveloka.com" target="_blank" rel="noreferrer">
            Traveloka
          </a>{' '}
          & Agoda · BE: <code>localhost:8080</code> có thể đổi qua <code>.env</code>
        </Text>
      </div>
    </div>
  )
}
