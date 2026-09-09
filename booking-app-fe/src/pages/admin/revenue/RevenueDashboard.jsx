import { Card, Row, Col, Typography, Space, Button, Tag, Skeleton, Alert, Segmented } from 'antd'
import { ReloadOutlined, DashboardOutlined, SyncOutlined } from '@ant-design/icons'
import { useEffect, useState, useCallback } from 'react'
import PageHeader from '../../../components/common/PageHeader'
import StatCard from '../../../components/common/StatCard'
import DataTable from '../../../components/common/DataTable'
import { BarChart, LineChart, DonutChart } from '../../../components/common/Chart'
import { reportApi } from '../../../api/reports'

const { Text } = Typography

const PERIODS = [
  { label: '7 ngày', value: '7d' },
  { label: '30 ngày', value: '30d' },
  { label: '90 ngày', value: '90d' },
  { label: '1 năm', value: '1y' },
]
const GRANULARITIES = [
  { label: 'Ngày', value: 'DAY' },
  { label: 'Tuần', value: 'WEEK' },
  { label: 'Tháng', value: 'MONTH' },
]

function vnd(v) {
  return (Number(v) || 0).toLocaleString('vi-VN') + ' ₫'
}

export default function RevenueDashboard() {
  const [period, setPeriod] = useState('30d')
  const [granularity, setGranularity] = useState('DAY')
  const [overview, setOverview] = useState(null)
  const [topRooms, setTopRooms] = useState([])
  const [occupancy, setOccupancy] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    Promise.allSettled([
      reportApi.overview({ period, granularity }),
      reportApi.topRooms({ period, limit: 10 }),
      reportApi.occupancy({ period }),
    ]).then(([o, t, oc]) => {
      if (o.status === 'fulfilled') setOverview(o.value)
      else setError(o.reason)
      setTopRooms(t.status === 'fulfilled' ? t.value || [] : [])
      setOccupancy(oc.status === 'fulfilled' ? oc.value || [] : [])
      setLoading(false)
    })
  }, [period, granularity])

  useEffect(() => { load() }, [load])

  const doRefresh = async () => {
    setRefreshing(true)
    try {
      await reportApi.refresh()
      load()
    } catch (e) {
      setError(e)
    } finally {
      setRefreshing(false)
    }
  }

  const points = overview?.points || []

  const byRoomType = points.reduce((acc, p) => {
    if (!p.roomType) return acc
    const row = acc.find((r) => r.roomType === p.roomType)
    if (row) row.revenue = Number(row.revenue || 0) + Number(p.revenue || 0)
    else acc.push({ roomType: p.roomType, revenue: Number(p.revenue || 0) })
    return acc
  }, [])

  const occupancySeries = occupancy.map((o) => ({
    period: o.period,
    rate: Math.round((Number(o.occupancyRate) || 0) * 1000) / 10,
  }))

  return (
    <div style={{ padding: 16 }}>
      <PageHeader
        title={<span><DashboardOutlined /> Doanh thu</span>}
        description={
          overview?.matViewLastRefresh
            ? `Cập nhật lúc ${overview.matViewLastRefresh}`
            : 'Tổng hợp từ các giao dịch đã thanh toán thành công'
        }
        extra={
          <Space wrap>
            <Segmented options={PERIODS} value={period} onChange={setPeriod} />
            <Segmented options={GRANULARITIES} value={granularity} onChange={setGranularity} />
            <Button icon={<SyncOutlined />} loading={refreshing} onClick={doRefresh}>Làm mới</Button>
            <Button icon={<ReloadOutlined />} loading={loading} onClick={load}>Tải lại</Button>
          </Space>
        }
      />

      {error && (
        <Alert
          type={error.status === 503 ? 'warning' : 'error'}
          showIcon
          style={{ marginBottom: 16, borderRadius: 12 }}
          message={error.status === 503 ? 'Báo cáo chưa sẵn sàng' : 'Không lấy được báo cáo'}
          description={error.message}
        />
      )}

      {loading && !overview ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <StatCard title="Doanh thu gộp" value={Number(overview?.grossRevenue || 0)} precision={0}
                        suffix="₫" icon={<DashboardOutlined />} color="#1968f5" />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatCard title="Hoa hồng nền tảng" value={Number(overview?.commission || 0)} precision={0}
                        suffix="₫" icon={<DashboardOutlined />} color="#722ed1" />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatCard title="Đơn đã thanh toán" value={Number(overview?.bookings || 0)}
                        icon={<DashboardOutlined />} color="#52c41a" />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatCard title="Tỷ lệ hủy" value={Math.round(Number(overview?.cancellationRate || 0) * 1000) / 10}
                        suffix="%" precision={1} icon={<DashboardOutlined />} color="#fa541c" />
            </Col>
          </Row>

          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} sm={12} lg={6}>
              <StatCard title="Doanh thu ròng" value={Number(overview?.netRevenue || 0)} precision={0}
                        suffix="₫" icon={<DashboardOutlined />} color="#13c2c2" />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatCard title="Đã hoàn tiền" value={Number(overview?.refundAmount || 0)} precision={0}
                        suffix="₫" icon={<DashboardOutlined />} color="#fa8c16" />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatCard title="Đêm đã bán" value={Number(overview?.nightsSold || 0)}
                        icon={<DashboardOutlined />} color="#eb2f96" />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatCard title="Giá trị đơn trung bình" value={Number(overview?.avgOrderValue || 0)} precision={0}
                        suffix="₫" icon={<DashboardOutlined />} color="#1968f5" />
            </Col>
          </Row>

          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={16}>
              <Card size="small" style={{ borderRadius: 16 }}
                    title={`Doanh thu theo ${granularity === 'MONTH' ? 'tháng' : granularity === 'WEEK' ? 'tuần' : 'ngày'}`}
                    extra={<Tag color="green">chỉ tính giao dịch thành công</Tag>}>
                {points.length === 0 ? (
                  <Text type="secondary">Chưa có giao dịch thành công trong kỳ này</Text>
                ) : (
                  <BarChart data={points} xKey="period" yKey="revenue" color="#1968f5" height={260} />
                )}
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card size="small" style={{ borderRadius: 16 }} title="Tỷ lệ lấp đầy (%)">
                {occupancySeries.length === 0 ? (
                  <Text type="secondary">Chưa có dữ liệu lịch phòng</Text>
                ) : (
                  <LineChart data={occupancySeries} xKey="period" height={260}
                             series={[{ key: 'rate', color: '#52c41a', label: 'Lấp đầy %' }]} />
                )}
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={10}>
              <Card size="small" style={{ borderRadius: 16 }} title="Phân bổ theo loại phòng">
                {byRoomType.length === 0 ? (
                  <Text type="secondary">
                    {granularity === 'MONTH' ? 'Chưa có dữ liệu' : 'Chọn độ phân giải "Tháng" để xem phân bổ theo loại phòng'}
                  </Text>
                ) : (
                  <DonutChart data={byRoomType} labelKey="roomType" valueKey="revenue" />
                )}
              </Card>
            </Col>
            <Col xs={24} lg={14}>
              <Card size="small" style={{ borderRadius: 16 }} title="Top phòng theo doanh thu">
                <DataTable
                  rowKey="roomId"
                  columns={[
                    { title: '#', width: 48, render: (_, __, i) => i + 1 },
                    { title: 'Mã phòng', dataIndex: 'roomCode', width: 100 },
                    { title: 'Tên phòng', dataIndex: 'roomName', ellipsis: true },
                    { title: 'Doanh thu', dataIndex: 'revenue', width: 150, align: 'right', render: vnd },
                    { title: 'Đơn', dataIndex: 'bookings', width: 70, align: 'right' },
                    { title: 'Đêm bán', dataIndex: 'nights', width: 90, align: 'right' },
                  ]}
                  dataSource={topRooms}
                  loading={loading}
                  emptyText="Chưa có phòng nào bán được trong kỳ này"
                />
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  )
}
