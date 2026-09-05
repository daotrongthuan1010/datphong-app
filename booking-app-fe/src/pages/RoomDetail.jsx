import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Modal, Form, Input, DatePicker, InputNumber, message, Tag, Typography, Space, Skeleton, Divider, Descriptions, Card, Carousel, Rate, Upload, Select, Empty, Pagination, Avatar } from 'antd'
import { CalendarOutlined, TeamOutlined, StarFilled, ArrowLeftOutlined, EnvironmentOutlined, LeftOutlined, RightOutlined, PictureOutlined, VideoCameraOutlined, CameraOutlined } from '@ant-design/icons'
import { useSelector } from 'react-redux'
import dayjs from 'dayjs'
import { roomApi } from '../api/rooms'
import { bookingApi } from '../api/bookings'
import { reviewApi } from '../api/reviews'
import { formatPrice, formatDateTime, formatDate, roomTypeLabel, roomStatusLabel, roomStatusColor } from '../utils/format'
import { MOCK_ROOM_IMAGES } from '../components/common/RoomCard'

const { Title, Text, Paragraph } = Typography

// ---- Nút prev/next tùy chỉnh cho Carousel ----
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

// ---- Thư viện ảnh+video: carousel có mũi tên + dải thumbnail (video phát được) ----
function Gallery({ media, name }) {
  const ref = useRef(null)
  const [active, setActive] = useState(0)

  return (
    <div>
      <div style={{ position: 'relative', borderRadius: 16, overflow: 'hidden', background: '#000' }}>
        <Carousel
          ref={(r) => {
            ref.current = r
          }}
          afterChange={(i) => setActive(i)}
          dots={{ className: 'vivu-gallery-dots' }}
          draggable
          infinite
          speed={350}
        >
          {media.map((m, i) =>
            m.type === 'VIDEO' ? (
              <div key={i}>
                <video src={m.url} controls preload="metadata" style={{ width: '100%', height: 420, display: 'block', background: '#000' }} />
              </div>
            ) : (
              <div key={i}>
                <img src={m.url} alt={`${name} ${i + 1}`} style={{ width: '100%', height: 420, objectFit: 'cover', display: 'block' }} />
              </div>
            ),
          )}
        </Carousel>
        {media.length > 1 && (
          <>
            <ArrowBtn dir="prev" onClick={() => ref.current?.prev()} />
            <ArrowBtn dir="next" onClick={() => ref.current?.next()} />
            <span
              style={{
                position: 'absolute',
                bottom: 12,
                right: 14,
                zIndex: 5,
                background: 'rgba(0,0,0,0.55)',
                color: '#fff',
                padding: '3px 10px',
                borderRadius: 999,
                fontSize: 12,
              }}
            >
              <PictureOutlined /> {active + 1} / {media.length}
            </span>
          </>
        )}
      </div>
      {media.length > 1 && (
        <div style={{ display: 'flex', gap: 8, marginTop: 8, overflowX: 'auto', paddingBottom: 4 }}>
          {media.map((m, i) =>
            m.type === 'VIDEO' ? (
              <div
                key={i}
                onClick={() => {
                  ref.current?.goTo(i)
                  setActive(i)
                }}
                style={{
                  width: 84,
                  height: 60,
                  borderRadius: 8,
                  cursor: 'pointer',
                  flexShrink: 0,
                  background: '#111',
                  display: 'grid',
                  placeItems: 'center',
                  color: '#fff',
                  fontSize: 18,
                  border: active === i ? '2px solid #1968f5' : '2px solid transparent',
                  opacity: active === i ? 1 : 0.75,
                }}
              >
                <VideoCameraOutlined />
              </div>
            ) : (
              <img
                key={i}
                src={m.url}
                alt=""
                onClick={() => {
                  ref.current?.goTo(i)
                  setActive(i)
                }}
                style={{
                  width: 84,
                  height: 60,
                  objectFit: 'cover',
                  borderRadius: 8,
                  cursor: 'pointer',
                  flexShrink: 0,
                  border: active === i ? '2px solid #1968f5' : '2px solid transparent',
                  opacity: active === i ? 1 : 0.75,
                }}
              />
            ),
          )}
        </div>
      )}
    </div>
  )
}

