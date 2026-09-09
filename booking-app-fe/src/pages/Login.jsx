import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, Typography, Divider, Checkbox, Alert, message } from 'antd'
import { UserOutlined, LockOutlined, GoogleOutlined, FacebookFilled, SafetyCertificateOutlined, QrcodeOutlined } from '@ant-design/icons'
import { loginChallengeThunk, completeLoginThunk, clearError } from '../store/authSlice'

const { Title, Text } = Typography
const { OTP } = Input

export default function Login() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const pending = useSelector((s) => s.auth.pendingChallenge)
  const { loading, error } = useSelector((s) => s.auth)
  const [phase, setPhase] = useState('password') // 'password' | 'otp'
  const [passwordValues, setPasswordValues] = useState(null)

  const onPasswordFinish = async (values) => {
    const res = await dispatch(loginChallengeThunk({ username: values.username, password: values.password }))
    if (loginChallengeThunk.fulfilled.match(res)) {
      setPasswordValues({ username: values.username, password: values.password })
      setPhase('otp')
    }
  }

  const onOtpFinish = async (values) => {
    const code = (values.code || '').trim()
    const loginToken = pending?.loginToken
    if (!loginToken) {
      message.error('Phiên xác thực đã hết hạn, vui lòng đăng nhập lại')
      setPhase('password')
      return
    }
    const res = await dispatch(completeLoginThunk({ loginToken, code }))
    if (completeLoginThunk.fulfilled.match(res)) {
      message.success(`Chào mừng ${res.payload.user?.username || res.payload.user?.fullName || ''}!`)
      navigate('/', { replace: true })
    }
  }

  const onBack = () => {
    setPhase('password')
  }

  const isSetupRequired = !!pending?.setupRequired
  const otpSecondsLeft = Number.isFinite(pending?.expiresIn) ? pending.expiresIn : null

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
            Hành trình bắt đầu từ một giấc ngủ ngon. Hàng nghìn lựa chọn, giá tốt mỗi ngày — đăng nhập để mở khóa trải nghiệm đầy đủ.
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
            {phase === 'otp' ? 'Xác thực OTP' : 'Đăng nhập'}
          </Title>
          <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 16 }}>
            {phase === 'otp'
              ? (isSetupRequired ? 'Lần đầu — quét QR bằng Google/Microsoft Authenticator rồi nhập mã 6 số.' : 'Nhập mã 6 số trong Google/Microsoft Authenticator.')
              : 'Nhập tài khoản — hệ thống bắt buộc xác thực OTP trước khi đăng nhập.'}
          </Text>

          {error && <Alert type="error" message={error} showIcon closable onClose={() => dispatch(clearError())} style={{ marginBottom: 16 }} />}

          {phase === 'password' ? (
            <Form name="login-password" onFinish={onPasswordFinish} layout="vertical" requiredMark={false} initialValues={{ username: 'demo', password: '123456', remember: true }}>
              <Form.Item name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
                <Input size="large" prefix={<UserOutlined />} placeholder="Tên đăng nhập" />
              </Form.Item>
              <Form.Item name="password" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
                <Input.Password size="large" prefix={<LockOutlined />} placeholder="Mật khẩu" />
              </Form.Item>

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
                  Tiếp tục
                </Button>
              </Form.Item>
            </Form>
          ) : (
            <>
              <Alert
                type={isSetupRequired ? 'warning' : 'info'}
                showIcon
                icon={isSetupRequired ? <QrcodeOutlined /> : <SafetyCertificateOutlined />}
                message={isSetupRequired ? 'Tài khoản chưa đăng ký app — quét QR trên bằng Google/Microsoft Authenticator trước.' : `Nhập mã 6 số của tài khoản ${passwordValues?.username || pending?.username || ''}.`}
                description={otpSecondsLeft != null ? `Phiên OTP còn ${Math.floor(otpSecondsLeft / 60)}:${String(otpSecondsLeft % 60).padStart(2, '0')} — nếu hết hạn hãy đăng nhập lại từ bước mật khẩu.` : undefined}
                style={{ marginBottom: 16 }}
              />
              {isSetupRequired && pending?.qrCodeDataUri && (
                <div style={{ display: 'grid', placeItems: 'center', gap: 10, marginBottom: 16, border: '1px dashed #d6e4ff', borderRadius: 12, padding: 16, background: '#fafcff' }}>
                  <img src={pending.qrCodeDataUri} alt="QR kích hoạt TOTP" style={{ width: 220, height: 220, borderRadius: 12, background: '#fff', border: '1px solid #e6efff', objectFit: 'contain' }} />
                  <Text type="secondary" style={{ fontSize: 12, textAlign: 'center' }}>Mở Google Authenticator / Microsoft Authenticator → Quét mã QR này.</Text>
                  {pending?.secret && (
                    <Text copyable={{ text: pending.secret }} type="secondary" style={{ fontSize: 11, wordBreak: 'break-all' }}>
                      Không quét được? Nhập tay: <Text strong>{pending.secret}</Text>
                    </Text>
                  )}
                  {pending?.otpAuthUri && (
                    <Text copyable={{ text: pending.otpAuthUri }} type="secondary" style={{ fontSize: 10, wordBreak: 'break-all', maxWidth: 360 }}>
                      otpauth: {pending.otpAuthUri.slice(0, 80)}…
                    </Text>
                  )}
                </div>
              )}
              <Form name="login-otp" onFinish={onOtpFinish} layout="vertical" requiredMark={false}>
                <Form.Item
                  name="code"
                  label="Mã OTP 6 số"
                  rules={[{ required: true, message: 'Nhập mã 6 số' }, { pattern: /^[0-9]{6}$/, message: 'Mã phải là 6 chữ số' }]}
                  extra="Mã thay đổi mỗi 30 giây trong app."
                >
                  <OTP length={6} autoFocus />
                </Form.Item>
                <Form.Item>
                  <Button type="primary" htmlType="submit" size="large" block loading={loading} style={{ height: 44, fontWeight: 600, borderRadius: 10 }}>
                    Xác nhận OTP & Đăng nhập
                  </Button>
                  <Button block style={{ marginTop: 8, borderRadius: 10 }} onClick={onBack} disabled={loading}>
                    Quay lại
                  </Button>
                </Form.Item>
              </Form>
            </>
          )}

          {phase === 'password' && (
            <>
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
            </>
          )}

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
