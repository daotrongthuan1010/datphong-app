import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, Typography, Divider, Checkbox, Alert, message } from 'antd'
import { UserOutlined, LockOutlined, GoogleOutlined, FacebookFilled, SafetyCertificateOutlined } from '@ant-design/icons'
import { loginThunk, clearError } from '../store/authSlice'

const { Title, Text } = Typography

export default function Login() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { loading, error } = useSelector((s) => s.auth)
  const [show2fa, setShow2fa] = useState(false)
  const [pendingValues, setPendingValues] = useState(null)

  const onFinish = async (values) => {
    const payload = show2fa
      ? { username: pendingValues.username, password: pendingValues.password, totpCode: values.totpCode }
      : { username: values.username, password: values.password, totpCode: values.totpCode || undefined }
    const res = await dispatch(loginThunk(payload))
    if (loginThunk.fulfilled.match(res)) {
      message.success(`Chào mừng ${res.payload.user?.username || res.payload.user?.fullName || ''}!`)
      navigate('/', { replace: true })
    } else {
      const msg = res.payload || 'Đăng nhập thất bại'
      if (typeof msg === 'string' && msg.toLowerCase().includes('2fa')) {
        setPendingValues(values)
        setShow2fa(true)
        message.info('Tài khoản đã bật 2FA — nhập mã 6 số từ Google/Microsoft Authenticator')
      }
    }
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
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: 12,
                background: 'rgba(255,255,255,0.2)',
                display: 'grid',
                placeItems: 'center',
                fontWeight: 800,
                fontSize: 20,
                backdropFilter: 'blur(8px)',
              }}
            >
              V
            </div>
            <span style={{ fontWeight: 800, fontSize: 24, letterSpacing: -0.5 }}>VIVU</span>
          </div>
          <Title level={1} style={{ color: '#fff', fontSize: 42, lineHeight: 1.15, marginBottom: 16 }}>
            Đặt phòng thảnh thơi,
            <br />
            vi vu khắp nơi
          </Title>
          <Text style={{ color: 'rgba(255,255,255,0.9)', fontSize: 16, lineHeight: 1.6 }}>
            Hành trình bắt đầu từ một giấc ngủ ngon. Hàng nghìn lựa chọn, giá tốt mỗi ngày — đăng nhập để mở khóa trải nghiệm đầy đủ của bạn, từ lịch sử đặt phòng đến ưu đãi riêng.
          </Text>
          <div style={{ display: 'flex', gap: 12, marginTop: 28, flexWrap: 'wrap' }}>
            {['Miễn phí hủy', 'Xác nhận tức thì', 'Hỗ trợ 24/7'].map((t) => (
              <span key={t} style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)', padding: '6px 12px', borderRadius: 999, fontSize: 13, backdropFilter: 'blur(8px)' }}>
                {t}
              </span>
            ))}
          </div>
        </div>
        <div style={{ position: 'absolute', right: -60, bottom: -60, width: 320, height: 320, borderRadius: '50%', background: 'rgba(255,255,255,0.12)' }} />
        <div style={{ position: 'absolute', left: 80, top: 80, width: 180, height: 180, borderRadius: '50%', background: 'rgba(255,255,255,0.08)' }} />
      </div>

      <div style={{ display: 'grid', placeItems: 'center', padding: 24, background: '#fff' }}>
        <Card style={{ width: '100%', maxWidth: 440, boxShadow: '0 12px 40px rgba(0,0,0,0.08)', borderRadius: 16, border: 'none' }} bodyStyle={{ padding: '32px 28px 24px' }}>
          <Title level={3} style={{ marginBottom: 4, textAlign: 'center' }}>
            Đăng nhập
          </Title>
          <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 16 }}>
            Đăng nhập để đặt phòng và quản lý chuyến đi của bạn
          </Text>

          {error && <Alert type="error" message={error} showIcon closable onClose={() => dispatch(clearError())} style={{ marginBottom: 16 }} />}
          {show2fa && (
            <Alert type="info" showIcon icon={<SafetyCertificateOutlined />} message="Nhập mã 2FA (6 số) từ Google/Microsoft Authenticator" style={{ marginBottom: 16 }} />
          )}

          <Form name="login" onFinish={onFinish} layout="vertical" requiredMark={false} initialValues={{ username: 'demo', password: '123456', remember: true }}>
            {!show2fa ? (
              <>
                <Form.Item name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
                  <Input size="large" prefix={<UserOutlined />} placeholder="Tên đăng nhập / Email" />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
                  <Input.Password size="large" prefix={<LockOutlined />} placeholder="Mật khẩu" />
                </Form.Item>
                <Form.Item name="totpCode" label="Mã 2FA (nếu tài khoản đã bật)" extra="Để trống nếu chưa bật 2FA">
                  <Input size="large" prefix={<SafetyCertificateOutlined />} placeholder="123456" maxLength={6} />
                </Form.Item>
              </>
            ) : (
              <Form.Item name="totpCode" rules={[{ required: true, message: 'Nhập mã 6 số' }, { pattern: /^[0-9]{6}$/, message: 'Mã phải là 6 chữ số' }]}>
                <Input size="large" prefix={<SafetyCertificateOutlined />} placeholder="Mã 6 số từ Authenticator" maxLength={6} autoFocus />
              </Form.Item>
            )}

            <Form.Item>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox>Ghi nhớ</Checkbox>
                </Form.Item>
                <Link to="/forgot-password">Quên mật khẩu?</Link>
              </div>
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" size="large" block loading={loading} style={{ height: 44, fontWeight: 600, borderRadius: 10 }}>
                {show2fa ? 'Xác nhận 2FA & Đăng nhập' : 'Đăng nhập'}
              </Button>
              {show2fa && (
                <Button block style={{ marginTop: 8, borderRadius: 10 }} onClick={() => setShow2fa(false)}>
                  Quay lại
                </Button>
              )}
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
              Chưa có tài khoản? <Link to="/register">Đăng ký</Link> · <Link to="/" style={{ color: '#888' }}>Xem phòng không cần đăng nhập</Link>
            </Text>
          </div>
        </Card>
        <Text type="secondary" style={{ textAlign: 'center', marginTop: 16, fontSize: 11, lineHeight: 1.5 }}>
          VIVU — Đặt phòng thảnh thơi, vi vu khắp nơi
          <br />© 2026 PhanAnh · An · Việt · All rights reserved
        </Text>
      </div>
    </div>
  )
}