// ---- Một review: sao + bình luận + media (ảnh xem được, video phát được) ----
function ReviewItem({ r }) {
  return (
    <div style={{ padding: '14px 0', borderBottom: '1px solid #f0f0f0' }}>
      <Space align="start" size={12}>
        <Avatar style={{ background: '#1968f5', flexShrink: 0 }}>{(r.userFullName || '?')[0]?.toUpperCase()}</Avatar>
        <div style={{ minWidth: 0 }}>
          <Space size={8} wrap>
            <Text strong>{r.userFullName}</Text>
            <Rate disabled value={r.rating} style={{ fontSize: 13 }} />
          </Space>
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {formatDateTime(r.createdAt)}
            </Text>
          </div>
          {r.comment && (
            <Paragraph style={{ margin: '6px 0 0', fontSize: 14, whiteSpace: 'pre-wrap' }}>{r.comment}</Paragraph>
          )}
          {r.media && r.media.length > 0 && (
            <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
              {r.media.map((m) =>
                m.mediaType === 'VIDEO' ? (
                  <video key={m.url} src={m.url} controls preload="metadata" style={{ width: 220, borderRadius: 10, background: '#000' }} />
                ) : (
                  <img
                    key={m.url}
                    src={m.url}
                    alt=""
                    onClick={() => window.open(m.url, '_blank')}
                    style={{ width: 90, height: 90, objectFit: 'cover', borderRadius: 10, cursor: 'zoom-in' }}
                  />
                ),
              )}
            </div>
          )}
        </div>
      </Space>
    </div>
  )
}

