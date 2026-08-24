import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Input, Select, Button, Card, Tag, Row, Col, Skeleton, Empty, Pagination, Space, Typography, InputNumber, DatePicker, message } from 'antd'
import { SearchOutlined, EnvironmentOutlined, CalendarOutlined, TeamOutlined, StarFilled, HeartOutlined, HeartFilled, ReloadOutlined } from '@ant-design/icons'
import { fetchRooms } from '../store/roomSlice'
import { formatPrice, roomTypeLabel, roomStatusColor } from '../utils/format'
import { ROOM_TYPES } from '../utils/constants'

const { Title, Text } = Typography

const MOCK_IMAGES = [
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
  'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&q=80',
  'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&q=80',
  'https://images.unsplash.com/photo-1551882547-b79c4176354d?w=800&q=80',
]

function RoomCard({ room, index }) {
  const [liked, setLiked] = useState(false)
  const img = room.imageUrl || MOCK_IMAGES[index % MOCK_IMAGES.length]
  return (
    <Card
      hoverable
      style={{ borderRadius: 16, overflow: 'hidden', border: '1px solid #eef0f5' }}
      bodyStyle={{ padding: 14 }}
      cover={
        <div style={{ position: 'relative', height: 180, overflow: 'hidden', background: '#f0f2f5' }}>
          <img src={img} alt={room.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} loading="lazy" />
          <div style={{ position: 'absolute', top: 10, left: 10, display: 'flex', gap: 6 }}>
            <Tag color={roomStatusColor(room.status)} style={{ margin: 0, borderRadius: 999 }}>
              {room.status}
            </Tag>
            <Tag color="blue" style={{ margin: 0, borderRadius: 999 }}>
              {roomTypeLabel(room.type)}
            </Tag>
          </div>
          <Button
            size="small"
            shape="circle"
            icon={liked ? <HeartFilled style={{ color: '#ff4d4f' }} /> : <HeartOutlined />}
            onClick={(e) => {
              e.preventDefault()
              setLiked(!liked)
            }}
            style={{ position: 'absolute', top: 10, right: 10, background: 'rgba(255,255,255,0.95)' }}
          />
          <div style={{ position: 'absolute', bottom: 8, left: 10, background: 'rgba(0,0,0,0.55)', color: '#fff', padding: '2px 8px', borderRadius: 999, fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}>
            <StarFilled style={{ color: '#ffd666', fontSize: 11 }} /> 4.{(index % 5) + 4} · {(index + 12) * 3} đánh giá
          </div>
        </div>
      }
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 6 }}>
        <Text strong style={{ fontSize: 15, lineHeight: 1.3, flex: 1 }} ellipsis>
          {room.name}
        </Text>
        <Text type="secondary" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>
          {room.code}
        </Text>
      </div>
      <Space size={6} style={{ color: '#888', fontSize: 12, marginBottom: 8 }}>
        <EnvironmentOutlined /> Quận 1, TP.HCM · <TeamOutlined /> {room.capacity} khách
      </Space>
      {room.description && (
        <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 10 }} ellipsis>
          {room.description}
        </Text>
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div>
          <Text strong style={{ fontSize: 16, color: '#ff3b30' }}>
            {formatPrice(room.pricePerNight)}
          </Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {' '}
            / đêm
          </Text>
        </div>
        <Button type="primary" size="small" style={{ borderRadius: 999, fontWeight: 600 }}>
          Đặt ngay
        </Button>
      </div>
    </Card>
  )
}

