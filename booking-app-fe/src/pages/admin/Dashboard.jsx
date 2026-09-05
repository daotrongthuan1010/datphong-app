import { useEffect, useState } from 'react'
import { Row, Col, Card, Typography, Space, Tag, Statistic } from 'antd'
import { ApartmentOutlined, TeamOutlined, GiftOutlined, CrownOutlined, HeartOutlined, ThunderboltOutlined } from '@ant-design/icons'
import StatCard from '../../components/common/StatCard'
import { roomApi } from '../../api/rooms'
import { userApi } from '../../api/users'
import { voucherApi, hostApi } from '../../api/vouchers'

const { Title, Text } = Typography

export default function Dashboard() {
  const [stats, setStats] = useState({ rooms: 0, users: 0, vouchers: 0, hosts: 0 })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    setLoading(true)
    Promise.allSettled([
      roomApi.list({ page: 0, size: 1 }),
      userApi.list({ page: 0, size: 1 }),
      voucherApi.list({ page: 0, size: 1 }),
      hostApi.list({ page: 0, size: 1 }),
    ]).then(([r, u, v, h]) => {
      if (!mounted) return
      setStats({
        rooms: r.value?.totalElements ?? 0,
        users: u.value?.totalElements ?? 0,
        vouchers: v.value?.totalElements ?? 0,
        hosts: h.value?.totalElements ?? 0,
      })
      setLoading(false)
    })
    return () => {
      mounted = false
    }
  }, [])

  return (
    <div>
      <Card style={{ borderRadius: 16, marginBottom: 16, border: 'none' }} bodyStyle={{ padding: 20 }}>
        <Space direction="vertical" size={4}>
          <Title level={3} style={{ margin: 0 }}>
            Bảng điều khiển
          </Title>
          <Text type="secondary">Tổng quan hệ thống VIVU Booking</Text>
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Tổng số phòng" value={stats.rooms} icon={<ApartmentOutlined />} color="#1968f5" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Người dùng" value={stats.users} icon={<TeamOutlined />} color="#52c41a" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Voucher" value={stats.vouchers} icon={<GiftOutlined />} color="#fa8c16" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Chủ nhà" value={stats.hosts} icon={<CrownOutlined />} color="#722ed1" loading={loading} />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="Định hướng phát triển" style={{ borderRadius: 16 }} size="small">
            <Space direction="vertical" size={8}>
              {[
                ['Đặt phòng', 'Quản lý lịch đặt, giữ chỗ tự động và hoàn tiền linh hoạt'],
                ['Thanh toán', 'Tích hợp ví điện tử và cổng thanh toán nội địa'],
                ['Tốc độ', 'Bộ nhớ đệm giúp tải danh sách phòng nhanh hơn'],
                ['Hình ảnh', 'Kho ảnh phòng riêng cho từng chủ nhà'],
              ].map(([t, d]) => (
                <Space key={t} align="start">
                  <ThunderboltOutlined style={{ color: '#fa8c16', marginTop: 3 }} />
                  <div>
                    <Text strong>{t}</Text>
                    <br />
                    <Text type="secondary" style={{ fontSize: 13 }}>
                      {d}
                    </Text>
                  </div>
                </Space>
              ))}
            </Space>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Trạng thái hệ thống" style={{ borderRadius: 16 }} size="small">
            <Space direction="vertical" size={10} style={{ width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text>Máy chủ ứng dụng</Text>
                <Tag color="green">Đang chạy</Tag>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text>Cơ sở dữ liệu</Text>
                <Tag color="green">Kết nối tốt</Tag>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text>Bộ nhớ đệm & phiên</Text>
                <Tag color="green">Sẵn sàng</Tag>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text>Kho lưu trữ tệp</Text>
                <Tag color="green">Sẵn sàng</Tag>
              </div>
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