// ---- Khối đánh giá: tóm tắt điểm + danh sách + form gửi (kèm video review) ----
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
    reviewApi
      .listByRoom(room.id, { page: p - 1, size: 5 })
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load(1)
  }, [room.id])

  const openForm = () => {
    setFormOpen(true)
    if (isAuthenticated && myBookings.length === 0) {
      // Lấy đặt phòng của chính user cho phòng này (đã xác nhận/hoàn thành, chưa đánh giá)
      bookingApi
        .myList({ page: 0, size: 50 })
        .then((res) => {
          const eligible = (res.content || []).filter(
            (b) => b.roomId === room.id && (b.status === 'CONFIRMED' || b.status === 'COMPLETED'),
          )
          setMyBookings(eligible)
        })
        .catch(() => {})
    }
  }

  const submitReview = async () => {
    const values = await form.validateFields()
    const files = (values.media || []).map((f) => f.originFileObj).filter(Boolean)
    setSubmitting(true)
    try {
      await reviewApi.create({ bookingId: values.bookingId, rating: values.rating, comment: values.comment || undefined }, files)
      message.success('Cảm ơn bạn đã đánh giá!')
      setFormOpen(false)
      form.resetFields()
      load(1)
    } catch (e) {
      message.error(e.message)
    } finally {
      setSubmitting(false)
    }
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
            <Title level={5} style={{ margin: 0 }}>
              Đánh giá từ khách đã ở
            </Title>
            <Text type="secondary" style={{ fontSize: 13 }}>
              {data.totalElements} đánh giá · chỉ khách có đặt phòng hợp lệ mới gửi được
            </Text>
          </div>
        </Space>
        {isAuthenticated && (
          <Button type="primary" icon={<CameraOutlined />} onClick={openForm}>
            Viết đánh giá
          </Button>
        )}
      </div>

      <Modal
        title={`Đánh giá phòng ${room.name}`}
        open={formOpen}
        onCancel={() => setFormOpen(false)}
        onOk={submitReview}
        confirmLoading={submitting}
        okText="Gửi đánh giá"
        cancelText="Hủy"
        destroyOnClose
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item name="bookingId" label="Chọn đặt phòng" rules={[{ required: true, message: 'Chọn đặt phòng để đánh giá' }]}>
            <Select
              placeholder={myBookings.length === 0 ? 'Không tìm thấy đặt phòng hợp lệ cho phòng này' : 'Chọn mã đặt phòng'}
              options={myBookings.map((b) => ({
                value: b.id,
                label: `${b.bookingCode} · ${formatDate(b.checkinDate)} → ${formatDate(b.checkoutDate)}`,
              }))}
            />
          </Form.Item>
          <Form.Item name="rating" label="Chấm điểm" rules={[{ required: true, message: 'Chọn số sao' }]}>
            <Rate />
          </Form.Item>
          <Form.Item name="comment" label="Cảm nhận của bạn" rules={[{ max: 1000, message: 'Tối đa 1000 ký tự' }]}>
            <Input.TextArea rows={3} placeholder="Phòng sạch sẽ, nhân viên thân thiện..." />
          </Form.Item>
          <Form.Item
            name="media"
            label="Thêm ảnh / video (tùy chọn)"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
            extra="Hỗ trợ cả video để review chi tiết phòng — video sẽ phát ngay trong phần đánh giá"
          >
            <Upload listType="picture-card" accept="image/*,video/*" multiple beforeUpload={() => false}>
              <div>
                <VideoCameraOutlined />
                <div style={{ fontSize: 12 }}>Tải lên</div>
              </div>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      {loading && data.content.length === 0 ? (
        <Skeleton active style={{ marginTop: 16 }} />
      ) : data.content.length === 0 ? (
        <Empty style={{ marginTop: 20 }} description="Chưa có đánh giá nào — hãy là người đầu tiên sau khi đặt phòng" />
      ) : (
        <>
          {data.content.map((r) => (
            <ReviewItem key={r.id} r={r} />
          ))}
          {data.totalElements > 5 && (
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: 12 }}>
              <Pagination current={page} pageSize={5} total={data.totalElements} showSizeChanger={false} onChange={(p) => { setPage(p); load(p) }} />
            </div>
          )}
        </>
      )}
    </Card>
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
  const rangeWatch = Form.useWatch('range', form)

  useEffect(() => {
    setLoading(true)
    roomApi
      .get(id)
      .then(setRoom)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  const nights = Array.isArray(rangeWatch) && rangeWatch[0] && rangeWatch[1] ? Math.max(0, rangeWatch[1].diff(rangeWatch[0], 'day')) : 0

  const submitBooking = async () => {
    const values = await form.validateFields()
    const [checkin, checkout] = values.range
    if (!checkin || !checkout || checkout.diff(checkin, 'day') < 1) {
      message.error('Ngày trả phòng phải sau ngày nhận phòng')
      return
    }
    setSubmitting(true)
    try {
      const created = await bookingApi.create({
        roomId: room.id,
        checkinDate: checkin.format('YYYY-MM-DD'),
        checkoutDate: checkout.format('YYYY-MM-DD'),
        guestsCount: values.guests,
        voucherCode: values.voucherCode?.trim() || undefined,
      })
      Modal.success({
        title: 'Đặt phòng thành công',
        okText: 'Xem đặt phòng của tôi',
        cancelText: 'Tiếp tục khám phá',
        closable: true,
        onOk: () => navigate('/bookings'),
        content: (
          <div>
            <p>
              Mã đặt phòng: <Text strong>{created.bookingCode}</Text>
            </p>
            <p>
              {room.name} · {formatPrice(Number(created.totalPrice))} / {nights} đêm
            </p>
            <p style={{ marginBottom: 0 }}>Trạng thái: Đã xác nhận — miễn phí hủy trước 24h.</p>
          </div>
        ),
      })
    } catch (e) {
      message.error(e.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div style={{ maxWidth: 1100, margin: '0 auto', padding: 24 }}>
        <Skeleton active paragraph={{ rows: 6 }} />
      </div>
    )
  }
  if (error || !room) {
    return (
      <div style={{ maxWidth: 1100, margin: '0 auto', padding: 24 }}>
        <Card>
          <Text type="danger">Lỗi: {error || 'Không tìm thấy phòng'}</Text>
          <Button style={{ marginLeft: 12 }} onClick={() => navigate(-1)}>
            Quay lại
          </Button>
        </Card>
      </div>
    )
  }

  // Media thật từ BE (MinIO): ảnh (images) + video (videos) — nếu chưa có thì dùng demo
  const imageUrls = room.images && room.images.length > 0 ? room.images : []
  const videoUrls = room.videos && room.videos.length > 0 ? room.videos : []
  const hero = room.imageUrl || imageUrls[0] || MOCK_ROOM_IMAGES[Number(id) % MOCK_ROOM_IMAGES.length]
  const galleryMedia =
    imageUrls.length > 0 || videoUrls.length > 0
      ? [...imageUrls.map((u) => ({ url: u, type: 'IMAGE' })), ...videoUrls.map((u) => ({ url: u, type: 'VIDEO' }))]
      : [hero, ...MOCK_ROOM_IMAGES.filter((u) => u !== hero)].slice(0, 4).map((u) => ({ url: u, type: 'IMAGE' }))

  return (
    <div style={{ background: '#f5f7fb', minHeight: 'calc(100vh - 64px)' }}>
      <div style={{ maxWidth: 1100, margin: '0 auto', padding: '16px 24px 32px' }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} style={{ marginBottom: 12, paddingLeft: 0 }}>
          Quay lại danh sách
        </Button>

        {/* Thư viện ảnh+video: carousel có nút prev/next + thumbnail (video phát được trong Gallery) */}
        <Gallery media={galleryMedia} name={room.name} />

        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 0.9fr', gap: 16, marginTop: 16, alignItems: 'start' }}>
          <div>
            <Card style={{ borderRadius: 16 }} bodyStyle={{ padding: 20 }}>
              <Space size={8} wrap style={{ marginBottom: 8 }}>
                <Tag color={roomStatusColor[room.status] || 'default'} style={{ borderRadius: 999, margin: 0 }}>
                  {roomStatusLabel(room.status)}
                </Tag>
                <Tag color="blue" style={{ borderRadius: 999, margin: 0 }}>
                  {roomTypeLabel(room.type)}
                </Tag>
                <Tag style={{ borderRadius: 999, margin: 0 }}>{room.code}</Tag>
              </Space>
              <Title level={3} style={{ margin: 0, marginBottom: 6 }}>
                {room.name}
              </Title>
              <Space size={12} style={{ color: '#666', fontSize: 13, flexWrap: 'wrap' }}>
                <span>
                  <EnvironmentOutlined /> Quận 1, TP.HCM
                </span>
                <span>
                  <TeamOutlined /> Tối đa {room.capacity} khách
                </span>
              </Space>
              {room.description && (
                <>
                  <Divider style={{ margin: '16px 0' }} />
                  <Text style={{ fontSize: 14, lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>{room.description}</Text>
                </>
              )}
              <Divider style={{ margin: '16px 0' }} />
              <Title level={5} style={{ marginBottom: 8 }}>
                Tiện nghi
              </Title>
              <Space size={[8, 8]} wrap>
                {['WiFi miễn phí', 'Điều hòa', 'Bữa sáng', 'Hồ bơi', 'Đưa đón sân bay', 'Lễ tân 24h'].map((a) => (
                  <Tag key={a} style={{ borderRadius: 999, padding: '4px 12px' }}>
                    {a}
                  </Tag>
                ))}
              </Space>
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

            {/* Đánh giá — có video review */}
            <ReviewsSection room={room} />
          </div>

          {/* Đặt phòng */}
          <div style={{ position: 'sticky', top: 80 }}>
            <Card style={{ borderRadius: 16, boxShadow: '0 12px 32px rgba(0,0,0,0.08)' }} bodyStyle={{ padding: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
                <Text strong style={{ fontSize: 22, color: '#ff3b30' }}>
                  {formatPrice(room.pricePerNight)}
                </Text>
                <Text type="secondary">/ đêm</Text>
              </div>
              <Text type="secondary" style={{ fontSize: 12 }}>
                Đã bao gồm thuế và phí · Miễn phí hủy trước 24h
              </Text>
              <Divider style={{ margin: '16px 0' }} />
              {!isAuthenticated ? (
                <Space direction="vertical" style={{ width: '100%' }} size={10}>
                  <Text type="secondary">Đăng nhập để chọn ngày và đặt phòng ngay.</Text>
                  <Button type="primary" size="large" block onClick={() => navigate('/login')} style={{ height: 48, borderRadius: 12, fontWeight: 700 }}>
                    Đăng nhập để đặt
                  </Button>
                </Space>
              ) : (
                <Form form={form} layout="vertical" requiredMark={false} onFinish={submitBooking}>
                  <Form.Item
                    name="range"
                    label="Ngày nhận · trả phòng"
                    rules={[{ required: true, message: 'Chọn khoảng ngày' }]}
                    extra={nights > 0 ? `${nights} đêm · Tạm tính ${formatPrice(room.pricePerNight * nights)}` : 'Tối thiểu 1 đêm'}
                  >
                    <DatePicker.RangePicker size="large" style={{ width: '100%' }} minDate={dayjs()} format="DD/MM/YYYY" />
                  </Form.Item>
                  <Form.Item name="guests" label="Số khách" rules={[{ required: true }]} initialValue={Math.min(2, room.capacity)}>
                    <InputNumber size="large" min={1} max={room.capacity} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="voucherCode" label="Mã voucher (tùy chọn)">
                    <Input size="large" placeholder="VD: SALE10" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" size="large" block loading={submitting} style={{ height: 48, borderRadius: 12, fontWeight: 700, fontSize: 16 }}>
                    Đặt ngay
                  </Button>
                </Form>
              )}
              <Divider style={{ margin: '16px 0' }} />
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Text strong style={{ fontSize: 13 }}>
                  Chính sách
                </Text>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  · Miễn phí hủy trước 24h
                  <br />· Thanh toán tại chỗ hoặc online (VNPay/MoMo sắp hỗ trợ)
                  <br />· Xuất hóa đơn VAT
                </Text>
              </Space>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}
