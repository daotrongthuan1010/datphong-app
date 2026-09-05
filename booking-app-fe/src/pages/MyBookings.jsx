import { useEffect, useState, useCallback } from 'react'
import { Button, Tag, Card, Space, Typography, Empty, Skeleton, Popconfirm, message, Divider } from 'antd'
import { CalendarOutlined, TeamOutlined, FileTextOutlined } from '@ant-design/icons'
import PageHeader from '../components/common/PageHeader'
import DataTable from '../components/common/DataTable'
import { bookingApi } from '../api/bookings'
import { formatPrice, formatDate, formatDateTime, bookingStatusColor, bookingStatusLabel } from '../utils/format'

const { Text, Title } = Typography

export default function MyBookings() {
  const [data, setData] = useState({ content: [], page: 0, size: 10, totalElements: 0 })
  const [loading, setLoading] = useState(false)

  const load = useCallback((page = 0) => {
    setLoading(true)
    bookingApi
      .myList({ page, size: 10 })
      .then(setData)
      .catch((e) => message.error(e.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load(0)
  }, [])

  const onCancel = async (id) => {
    try {
      await bookingApi.cancel(id)
      message.success('Đã hủy đặt phòng')
      load(data.page)
    } catch (e) {
      message.error(e.message)
    }
  }

  const columns = [
    {
      title: 'Mã đặt phòng',
      dataIndex: 'bookingCode',
      width: 140,
      render: (c) => <Text strong copyable style={{ fontFamily: 'monospace' }}>{c}</Text>,
    },
    {
      title: 'Phòng',
      render: (_, r) => (
        <div>
          <Text strong>{r.roomName || r.roomCode}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>{r.roomCode} · {r.roomId ? `ID ${r.roomId}` : ''}</Text>
        </div>
      ),
    },
    {
      title: 'Ngày',
      width: 200,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <span><CalendarOutlined /> {formatDate(r.checkinDate)} → {formatDate(r.checkoutDate)}</span>
          <Text type="secondary" style={{ fontSize: 12 }}><TeamOutlined /> {r.guestsCount} khách</Text>
        </Space>
      ),
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalPrice',
      width: 140,
      render: (v) => <Text strong style={{ color: '#ff3b30' }}>{formatPrice(Number(v))}</Text>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 130,
      render: (s) => <Tag color={bookingStatusColor[s]} style={{ borderRadius: 999 }}>{bookingStatusLabel(s)}</Tag>,
    },
    {
      title: 'Đặt lúc',
      dataIndex: 'createdAt',
      width: 150,
      render: formatDateTime,
    },
    {
      title: '',
      width: 100,
      render: (_, r) => {
        const canCancel = r.status !== 'CANCELLED' && r.status !== 'COMPLETED' && r.status !== 'REFUNDED'
        if (!canCancel) return <Text type="secondary" style={{ fontSize: 12 }}>—</Text>
        return (
          <Popconfirm title="Hủy đặt phòng này?" description="Bạn có thể đặt lại sau nếu còn phòng trống." onConfirm={() => onCancel(r.id)}>
            <Button size="small" danger>Hủy</Button>
          </Popconfirm>
        )
      },
    },
  ]

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '16px 24px 32px' }}>
      <PageHeader
        title={<span><FileTextOutlined /> Đặt phòng của tôi</span>}
        description="Theo dõi toàn bộ lịch sử đặt phòng, trạng thái xác nhận và tổng tiền — hủy miễn phí trước 24h"
      />

      {loading && data.content.length === 0 ? (
        <Card><Skeleton active /></Card>
      ) : data.content.length === 0 && !loading ? (
        <Card style={{ borderRadius: 16, textAlign: 'center', padding: '40px 0' }}>
          <Empty description="Bạn chưa có đặt phòng nào — hãy tìm phòng ưng ý ở trang chủ và đặt ngay" />
        </Card>
      ) : (
        <DataTable
          columns={columns}
          dataSource={data.content}
          loading={loading}
          page={data.page}
          size={data.size}
          totalElements={data.totalElements}
          onPageChange={load}
          rowKey="id"
          scroll={{ x: 860 }}
          emptyText="Chưa có đặt phòng nào"
        />
      )}
    </div>
  )
}
