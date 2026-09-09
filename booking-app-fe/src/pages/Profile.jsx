import { useEffect, useState } from 'react'
import { Card, Form, Input, Button, Upload, Select, message, Skeleton, Typography, Divider, Space, Tag } from 'antd'
import { UploadOutlined, UserOutlined, SaveOutlined } from '@ant-design/icons'
import PageHeader from '../components/common/PageHeader'
import { profileApi } from '../api/profile'
import { loyaltyApi } from '../api/loyalty'
import { useDispatch } from 'react-redux'
import { setAuth } from '../store/authSlice'

const { Text } = Typography

function RankBadge({ profile }) {
  if (!profile) return null
  const cur = profile.currentRank
  const next = profile.nextRank
  return (
    <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center', marginBottom: 16 }}>
      {cur ? (
        <Tag color="gold" style={{ borderRadius: 999, padding: '4px 12px', fontWeight: 700 }}>
          {cur.name} · {cur.discountPercent ?? 0}% off · mốc {cur.minPoints} điểm
        </Tag>
      ) : (
        <Tag>Chưa có hạng</Tag>
      )}
      <Text type="secondary" style={{ fontSize: 13 }}>
        Tổng điểm: <Text strong>{profile.totalPoints}</Text>
        {next && ` · còn ${profile.pointsToNextRank} điểm để lên ${next.name} (${next.minPoints})`}
        {!next && cur && ' · đã đạt hạng cao nhất'}
      </Text>
    </div>
  )
}

export default function Profile() {
  const dispatch = useDispatch()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [me, setMe] = useState(null)
  const [loyalty, setLoyalty] = useState(null)
  const [fileList, setFileList] = useState([])
  const [form] = Form.useForm()

  const load = () => {
    setLoading(true)
    Promise.allSettled([profileApi.getMe(), loyaltyApi.myProfile()]).then(([a, b]) => {
      if (a.status === 'fulfilled') {
        setMe(a.value)
        form.setFieldsValue({
          fullName: a.value.fullName,
          email: a.value.email,
          phone: a.value.phone,
          gender: a.value.gender === true ? 'male' : a.value.gender === false ? 'female' : undefined,
        })
      } else {
        message.error(a.reason?.message || 'Không tải được hồ sơ')
      }
      if (b.status === 'fulfilled') setLoyalty(b.value)
    }).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const onFinish = async (values) => {
    const payload = {
      fullName: values.fullName?.trim() || undefined,
      email: values.email?.trim() || undefined,
      phone: values.phone?.trim() || undefined,
      gender: values.gender === 'male' ? true : values.gender === 'female' ? false : undefined,
    }
    const file = fileList[0]?.originFileObj || null
    // Không gửi field undefined để BE giữ nguyên
    Object.keys(payload).forEach((k) => payload[k] === undefined && delete payload[k])
    setSaving(true)
    try {
      const updated = await profileApi.updateMe(payload, file)
      setMe(updated)
      message.success('Đã cập nhật hồ sơ')
      setFileList([])
      // Đồng bộ username/fullName trong auth slice khi đổi email/fullName
      const authRaw = localStorage.getItem('vivu_auth')
      if (authRaw) {
        try {
          const auth = JSON.parse(authRaw)
          auth.user = { ...(auth.user || {}), fullName: updated.fullName, username: updated.username, avatar: updated.avatar }
          localStorage.setItem('vivu_auth', JSON.stringify(auth))
          dispatch(setAuth({ user: auth.user, token: auth.token, refreshToken: auth.refreshToken }))
        } catch {}
      }
    } catch (e) {
      message.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div style={{ maxWidth: 900, margin: '0 auto', padding: 24 }}><Card><Skeleton active /></Card></div>

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '16px 24px 32px' }}>
      <PageHeader title={<><UserOutlined /> Hồ sơ của tôi</>} description="Xem và cập nhật thông tin cá nhân (email, điện thoại, ảnh đại diện)." />

      <RankBadge profile={loyalty} />

      <Card style={{ borderRadius: 16 }}>
        <Form form={form} layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item name="fullName" label="Họ và tên" rules={[{ max: 150, message: 'Tối đa 150 ký tự' }]}>
            <Input placeholder="Nguyễn Văn A" />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}>
            <Input placeholder="you@example.com" />
          </Form.Item>
          <Form.Item name="phone" label="Số điện thoại" rules={[{ pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
            <Input placeholder="0901234567" />
          </Form.Item>
          <Form.Item name="gender" label="Giới tính">
            <Select allowClear placeholder="Chọn" options={[{ value: 'male', label: 'Nam' }, { value: 'female', label: 'Nữ' }]} />
          </Form.Item>

          {me?.avatar && (
            <div style={{ marginBottom: 12 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>Ảnh hiện tại:</Text>
              <br />
              <img src={me.avatar} alt="avatar" style={{ width: 96, height: 96, borderRadius: 999, objectFit: 'cover', marginTop: 8, border: '1px solid #eee' }} />
            </div>
          )}

          <Form.Item label="Ảnh đại diện mới (tùy chọn)">
            <Upload
              listType="picture"
              maxCount={1}
              accept="image/*"
              beforeUpload={() => false}
              fileList={fileList}
              onChange={({ fileList: fl }) => setFileList(fl.slice(-1))}
            >
              <Button icon={<UploadOutlined />}>Chọn ảnh</Button>
            </Upload>
          </Form.Item>

          <Divider />
          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>Lưu thay đổi</Button>
            <Button onClick={load}>Tải lại</Button>
          </Space>
        </Form>
      </Card>

      {me && (
        <Card size="small" style={{ borderRadius: 12, marginTop: 16, background: '#fafcff' }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Username: <Text strong>{me.username}</Text> · Trạng thái: {me.status || '-'} · Đổi mật khẩu qua <a href="/forgot-password">Quên mật khẩu</a>.
          </Text>
        </Card>
      )}
    </div>
  )
}
