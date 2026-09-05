import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, Typography, Alert, Steps, message } from 'antd'
import { MailOutlined, SafetyCertificateOutlined, LockOutlined, ArrowLeftOutlined } from '@ant-design/icons'
import { authApi } from '../api/auth'

const { Title, Text } = Typography

export default function ForgotPassword() {
  const [step, setStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [email, setEmail] = useState('')
  const navigate = useNavigate()

  const sendOtp = async (values) => {
    setError(null)
    setLoading(true)
    try {
      await authApi.forgotPassword({ email: values.email })
      setEmail(values.email)
      message.success('Đã gửi mã OTP tới email')
      setStep(1)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const reset = async (values) => {
    setError(null)
    setLoading(true)
    try {
      await authApi.resetPassword({ email, otpCode: values.otpCode, newPassword: values.newPassword, confirmPassword: values.confirmPassword })
      message.success('Đổi mật khẩu thành công — hãy đăng nhập lại')
      navigate('/login', { replace: true })
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: '#f5f7fb', padding: 24 }}>
      <Card style={{ width: '100%', maxWidth: 480, borderRadius: 16, boxShadow: '0 12px 40px rgba(0,0,0,0.08)' }} bodyStyle={{ padding: 28 }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => (step === 1 ? setStep(0) : navigate('/login'))} style={{ marginBottom: 8, paddingLeft: 0 }}>Quay lại</Button>
        <Title level={3} style={{ marginBottom: 4 }}>Quên mật khẩu</Title>
        <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
          {step === 0 ? 'Nhập email đã đăng ký, chúng tôi sẽ gửi mã xác thực để bạn đặt lại mật khẩu' : `Mã OTP đã gửi tới ${email} — nhập mã cùng mật khẩu mới để hoàn tất`}
        </Text>

        <Steps size="small" current={step} items={[{ title: 'Gửi OTP' }, { title: 'Đặt lại mật khẩu' }]} style={{ marginBottom: 20 }} />

        {error && <Alert type="error" message={error} showIcon closable onClose={() => setError(null)} style={{ marginBottom: 16 }} />}

        {step === 0 ? (
          <Form layout="vertical" requiredMark={false} onFinish={sendOtp}>
            <Form.Item name="email" label="Email" rules={[{ required: true, message: 'Nhập email' }, { type: 'email', message: 'Email không hợp lệ' }]}>
              <Input size="large" prefix={<MailOutlined />} placeholder="ban@vidu.com" />
            </Form.Item>
            <Form.Item><Button type="primary" htmlType="submit" size="large" block loading={loading} style={{ borderRadius: 10, fontWeight: 600 }}>Gửi mã OTP</Button></Form.Item>
            <div style={{ textAlign: 'center' }}><Text type="secondary">Nhớ mật khẩu? <Link to="/login">Đăng nhập</Link></Text></div>
          </Form>
        ) : (
          <Form layout="vertical" requiredMark={false} onFinish={reset}>
            <Form.Item name="otpCode" label="Mã OTP" rules={[{ required: true, message: 'Nhập mã OTP' }]}>
              <Input size="large" prefix={<SafetyCertificateOutlined />} placeholder="Mã 6 số trong email" maxLength={10} />
            </Form.Item>
            <Form.Item name="newPassword" label="Mật khẩu mới" rules={[{ required: true, message: 'Nhập mật khẩu mới' }, { min: 6, message: 'Tối thiểu 6 ký tự' }]} hasFeedback>
              <Input.Password size="large" prefix={<LockOutlined />} placeholder="••••••••" />
            </Form.Item>
            <Form.Item name="confirmPassword" label="Nhập lại mật khẩu mới" dependencies={['newPassword']} hasFeedback rules={[{ required: true, message: 'Nhập lại mật khẩu' }, ({ getFieldValue }) => ({ validator(_, v) { if (!v || getFieldValue('newPassword') === v) return Promise.resolve(); return Promise.reject(new Error('Mật khẩu không khớp')) } })]}>
              <Input.Password size="large" prefix={<LockOutlined />} placeholder="••••••••" />
            </Form.Item>
            <Form.Item><Button type="primary" htmlType="submit" size="large" block loading={loading} style={{ borderRadius: 10, fontWeight: 600 }}>Đặt lại mật khẩu</Button></Form.Item>
            <Button block onClick={() => setStep(0)}>Gửi lại OTP</Button>
          </Form>
        )}
      </Card>
    </div>
  )
}
