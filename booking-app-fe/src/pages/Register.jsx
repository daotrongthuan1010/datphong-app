import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, Typography, Alert, Divider, Select, message } from 'antd'
import { UserOutlined, MailOutlined, PhoneOutlined, LockOutlined } from '@ant-design/icons'
import { authApi } from '../api/auth'

const { Title, Text } = Typography

export default function Register() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  const onFinish = async (values) => {
    setError(null)
    setLoading(true)
    try {
      await authApi.register(values)
      message.success('Đăng ký thành công — hãy đăng nhập')
      navigate('/login', { replace: true })
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'grid', gridTemplateColumns: '1.05fr 0.95fr', background: '#f5f7fb' }}>
      <div style={{ background: 'linear-gradient(135deg,#1a73e8 0%,#6c5ce7 35%,#ff6b9d 100%)', display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: '48px 56px', color: '#fff', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'relative', zIndex: 1, maxWidth: 520 }}>
          <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 32, color: '#fff', textDecoration: 'none' }}>
            <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(255,255,255,0.2)', display: 'grid', placeItems: 'center', fontWeight: 800, fontSize: 20 }}>V</div>
            <span style={{ fontWeight: 800, fontSize: 24 }}>VIVU</span>
          </Link>
          <Title level={1} style={{ color: '#fff', fontSize: 38, lineHeight: 1.15, marginBottom: 16 }}>Tạo tài khoản<br />và bắt đầu đặt phòng</Title>
          <Text style={{ color: 'rgba(255,255,255,0.9)', fontSize: 15, lineHeight: 1.6 }}>Tài khoản mặc định được gán role <code style={{ background: 'rgba(255,255,255,0.2)', color: '#fff' }}>user</code> — admin tạo user kèm role ở <code style={{ background: 'rgba(255,255,255,0.2)', color: '#fff' }}>/admin/users</code>.</Text>
        </div>
        <div style={{ position: 'absolute', right: -60, bottom: -60, width: 320, height: 320, borderRadius: '50%', background: 'rgba(255,255,255,0.12)' }} />
      </div>

      <div style={{ display: 'grid', placeItems: 'center', padding: 24, background: '#fff', overflowY: 'auto' }}>
        <Card style={{ width: '100%', maxWidth: 480, boxShadow: '0 12px 40px rgba(0,0,0,0.08)', borderRadius: 16, border: 'none' }} bodyStyle={{ padding: '28px 28px 20px' }}>
          <Title level={3} style={{ marginBottom: 4, textAlign: 'center' }}>Đăng ký</Title>
          <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 16 }}>Tạo tài khoản miễn phí — đặt phòng nhanh hơn, lưu lịch sử và nhận ưu đãi</Text>
          {error && <Alert type="error" message={error} showIcon closable onClose={() => setError(null)} style={{ marginBottom: 16 }} />}

          <Form layout="vertical" requiredMark={false} onFinish={onFinish} initialValues={{ gender: true }}>
            <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, message: 'Nhập họ tên' }, { max: 150, message: 'Tối đa 150 ký tự' }]}>
              <Input size="large" prefix={<UserOutlined />} placeholder="Nguyễn Văn A" />
            </Form.Item>
            <Form.Item name="email" label="Email" rules={[{ required: true, message: 'Nhập email' }, { type: 'email', message: 'Email không hợp lệ' }]}>
              <Input size="large" prefix={<MailOutlined />} placeholder="ban@vidu.com" />
            </Form.Item>
            <Form.Item name="phone" label="Số điện thoại" rules={[{ required: true, message: 'Nhập số điện thoại' }, { pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
              <Input size="large" prefix={<PhoneOutlined />} placeholder="0912345678" maxLength={10} />
            </Form.Item>
            <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập username' }, { min: 4, message: 'Tối thiểu 4 ký tự' }, { pattern: /^[a-zA-Z0-9._]+$/, message: 'Chỉ chữ, số, . và _' }]}>
              <Input size="large" prefix={<UserOutlined />} placeholder="username" />
            </Form.Item>
            <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Nhập mật khẩu' }, { min: 6, message: 'Tối thiểu 6 ký tự' }]} hasFeedback>
              <Input.Password size="large" prefix={<LockOutlined />} placeholder="••••••••" />
            </Form.Item>
            <Form.Item name="confirmPassword" label="Nhập lại mật khẩu" dependencies={['password']} hasFeedback rules={[{ required: true, message: 'Nhập lại mật khẩu' }, ({ getFieldValue }) => ({ validator(_, v) { if (!v || getFieldValue('password') === v) return Promise.resolve(); return Promise.reject(new Error('Mật khẩu không khớp')) } })]}>
              <Input.Password size="large" prefix={<LockOutlined />} placeholder="••••••••" />
            </Form.Item>
            <Form.Item name="gender" label="Giới tính">
              <Select size="large" options={[{ value: true, label: 'Nam' }, { value: false, label: 'Nữ' }]} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" size="large" block loading={loading} style={{ height: 44, fontWeight: 600, borderRadius: 10 }}>Đăng ký</Button>
            </Form.Item>
          </Form>

          <Divider plain style={{ fontSize: 13, color: '#999' }}>đã có tài khoản?</Divider>
          <div style={{ textAlign: 'center' }}><Text>Đã có tài khoản? <Link to="/login">Đăng nhập</Link> · <Link to="/" style={{ color: '#888' }}>Về trang chủ</Link></Text></div>
        </Card>
      </div>
    </div>
  )
}