export default function Home() {
  const dispatch = useDispatch()
  const { list, loading, totalElements, page, size, error } = useSelector((s) => s.room)
  const [q, setQ] = useState('')
  const [type, setType] = useState(undefined)
  const [currentPage, setCurrentPage] = useState(1)

  const load = (p = 1, extra = {}) => {
    setCurrentPage(p)
    dispatch(fetchRooms({ q: q || undefined, type: type || undefined, page: p - 1, size, ...extra }))
  }

  useEffect(() => {
    load(1)
  }, [])

  const onSearch = () => load(1)
  const onReset = () => {
    setQ('')
    setType(undefined)
    dispatch(fetchRooms({ page: 0, size }))
    setCurrentPage(1)
  }

  return (
    <div>
      <div
        style={{
          background: 'linear-gradient(135deg,#0f6bff 0%,#6c5ce7 50%,#ff6b9d 100%)',
          padding: '28px 24px 36px',
          color: '#fff',
        }}
      >
        <div style={{ maxWidth: 1200, margin: '0 auto' }}>
          <Title level={2} style={{ color: '#fff', marginBottom: 6 }}>
            Tìm chỗ nghỉ hoàn hảo cho chuyến đi
          </Title>
          <Text style={{ color: 'rgba(255,255,255,0.9)' }}>Gợi ý từ Traveloka & Agoda — lọc theo loại phòng, tìm theo tên/mã, phân trang từ BE</Text>

          <Card style={{ marginTop: 20, borderRadius: 16, boxShadow: '0 12px 32px rgba(0,0,0,0.18)' }} bodyStyle={{ padding: 16 }}>
            <Row gutter={[12, 12]} align="middle">
              <Col xs={24} md={8}>
                <Input
                  size="large"
                  prefix={<SearchOutlined />}
                  placeholder="Tìm theo tên hoặc mã phòng (VD: R101, Deluxe)"
                  value={q}
                  onChange={(e) => setQ(e.target.value)}
                  onPressEnter={onSearch}
                  allowClear
                />
              </Col>
              <Col xs={12} md={5}>
                <Select
                  size="large"
                  placeholder="Loại phòng"
                  value={type}
                  onChange={setType}
                  options={[{ value: undefined, label: 'Tất cả loại' }, ...ROOM_TYPES.map((t) => ({ value: t, label: roomTypeLabel(t) }))]}
                  style={{ width: '100%' }}
                  allowClear
                />
              </Col>
              <Col xs={12} md={4}>
                <DatePicker size="large" placeholder="Ngày nhận" style={{ width: '100%' }} suffixIcon={<CalendarOutlined />} />
              </Col>
              <Col xs={12} md={3}>
                <InputNumber size="large" placeholder="Khách" min={1} max={20} style={{ width: '100%' }} prefix={<TeamOutlined />} />
              </Col>
              <Col xs={12} md={4} style={{ display: 'flex', gap: 8 }}>
                <Button type="primary" size="large" icon={<SearchOutlined />} onClick={onSearch} style={{ flex: 1, borderRadius: 10, fontWeight: 600 }}>
                  Tìm kiếm
                </Button>
                <Button size="large" icon={<ReloadOutlined />} onClick={onReset} style={{ borderRadius: 10 }} />
              </Col>
            </Row>
            <div style={{ marginTop: 10, display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                Gợi ý:
              </Text>
              {['Deluxe', 'R101', 'FAMILY'].map((k) => (
                <Tag key={k} style={{ cursor: 'pointer', borderRadius: 999 }} onClick={() => { setQ(k); setTimeout(() => load(1, { q: k }), 0) }}>
                  {k}
                </Tag>
              ))}
              <Text type="secondary" style={{ fontSize: 12, marginLeft: 'auto' }}>
                Tổng: <strong>{totalElements}</strong> phòng · Trang {currentPage}
              </Text>
            </div>
          </Card>
        </div>
      </div>

      <div style={{ maxWidth: 1200, margin: '0 auto', padding: '24px 24px 32px' }}>
        {error && (
          <Card style={{ marginBottom: 16, borderColor: '#ffccc7', background: '#fff2f0' }}>
            <Space>
              <Text type="danger">Lỗi: {error}</Text>
              <Button size="small" onClick={() => load(currentPage)}>
                Thử lại
              </Button>
            </Space>
          </Card>
        )}

        {loading ? (
          <Row gutter={[16, 16]}>
            {Array.from({ length: 8 }).map((_, i) => (
              <Col key={i} xs={24} sm={12} md={8} lg={6}>
                <Card style={{ borderRadius: 16 }}>
                  <Skeleton active paragraph={{ rows: 2 }} />
                </Card>
              </Col>
            ))}
          </Row>
        ) : list.length === 0 ? (
          <Card style={{ borderRadius: 16, textAlign: 'center', padding: '40px 0' }}>
            <Empty description="Chưa có phòng phù hợp — thử đổi từ khóa hoặc tạo phòng mới từ BE" />
            <Button type="link" onClick={onReset} style={{ marginTop: 12 }}>
              Xóa bộ lọc
            </Button>
          </Card>
        ) : (
          <>
            <Row gutter={[16, 16]}>
              {list.map((room, idx) => (
                <Col key={room.id} xs={24} sm={12} md={8} lg={6}>
                  <RoomCard room={room} index={idx} />
                </Col>
              ))}
            </Row>
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: 24 }}>
              <Pagination
                current={currentPage}
                pageSize={size}
                total={totalElements}
                showSizeChanger={false}
                onChange={(p) => load(p)}
                showTotal={(t) => `Tổng ${t} phòng`}
              />
            </div>
          </>
        )}

        <Card style={{ marginTop: 24, borderRadius: 16, background: '#fffbe6', borderColor: '#ffe58f' }} bodyStyle={{ padding: 16 }}>
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <Text strong>Note cho BE (ý tưởng Traveloka/Agoda để mở rộng):</Text>
            <Text type="secondary" style={{ fontSize: 13 }}>
              • Home đang gọi <code>GET /api/rooms?q=&type=&page=&size=</code> (đã có) — BE đã hỗ trợ search/filter/phân trang. FE dùng Redux <code>roomSlice</code> + <code>axios</code> qua <code>VITE_API_BASE_URL</code> (đổi 1 chỗ trong <code>.env</code>).
              <br />• Tiếp theo gợi ý: thêm <code>/api/bookings</code>, <code>/api/hotels</code>, auth JWT, upload ảnh phòng qua MinIO <code>vivu-bucket</code>, cache list phòng qua Redis db 1.
            </Text>
          </Space>
        </Card>
      </div>
    </div>
  )
}
