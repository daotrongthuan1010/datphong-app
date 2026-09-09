import { useEffect, useMemo, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Modal, Form, Input, InputNumber, message, Tag, Typography, Space, Skeleton, Divider, Descriptions, Card, Carousel, Rate, Upload, Select, Empty, Pagination, Avatar, Alert } from 'antd'
import { CalendarOutlined, TeamOutlined, ArrowLeftOutlined, EnvironmentOutlined, LeftOutlined, RightOutlined, PictureOutlined, VideoCameraOutlined, CameraOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { useSelector } from 'react-redux'
import dayjs from 'dayjs'
import { roomApi } from '../api/rooms'
import { bookingApi } from '../api/bookings'
import { paymentApi } from '../api/payments'
import { reviewApi } from '../api/reviews'
import { formatPrice, formatDateTime, formatDate, roomTypeLabel, roomStatusLabel, roomStatusColor, calendarStatusLabel, calendarDayColor, calendarDayBorder, countdownFrom } from '../utils/format'

const { Title, Text, Paragraph } = Typography
const ROOM_PLACEHOLDER = 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&q=80'

const ArrowBtn = ({ dir, onClick }) => (
  <button
    onClick={onClick}
    style={{
      position: 'absolute',
      top: '50%',
      [dir === 'prev' ? 'left' : 'right']: 14,
      transform: 'translateY(-50%)',
      zIndex: 5,
      width: 42,
      height: 42,
      borderRadius: '50%',
      border: 'none',
      background: 'rgba(0,0,0,0.45)',
      color: '#fff',
      fontSize: 16,
      cursor: 'pointer',
      display: 'grid',
      placeItems: 'center',
      backdropFilter: 'blur(4px)',
      transition: 'background .2s',
    }}
    onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(0,0,0,0.75)')}
    onMouseLeave={(e) => (e.currentTarget.style.background = 'rgba(0,0,0,0.45)')}
    aria-label={dir === 'prev' ? 'Ảnh trước' : 'Ảnh sau'}
  >
    {dir === 'prev' ? <LeftOutlined /> : <RightOutlined />}
  </button>
)

function Gallery({ media, name }) {
  const ref = useRef(null)
  const [active, setActive] = useState(0)
  return (
    <div>
      <div style={{ position: 'relative', borderRadius: 16, overflow: 'hidden', background: '#000' }}>
        <Carousel ref={(r) => { ref.current = r }} afterChange={(i) => setActive(i)} dots={{ className: 'vivu-gallery-dots' }} draggable infinite speed={350}>
          {media.map((m, i) =>
            m.type === 'VIDEO' ? (
              <div key={i}><video src={m.url} controls preload="metadata" style={{ width: '100%', height: 420, display: 'block', background: '#000' }} /></div>
            ) : (
              <div key={i}><img src={m.url} alt={`${name} ${i + 1}`} style={{ width: '100%', height: 420, objectFit: 'cover', display: 'block' }} /></div>
            ),
          )}
        </Carousel>
        {media.length > 1 && (
          <>
            <ArrowBtn dir="prev" onClick={() => ref.current?.prev()} />
            <ArrowBtn dir="next" onClick={() => ref.current?.next()} />
            <span style={{ position: 'absolute', bottom: 12, right: 14, zIndex: 5, background: 'rgba(0,0,0,0.55)', color: '#fff', padding: '3px 10px', borderRadius: 999, fontSize: 12 }}>
              <PictureOutlined /> {active + 1} / {media.length}
            </span>
          </>
        )}
      </div>
      {media.length > 1 && (
        <div style={{ display: 'flex', gap: 8, marginTop: 8, overflowX: 'auto', paddingBottom: 4 }}>
          {media.map((m, i) =>
            m.type === 'VIDEO' ? (
              <div key={i} onClick={() => { ref.current?.goTo(i); setActive(i) }} style={{ width: 84, height: 60, borderRadius: 8, cursor: 'pointer', flexShrink: 0, background: '#111', display: 'grid', placeItems: 'center', color: '#fff', fontSize: 18, border: active === i ? '2px solid #1968f5' : '2px solid transparent', opacity: active === i ? 1 : 0.75 }}>
                <VideoCameraOutlined />
              </div>
            ) : (
              <img key={i} src={m.url} alt="" onClick={() => { ref.current?.goTo(i); setActive(i) }} style={{ width: 84, height: 60, objectFit: 'cover', borderRadius: 8, cursor: 'pointer', flexShrink: 0, border: active === i ? '2px solid #1968f5' : '2px solid transparent', opacity: active === i ? 1 : 0.75 }} />
            ),
          )}
        </div>
      )}
    </div>
  )
}

function ReviewItem({ r }) {
  return (
    <div style={{ padding: '14px 0', borderBottom: '1px solid #f0f0f0' }}>
      <Space align="start" size={12}>
        <Avatar style={{ background: '#1968f5', flexShrink: 0 }}>{(r.userFullName || '?')[0]?.toUpperCase()}</Avatar>
        <div style={{ minWidth: 0 }}>
          <Space size={8} wrap><Text strong>{r.userFullName}</Text><Rate disabled value={r.rating} style={{ fontSize: 13 }} /></Space>
          <div><Text type="secondary" style={{ fontSize: 12 }}>{formatDateTime(r.createdAt)}</Text></div>
          {r.comment && <Paragraph style={{ margin: '6px 0 0', fontSize: 14, whiteSpace: 'pre-wrap' }}>{r.comment}</Paragraph>}
          {r.media && r.media.length > 0 && (
            <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
              {r.media.map((m) => m.mediaType === 'VIDEO'
                ? <video key={m.url} src={m.url} controls preload="metadata" style={{ width: 220, borderRadius: 10, background: '#000' }} />
                : <img key={m.url} src={m.url} alt="" onClick={() => window.open(m.url, '_blank')} style={{ width: 90, height: 90, objectFit: 'cover', borderRadius: 10, cursor: 'zoom-in' }} />)}
            </div>
          )}
        </div>
      </Space>
    </div>
  )
}

function ReviewsSection({ room }) {
  const { isAuthenticated } = useSelector((s) => s.auth)
  const [data, setData] = useState({ content: [], totalElements: 0, avgRating: 0 })
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [myBookings, setMyBookings] = useState([])
  const [formOpen, setFormOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm()
  const load = (p = 1) => {
    setLoading(true)
    reviewApi.listByRoom(room.id, { page: p - 1, size: 5 }).then(setData).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(() => { load(1) }, [room.id])
  const openForm = () => {
    setFormOpen(true)
    if (isAuthenticated && myBookings.length === 0) {
      bookingApi.myList({ page: 0, size: 50 }).then((res) => {
        const eligible = (res.content || []).filter((b) => b.roomId === room.id && (b.status === 'CONFIRMED' || b.status === 'COMPLETED'))
        setMyBookings(eligible)
      }).catch(() => {})
    }
  }
  const submitReview = async () => {
    const values = await form.validateFields()
    const files = (values.media || []).map((f) => f.originFileObj).filter(Boolean)
    setSubmitting(true)
    try {
      await reviewApi.create({ bookingId: values.bookingId, rating: values.rating, comment: values.comment || undefined }, files)
      message.success('Cảm ơn bạn đã đánh giá!')
      setFormOpen(false); form.resetFields(); load(1)
    } catch (e) { message.error(e.message) } finally { setSubmitting(false) }
  }
  return (
    <Card style={{ borderRadius: 16, marginTop: 16 }} bodyStyle={{ padding: 20 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <Space size={16} align="center">
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 36, fontWeight: 800, lineHeight: 1 }}>{data.avgRating || '-'}</div>
            <Rate disabled allowHalf value={data.avgRating || 0} style={{ fontSize: 14 }} />
          </div>
          <div>
            <Title level={5} style={{ margin: 0 }}>Đánh giá từ khách đã ở</Title>
            <Text type="secondary" style={{ fontSize: 13 }}>{data.totalElements} đánh giá · chỉ khách có đặt phòng hợp lệ mới gửi được</Text>
          </div>
        </Space>
        {isAuthenticated && <Button type="primary" icon={<CameraOutlined />} onClick={openForm}>Viết đánh giá</Button>}
      </div>
      <Modal title={`Đánh giá phòng ${room.name}`} open={formOpen} onCancel={() => setFormOpen(false)} onOk={submitReview} confirmLoading={submitting} okText="Gửi đánh giá" cancelText="Hủy" destroyOnClose>
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item name="bookingId" label="Chọn đặt phòng" rules={[{ required: true, message: 'Chọn đặt phòng để đánh giá' }]}>
            <Select placeholder={myBookings.length === 0 ? 'Không tìm thấy đặt phòng hợp lệ cho phòng này' : 'Chọn mã đặt phòng'} options={myBookings.map((b) => ({ value: b.id, label: `${b.bookingCode} · ${formatDate(b.checkinDate)} → ${formatDate(b.checkoutDate)}` }))} />
          </Form.Item>
          <Form.Item name="rating" label="Chấm điểm" rules={[{ required: true, message: 'Chọn số sao' }]}><Rate /></Form.Item>
          <Form.Item name="comment" label="Cảm nhận của bạn" rules={[{ max: 1000, message: 'Tối đa 1000 ký tự' }]}><Input.TextArea rows={3} placeholder="Phòng sạch sẽ, nhân viên thân thiện..." /></Form.Item>
          <Form.Item name="media" label="Thêm ảnh / video (tùy chọn)" valuePropName="fileList" getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)} extra="Hỗ trợ cả video để review chi tiết phòng — video sẽ phát ngay trong phần đánh giá">
            <Upload listType="picture-card" accept="image/*,video/*" multiple beforeUpload={() => false}><div><VideoCameraOutlined /><div style={{ fontSize: 12 }}>Tải lên</div></div></Upload>
          </Form.Item>
        </Form>
      </Modal>
      {loading && data.content.length === 0 ? <Skeleton active style={{ marginTop: 16 }} /> : data.content.length === 0 ? <Empty style={{ marginTop: 20 }} description="Chưa có đánh giá nào — hãy là người đầu tiên sau khi đặt phòng" /> : (
        <>
          {data.content.map((r) => <ReviewItem key={r.id} r={r} />)}
          {data.totalElements > 5 && <div style={{ display: 'flex', justifyContent: 'center', marginTop: 12 }}><Pagination current={page} pageSize={5} total={data.totalElements} showSizeChanger={false} onChange={(p) => { setPage(p); load(p) }} /></div>}
        </>
      )}
    </Card>
  )
}

// ---- Lịch tháng: ô ngày màu theo RoomCalendar, bấm chọn khoảng ----

function CalendarMonth({ yearMonth, rows, selected, onPick }) {
  const ym = dayjs(yearMonth + '-01')
  const start = ym.startOf('month')
  const end = ym.endOf('month')
  const byDate = useMemo(() => {
    const m = new Map()
    for (const r of rows) m.set(r.date, r.status)
    return m
  }, [rows])

  const firstDow = (start.day() + 6) % 7 // Mon=0
  const cells = []
  for (let i = 0; i < firstDow; i++) cells.push(null)
  for (let d = 0; d < end.date(); d++) cells.push(start.add(d, 'day'))
  while (cells.length % 7 !== 0) cells.push(null)

  const selFrom = selected.checkin ? dayjs(selected.checkin) : null
  const selTo = selected.checkout ? dayjs(selected.checkout) : null

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 6, marginBottom: 6 }}>
        {['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].map((w) => <div key={w} style={{ textAlign: 'center', fontSize: 11, color: '#888', fontWeight: 700 }}>{w}</div>)}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 6 }}>
        {cells.map((d, i) => {
          if (!d) return <div key={i} />
          const iso = d.format('YYYY-MM-DD')
          const st = byDate.get(iso) || 'AVAILABLE'
          const isPast = d.isBefore(dayjs().startOf('day'))
          const blocked = st !== 'AVAILABLE'
          const disabled = isPast || blocked
          const inRange = selFrom && selTo && (d.isAfter(selFrom, 'day') && d.isBefore(selTo, 'day'))
          const isEdge = (selFrom && d.isSame(selFrom, 'day')) || (selTo && d.isSame(selTo, 'day'))
          const bg = isEdge ? '#1677ff' : inRange ? '#e6f4ff' : (disabled ? (blocked ? calendarDayColor[st] : '#f5f5f5') : calendarDayColor.AVAILABLE)
          const border = isEdge ? '#1677ff' : (blocked ? calendarDayBorder[st] : (inRange ? '#91caff' : calendarDayBorder.AVAILABLE))
          const color = isEdge ? '#fff' : disabled ? '#aaa' : '#1a1a1a'
          return (
            <button
              key={iso}
              disabled={disabled}
              onClick={() => onPick(iso)}
              title={`${iso} · ${calendarStatusLabel(st)}`}
              style={{
                height: 38, borderRadius: 8, border: `1px solid ${border}`, background: bg, color,
                fontWeight: isEdge ? 800 : 600, fontSize: 13, cursor: disabled ? 'not-allowed' : 'pointer',
                opacity: disabled && !blocked ? 0.45 : 1,
              }}
            >
              {d.date()}
            </button>
          )
        })}
      </div>
      <Space size={8} wrap style={{ marginTop: 10 }}>
        <Tag style={{ background: calendarDayColor.AVAILABLE, borderColor: calendarDayBorder.AVAILABLE }}>Còn trống</Tag>
        <Tag style={{ background: calendarDayColor.BLOCKED, borderColor: calendarDayBorder.BLOCKED }}>Đang giữ chỗ</Tag>
        <Tag style={{ background: calendarDayColor.BOOKED, borderColor: calendarDayBorder.BOOKED }}>Đã đặt</Tag>
      </Space>
    </div>
  )
}

