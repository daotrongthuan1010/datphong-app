import { useEffect, useState } from 'react'
import { Row, Col, Card, Typography, Space, Button, Tag, Skeleton } from 'antd'
import { Link } from 'react-router-dom'
import { ApartmentOutlined, TeamOutlined, GiftOutlined, CrownOutlined, FundOutlined, InfoCircleOutlined } from '@ant-design/icons'
import StatCard from '../../components/common/StatCard'
import { roomApi } from '../../api/rooms'
import { userApi } from '../../api/users'
import { voucherApi, hostApi } from '../../api/vouchers'
import { reportApi } from '../../api/reports'

const { Title, Text } = Typography

export default function Dashboard() {
  const [stats, setStats] = useState({ rooms: 0, users: 0, vouchers: 0, hosts: 0 })
  const [loading, setLoading] = useState(true)
  const [revenue, setRevenue] = useState(null)

  useEffect(() => {
    let mounted = true
    setLoading(true)
    Promise.allSettled([
      roomApi.list({ page: 0, size: 1 }),
      userApi.list({ page: 0, size: 1 }),
      voucherApi.list({ page: 0, size: 1 }),
      hostApi.list({ page: 0, size: 1 }),
      reportApi.overview({ period: '30d', granularity: 'DAY' }).catch(() => null),
    ]).then(([r, u, v, h, rev]) => {
      if (!mounted) return
      setStats({
        rooms: r.value?.totalElements ?? 0,
        users: u.value?.totalElements ?? 0,
        vouchers: v.value?.totalElements ?? 0,
        hosts: h.value?.totalElements ?? 0,
      })
      if (rev.status === 'fulfilled' && rev.value) setRevenue(rev.value)
      setLoading(false)
    })
    return () => { mounted = false }
  }, [])

  return (
    <div style={{ padding: 16 }}>
      <Card style={{ borderRadius: 16, marginBottom: 16, border: 'none' }} bodyStyle={{ padding: 20 }}>
        <Space direction="vertical" size={4}>
          <Title level={3} style={{ margin: 0 }}>Bang dieu khien</Title>
          <Text type="secondary">Tong quan he thong VIVU Booking</Text>
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Tong so phong" value={stats.rooms} icon={<ApartmentOutlined />} color="#1968f5" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Nguoi dung" value={stats.users} icon={<TeamOutlined />} color="#52c41a" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Voucher" value={stats.vouchers} icon={<GiftOutlined />} color="#fa8c16" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Chu nha" value={stats.hosts} icon={<CrownOutlined />} color="#722ed1" loading={loading} />
        </Col>
      </Row>

      <Card
        size="small"
        style={{ borderRadius: 16, marginTop: 16 }}
        title={<span><FundOutlined /> Doanh thu 30 ngay</span>}
        extra={<Link to="/admin/revenue"><Button type="link" size="small">Xem chi tiet</Button></Link>}
      >
        {loading && !revenue ? (
          <Skeleton active paragraph={{ rows: 2 }} />
        ) : !revenue ? (
          <Text type="secondary">Chưa có dữ liệu doanh thu trong kỳ này.</Text>
        ) : (
          <Row gutter={[16, 12]}>
            <Col xs={12} sm={6}>
              <Text type="secondary" style={{ fontSize: 12 }}>Doanh thu gop</Text>
              <div style={{ fontSize: 20, fontWeight: 700 }}>
                {(Number(revenue.grossRevenue) || 0).toLocaleString('vi-VN')} <Text type="secondary" style={{ fontSize: 13 }}>d</Text>
              </div>
              <Text type="secondary" style={{ fontSize: 12 }}>30 ngày gần nhất</Text>
            </Col>
            <Col xs={12} sm={6}>
              <Text type="secondary" style={{ fontSize: 12 }}>Hoa hong (10%)</Text>
              <div style={{ fontSize: 20, fontWeight: 700 }}>{(Number(revenue.commission) || 0).toLocaleString('vi-VN')} d</div>
              <Text type="secondary" style={{ fontSize: 12 }}>nền tảng</Text>
            </Col>
            <Col xs={12} sm={6}>
              <Text type="secondary" style={{ fontSize: 12 }}>Don da thanh toan</Text>
              <div style={{ fontSize: 20, fontWeight: 700 }}>{Number(revenue.bookings) || 0}</div>
              <Text type="secondary" style={{ fontSize: 12 }}>đã hoàn tất</Text>
            </Col>
            <Col xs={12} sm={6}>
              <Text type="secondary" style={{ fontSize: 12 }}>Ty le huy</Text>
              <div style={{ fontSize: 20, fontWeight: 700 }}>
                {revenue.cancellationRate == null ? '-' : (Number(revenue.cancellationRate) * 100).toFixed(1) + '%'}
              </div>
              <Text type="secondary" style={{ fontSize: 12 }}>huy / tong don tao trong ky</Text>
            </Col>
            <Col span={24}>
              <Space size={8} wrap style={{ marginTop: 8 }}>
                <Tag color="blue">Net {Number(revenue.netRevenue || 0).toLocaleString('vi-VN')} d</Tag>
                <Tag>Hoan {Number(revenue.refundAmount || 0).toLocaleString('vi-VN')} d</Tag>
                {revenue.matViewLastRefresh && (
                  <Tag icon={<InfoCircleOutlined />} style={{ borderRadius: 999 }}>
                    cập nhật {revenue.matViewLastRefresh}
                  </Tag>
                )}
              </Space>
            </Col>
          </Row>
        )}
      </Card>
    </div>
  )
}
