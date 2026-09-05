import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Input, Select, Button, Card, Row, Col, Skeleton, Empty, Pagination, Space, Typography, DatePicker, InputNumber, Slider } from 'antd'
import { SearchOutlined, EnvironmentOutlined, CalendarOutlined, TeamOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { fetchRooms } from '../store/roomSlice'
import { roomTypeLabel } from '../utils/format'
import { ROOM_TYPES } from '../utils/constants'
import RoomCard from '../components/common/RoomCard'

const { Title, Text } = Typography

export default function Home() {
  const dispatch = useDispatch()
  const { list, loading, totalElements, page, size, error } = useSelector((s) => s.room)
  const [q, setQ] = useState('')
  const [type, setType] = useState(undefined)
  const [range, setRange] = useState(null) // [checkIn, checkOut]
  const [guests, setGuests] = useState(null)
  const [priceRange, setPriceRange] = useState([0, 6000000])
  const [sortBy, setSortBy] = useState('default')
  const [currentPage, setCurrentPage] = useState(1)

  const buildParams = (p) => {
    const [min, max] = priceRange
    const params = {
      q: q || undefined,
      type: type || undefined,
      page: p - 1,
      size,
      minPrice: min > 0 ? min : undefined,
      maxPrice: max < 6000000 ? max : undefined,
      capacity: guests || undefined,
    }
    if (sortBy === 'price_asc') { params.sortBy = 'pricePerNight'; params.sortDir = 'asc' }
    else if (sortBy === 'price_desc') { params.sortBy = 'pricePerNight'; params.sortDir = 'desc' }
    else if (sortBy === 'newest') { params.sortBy = 'createdAt'; params.sortDir = 'desc' }
    return params
  }

  const load = (p = 1) => {
    setCurrentPage(p)
    dispatch(fetchRooms(buildParams(p)))
  }

  useEffect(() => {
    load(1)
  }, [])

  const onSearch = () => load(1)
  const onReset = () => {
    setQ('')
    setType(undefined)
    setRange(null)
    setGuests(null)
    setPriceRange([0, 6000000])
    setSortBy('default')
    setCurrentPage(1)
    dispatch(fetchRooms({ page: 0, size }))
  }

  const fmt = (v) => (v >= 1000000 ? `${(v / 1000000).toFixed(v % 1000000 ? 1 : 0)}tr` : `${(v / 1000).toFixed(0)}k`)

  return (
    <div>
      {/* Hero + thanh tìm kiếm như các trang đặt phòng hàng đầu */}
      <div style={{ background: 'linear-gradient(135deg,#0f6bff 0%,#6c5ce7 50%,#ff6b9d 100%)', padding: '32px 24px 40px', color: '#fff' }}>
        <div style={{ maxWidth: 1200, margin: '0 auto' }}>
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between' }}>
            <div>
              <Title level={2} style={{ color: '#fff', marginBottom: 6, fontSize: 30 }}>Đặt phòng thảnh thơi, vi vu khắp nơi</Title>
              <Text style={{ color: 'rgba(255,255,255,0.92)', fontSize: 14 }}>Hàng nghìn phòng đẹp đã sẵn sàng — tìm, so giá và giữ chỗ chỉ trong vài giây</Text>
            </div>
            <Space size={8} wrap>
              <span style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.28)', padding: '6px 14px', borderRadius: 999, fontSize: 12, backdropFilter: 'blur(8px)' }}>Miễn phí hủy</span>
              <span style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.28)', padding: '6px 14px', borderRadius: 999, fontSize: 12, backdropFilter: 'blur(8px)' }}>Xác nhận tức thì</span>
              <span style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.28)', padding: '6px 14px', borderRadius: 999, fontSize: 12, backdropFilter: 'blur(8px)' }}>Hỗ trợ 24/7</span>
            </Space>
          </div>

          <Card style={{ marginTop: 20, borderRadius: 16, boxShadow: '0 16px 40px rgba(0,0,0,0.18)' }} bodyStyle={{ padding: 16 }}>
            <Row gutter={[12, 12]} align="middle">
              <Col xs={24} md={7}>
                <Input size="large" prefix={<SearchOutlined />} placeholder="Tìm theo tên hoặc mã phòng (VD: R101, Deluxe)" value={q} onChange={(e) => setQ(e.target.value)} onPressEnter={onSearch} allowClear />
              </Col>
              <Col xs={12} md={4}>
                <Select size="large" placeholder="Loại phòng" value={type} onChange={(v) => setType(v)} options={[{ value: null, label: 'Tất cả loại' }, ...ROOM_TYPES.map((t) => ({ value: t, label: roomTypeLabel(t) }))]} style={{ width: '100%' }} allowClear />
              </Col>
              <Col xs={12} md={5}>
                <DatePicker.RangePicker
                  size="large"
                  style={{ width: '100%' }}
                  value={range}
                  onChange={(v) => setRange(v)}
                  suffixIcon={<CalendarOutlined />}
                  minDate={dayjs()}
                  format="DD/MM"
                  placeholder={['Nhận phòng', 'Trả phòng']}
                />
              </Col>
              <Col xs={12} md={3}>
                <InputNumber size="large" placeholder="Khách" min={1} max={20} value={guests} onChange={setGuests} style={{ width: '100%' }} prefix={<TeamOutlined />} />
              </Col>
              <Col xs={12} md={5} style={{ display: 'flex', gap: 8 }}>
                <Button type="primary" size="large" icon={<SearchOutlined />} onClick={onSearch} style={{ flex: 1, borderRadius: 10, fontWeight: 600 }}>Tìm kiếm</Button>
                <Button size="large" icon={<ReloadOutlined />} onClick={onReset} style={{ borderRadius: 10 }} />
              </Col>
            </Row>
            <Row gutter={16} align="middle" style={{ marginTop: 12 }}>
              <Col xs={24} md={10}>
                <Space size={8} style={{ width: '100%' }}>
                  <Text type="secondary" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>Giá/đêm:</Text>
                  <Slider range tooltip={{ formatter: fmt }} marks={{ 0: '0', 2000000: '2tr', 4000000: '4tr', 6000000: '6tr+' }} value={priceRange} onChange={setPriceRange} onChangeComplete={() => load(1)} min={0} max={6000000} step={100000} style={{ flex: 1, minWidth: 160 }} />
                </Space>
              </Col>
              <Col xs={12} md={6}>
                <Select size="small" value={sortBy} onChange={(v) => { setSortBy(v); setTimeout(() => load(1), 0) }} style={{ width: '100%' }} options={[
                  { value: 'default', label: 'Mặc định (mới nhất)' },
                  { value: 'price_asc', label: 'Giá thấp → cao' },
                  { value: 'price_desc', label: 'Giá cao → thấp' },
                ]} />
              </Col>
              <Col xs={12} md={8} style={{ textAlign: 'right' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>Tổng: <strong>{totalElements}</strong> phòng · Trang {currentPage}</Text>
              </Col>
            </Row>
          </Card>
        </div>
      </div>

      <div style={{ maxWidth: 1200, margin: '0 auto', padding: '24px 24px 32px' }}>
        {error && (
          <Card style={{ marginBottom: 16, borderColor: '#ffccc7', background: '#fff2f0', borderRadius: 12 }}>
            <Space><Text type="danger">Lỗi: {error}</Text><Button size="small" onClick={() => load(currentPage)}>Thử lại</Button></Space>
          </Card>
        )}

        {loading ? (
          <Row gutter={[16, 16]}>
            {Array.from({ length: 8 }).map((_, i) => (
              <Col key={i} xs={24} sm={12} md={8} lg={6}><Card style={{ borderRadius: 16 }}><Skeleton active paragraph={{ rows: 2 }} /></Card></Col>
            ))}
          </Row>
        ) : list.length === 0 ? (
          <Card style={{ borderRadius: 16, textAlign: 'center', padding: '40px 0' }}>
            <Empty description="Chưa có phòng phù hợp — thử đổi từ khóa, giá hoặc loại phòng" />
            <Button type="link" onClick={onReset} style={{ marginTop: 12 }}>Xóa bộ lọc</Button>
          </Card>
        ) : (
          <>
            <Title level={5} style={{ margin: '4px 0 12px' }}>
              <EnvironmentOutlined /> Gợi ý cho bạn · {totalElements} phòng
            </Title>
            <Row gutter={[16, 16]}>
              {list.map((room, idx) => (
                <Col key={room.id} xs={24} sm={12} md={8} lg={6}><RoomCard room={room} index={idx} /></Col>
              ))}
            </Row>
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: 24 }}>
              <Pagination current={currentPage} pageSize={size} total={totalElements} showSizeChanger={false} onChange={(p) => load(p)} showTotal={(t) => `Tổng ${t} phòng`} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