export default function RoomDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useSelector((s) => s.auth)
  const [room, setRoom] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm()
  const [ym, setYm] = useState(dayjs().format('YYYY-MM'))
  const [calRows, setCalRows] = useState([])
  const [calLoading, setCalLoading] = useState(false)
  const [selected, setSelected] = useState({ checkin: '', checkout: '' })
  const [guests, setGuests] = useState(2)
  const [voucherCode, setVoucherCode] = useState('')
  const [hold, setHold] = useState(null)
  const [nowMs, setNowMs] = useState(Date.now())
  const nights = useMemo(() => {
    if (!selected.checkin || !selected.checkout) return 0
    const a = dayjs(selected.checkin), b = dayjs(selected.checkout)
    const n = b.diff(a, 'day')
    return n > 0 ? n : 0
  }, [selected])

  useEffect(() => {
    setLoading(true)
    roomApi.get(id).then((r) => { setRoom(r); setGuests(Math.min(2, r.capacity || 2)) }).catch((e) => setError(e.message)).finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (!room) return
    setCalLoading(true)
    roomApi.calendar(room.id, { month: ym }).then(setCalRows).catch(() => setCalRows([])).finally(() => setCalLoading(false))
  }, [room?.id, ym])

  useEffect(() => {
    if (!hold?.holdExpiresAt) return
    const t = setInterval(() => setNowMs(Date.now()), 1000)
    return () => clearInterval(t)
  }, [hold?.holdExpiresAt])

  const remaining = hold ? countdownFrom(hold.holdExpiresAt, nowMs) : ''
  const expired = hold && !remaining

  const onPick = (iso) => {
    if (!selected.checkin || (selected.checkin && selected.checkout)) {
      setSelected({ checkin: iso, checkout: '' })
      return
    }
    const a = dayjs(selected.checkin), b = dayjs(iso)
    if (b.isSame(a, 'day')) return
    if (b.isAfter(a)) setSelected({ checkin: selected.checkin, checkout: iso })
    else setSelected({ checkin: iso, checkout: selected.checkin })
  }

  const submitBooking = async () => {
    if (!selected.checkin || !selected.checkout) { message.error('Chọn ngày nhận và trả phòng trên lịch'); return }
    if (nights < 1) { message.error('Ngày trả phòng phải sau ngày nhận phòng'); return }
    setSubmitting(true)
    try {
      const created = await bookingApi.create({
        roomId: room.id,
        checkinDate: selected.checkin,
        checkoutDate: selected.checkout,
        guestsCount: guests,
        voucherCode: voucherCode.trim() || undefined,
      })
      setHold(created)
      setNowMs(Date.now())
      message.success('Giữ chỗ thành công — hoàn tất thanh toán trước khi hết hạn')
    } catch (e) {
      const s = e.status
      if (s === 409) message.error(e.message || 'Phòng đã có người giữ trong khoảng ngày này')
      else message.error(e.message)
    } finally { setSubmitting(false) }
  }

  if (loading) return <div style={{ maxWidth: 1100, margin: '0 auto', padding: 24 }}><Skeleton active paragraph={{ rows: 6 }} /></div>
  if (error || !room) return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: 24 }}>
      <Card><Text type="danger">Lỗi: {error || 'Không tìm thấy phòng'}</Text><Button style={{ marginLeft: 12 }} onClick={() => navigate(-1)}>Quay lại</Button></Card>
    </div>
  )

  const imageUrls = room.images && room.images.length > 0 ? room.images : []
  const videoUrls = room.videos && room.videos.length > 0 ? room.videos : []
  const galleryMedia = [
    ...imageUrls.map((u) => ({ url: u, type: 'IMAGE' })),
    ...videoUrls.map((u) => ({ url: u, type: 'VIDEO' })),
  ]
  const hero = room.imageUrl || imageUrls[0] || null
  const amenities = room.amenities || []

  return (
    <div style={{ background: '#f5f7fb', minHeight: 'calc(100vh - 64px)' }}>
      <div style={{ maxWidth: 1100, margin: '0 auto', padding: '16px 24px 32px' }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} style={{ marginBottom: 12, paddingLeft: 0 }}>Quay lại danh sách</Button>
        {galleryMedia.length > 0 ? (
          <Gallery media={galleryMedia} name={room.name} />
        ) : hero ? (
          <div style={{ borderRadius: 16, overflow: 'hidden', height: 420, background: '#f0f2f5' }}>
            <img src={hero} alt={room.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          </div>
        ) : (
          <Card style={{ borderRadius: 16, textAlign: 'center', padding: 40, background: '#f5f7fb' }}>
            <PictureOutlined style={{ fontSize: 36, color: '#999' }} />
            <div style={{ marginTop: 8 }}><Text type="secondary">Chưa có ảnh — admin có thể thêm tại Quản lý phòng → Media</Text></div>
          </Card>
        )}
        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 0.9fr', gap: 16, marginTop: 16, alignItems: 'start' }}>
          <div>
            <Card style={{ borderRadius: 16 }} bodyStyle={{ padding: 20 }}>
              <Space size={8} wrap style={{ marginBottom: 8 }}>
                <Tag color={roomStatusColor[room.status] || 'default'} style={{ borderRadius: 999, margin: 0 }}>{roomStatusLabel(room.status)}</Tag>
                <Tag color="blue" style={{ borderRadius: 999, margin: 0 }}>{roomTypeLabel(room.type)}</Tag>
                <Tag style={{ borderRadius: 999, margin: 0 }}>{room.code}</Tag>
              </Space>
              <Title level={3} style={{ margin: 0, marginBottom: 6 }}>{room.name}</Title>
              <Space size={12} style={{ color: '#666', fontSize: 13, flexWrap: 'wrap' }}>
                {room.address ? <><EnvironmentOutlined /> {room.address}</> : <Text type="secondary" style={{ fontSize: 12 }}>Chưa cập nhật địa chỉ</Text>}
                <span><TeamOutlined /> Tối đa {room.capacity} khách</span>
                {typeof room.avgRating === 'number' && room.reviewCount > 0 && (
                  <span>· {Number(room.avgRating).toFixed(1)} / 5 · {room.reviewCount} đánh giá</span>
                )}
              </Space>
              {room.description && <><Divider style={{ margin: '16px 0' }} /><Text style={{ fontSize: 14, lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>{room.description}</Text></>}
              <Divider style={{ margin: '16px 0' }} />
              <Title level={5} style={{ marginBottom: 8 }}>Tiện nghi</Title>
              {amenities.length > 0 ? (
                <Space size={[8, 8]} wrap>
                  {amenities.map((a) => (
                    <Tag key={a.id} style={{ borderRadius: 999, padding: '4px 12px' }}>{a.name}</Tag>
                  ))}
                </Space>
              ) : (
                <Text type="secondary" style={{ fontSize: 13 }}>Chưa cập nhật tiện nghi.</Text>
              )}
              <Divider style={{ margin: '16px 0' }} />
              <Descriptions column={2} size="small" labelStyle={{ color: '#888' }}>
                <Descriptions.Item label="Mã phòng">{room.code}</Descriptions.Item>
                <Descriptions.Item label="Loại phòng">{roomTypeLabel(room.type)}</Descriptions.Item>
                <Descriptions.Item label="Sức chứa">{room.capacity} người</Descriptions.Item>
                <Descriptions.Item label="Trạng thái">{roomStatusLabel(room.status)}</Descriptions.Item>
                <Descriptions.Item label="Ngày tạo">{formatDateTime(room.createdAt)}</Descriptions.Item>
                <Descriptions.Item label="Cập nhật">{formatDateTime(room.updatedAt)}</Descriptions.Item>
              </Descriptions>
            </Card>
            <ReviewsSection room={room} />
          </div>

          <div style={{ position: 'sticky', top: 80, display: 'grid', gap: 16 }}>
            {hold && (
              <Alert
                type={expired ? 'error' : 'warning'}
                showIcon
                icon={<ClockCircleOutlined />}
                message={expired ? 'Giữ chỗ đã hết hạn' : `Đang giữ chỗ · còn ${remaining || '...'}`}
                description={
                  expired ? (
                    'Phòng đã được trả lại. Vui lòng chọn lại ngày và giữ chỗ mới.'
                  ) : (
                    <Space direction="vertical" size={8} style={{ width: '100%' }}>
                      <Text style={{ fontSize: 13 }}>
                        Mã <Text strong copyable>{hold.bookingCode}</Text> · {formatDate(hold.checkinDate)} → {formatDate(hold.checkoutDate)} · {formatPrice(Number(hold.totalPrice))}
                      </Text>
                      <Button type="primary" block icon={<ClockCircleOutlined />} onClick={() => navigate(`/checkout/${hold.id}`)} style={{ borderRadius: 10, fontWeight: 700 }}>
                        Thanh toán ngay
                      </Button>
                      <Button block onClick={() => navigate('/bookings')} style={{ borderRadius: 10 }}>Xem đặt phòng / Thanh toán sau</Button>
                    </Space>
                  )
                }
                style={{ borderRadius: 12 }}
              />
            )}

            <Card style={{ borderRadius: 16, boxShadow: '0 12px 32px rgba(0,0,0,0.08)' }} bodyStyle={{ padding: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
                <Text strong style={{ fontSize: 22, color: '#ff3b30' }}>{formatPrice(room.pricePerNight)}</Text>
                <Text type="secondary">/ đêm</Text>
              </div>
              <Text type="secondary" style={{ fontSize: 12 }}>Giá đã bao gồm thuế và phí</Text>
              <Divider style={{ margin: '16px 0' }} />

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <Text strong><CalendarOutlined /> Lịch phòng</Text>
                <Space size={4}>
                  <Button size="small" icon={<LeftOutlined />} onClick={() => setYm(dayjs(ym + '-01').subtract(1, 'month').format('YYYY-MM'))} />
                  <Text strong style={{ minWidth: 86, textAlign: 'center', display: 'inline-block' }}>{dayjs(ym + '-01').format('MM/YYYY')}</Text>
                  <Button size="small" icon={<RightOutlined />} onClick={() => setYm(dayjs(ym + '-01').add(1, 'month').format('YYYY-MM'))} />
                </Space>
              </div>
              {calLoading ? <Skeleton active paragraph={{ rows: 2 }} /> : <CalendarMonth yearMonth={ym} rows={calRows} selected={selected} onPick={onPick} />}

              <Divider style={{ margin: '16px 0' }} />

              {!isAuthenticated ? (
                <Space direction="vertical" style={{ width: '100%' }} size={10}>
                  <Text type="secondary">Đăng nhập để chọn ngày trên lịch và giữ chỗ.</Text>
                  <Button type="primary" size="large" block onClick={() => navigate('/login')} style={{ height: 48, borderRadius: 12, fontWeight: 700 }}>Đăng nhập để đặt</Button>
                </Space>
              ) : (
                <Form layout="vertical" requiredMark={false} onFinish={submitBooking}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                    <Form.Item label="Nhận phòng" style={{ marginBottom: 8 }}>
                      <Input value={selected.checkin} readOnly placeholder="Chọn trên lịch" suffix={<CalendarOutlined />} />
                    </Form.Item>
                    <Form.Item label="Trả phòng" style={{ marginBottom: 8 }}>
                      <Input value={selected.checkout} readOnly placeholder="Chọn trên lịch" suffix={<CalendarOutlined />} />
                    </Form.Item>
                  </div>
                  {nights > 0 && <Text type="secondary" style={{ fontSize: 12 }}>{nights} đêm · Tạm tính {formatPrice(room.pricePerNight * nights)}</Text>}
                  <Form.Item label="Số khách" style={{ marginTop: 8, marginBottom: 8 }}>
                    <InputNumber min={1} max={room.capacity} value={guests} onChange={(v) => setGuests(v || 1)} style={{ width: '100%' }} size="large" />
                  </Form.Item>
                  <Form.Item label="Mã voucher (tùy chọn)" style={{ marginBottom: 12 }}>
                    <Input size="large" placeholder="VD: SALE10" value={voucherCode} onChange={(e) => setVoucherCode(e.target.value)} />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" size="large" block loading={submitting} style={{ height: 48, borderRadius: 12, fontWeight: 700, fontSize: 16 }}>
                    Giữ chỗ 15 phút
                  </Button>
                  {selected.checkin && !selected.checkout && <Text type="secondary" style={{ fontSize: 12, display: 'block', textAlign: 'center', marginTop: 8 }}>Đã chọn nhận phòng {formatDate(selected.checkin)} — chọn tiếp ngày trả phòng trên lịch</Text>}
                </Form>
              )}
              <Divider style={{ margin: '16px 0' }} />
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Text strong style={{ fontSize: 13 }}>Chính sách</Text>
                <Text type="secondary" style={{ fontSize: 12 }}>· Giữ chỗ 15 phút, quá hạn tự động hủy.<br />· Hủy miễn phí theo chính sách của phòng.</Text>
              </Space>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}
