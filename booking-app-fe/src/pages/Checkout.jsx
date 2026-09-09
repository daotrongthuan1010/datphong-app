import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { Alert, Button, Card, Descriptions, Divider, Radio, Result, Space, Spin, Tag, Typography } from 'antd'
import { CalendarOutlined, ClockCircleOutlined, CreditCardOutlined, TeamOutlined, WalletOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import PageHeader from '../components/common/PageHeader'
import { bookingApi } from '../api/bookings'
import { paymentApi } from '../api/payments'
import { formatPrice, formatDate, formatDateTime, bookingStatusLabel, bookingStatusColor, countdownFrom } from '../utils/format'

const { Text, Title } = Typography

// Trang thanh toan cho 1 booking HOLD — buoc tiep theo cua "Giu cho" trong RoomDetail.
export default function Checkout() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [booking, setBooking] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [method, setMethod] = useState('CREDIT_CARD')
  const [paying, setPaying] = useState(false)
  const [nowMs, setNowMs] = useState(Date.now())
  const [payment, setPayment] = useState(null)
  const [polling, setPolling] = useState(false)
  const pollRef = useRef(null)

  const returnedFromGateway = searchParams.get('paid') === '1'

  const loadBooking = useCallback(() => {
    return bookingApi
      .get(id)
      .then((b) => {
        setBooking(b)
        return b
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    loadBooking()
  }, [loadBooking])

  useEffect(() => {
    if (!booking) return
    const s = booking.status
    if (s !== 'HOLD' && s !== 'PENDING_PAYMENT') return
    const t = setInterval(() => setNowMs(Date.now()), 1000)
    return () => clearInterval(t)
  }, [booking])

  const pollStatus = useCallback(() => {
    setPolling(true)
    let tries = 0
    const tick = async () => {
      tries += 1
      try {
        const p = await paymentApi.byBooking(id)
        setPayment(p)
        if (p.status === 'SUCCESS' || p.status === 'FAILED') {
          clearInterval(pollRef.current)
          setPolling(false)
          await loadBooking()
          return
        }
      } catch (e) {
        // chua co payment — thu tiep
      }
      if (tries >= 20) {
        clearInterval(pollRef.current)
        setPolling(false)
      }
    }
    pollRef.current = setInterval(tick, 1500)
    tick()
  }, [id, loadBooking])

  useEffect(() => {
    if (!returnedFromGateway) return
    let cancelled = false
    const run = async () => {
      // Doi soat chu dong truoc khi poll: BE hoi cong trang thai that cua giao dich.
      // Can thiet vi webhook cua cong co the khong goi duoc ve BE local (khac mang),
      // neu khong co buoc nay thi tien da tra tren cong ma booking van treo PENDING.
      try { await paymentApi.reconcile(id) } catch (e) { /* chua co payment / loi mang — poll se thu tiep */ }
      if (!cancelled) pollStatus()
    }
    run()
    return () => { cancelled = true; clearInterval(pollRef.current) }
  }, [returnedFromGateway, id, pollStatus])

  const onPay = async () => {
    setPaying(true)
    setError(null)
    try {
      const returnUrl = `${window.location.origin}/checkout/${id}?paid=1`
      const created = await paymentApi.create({ bookingId: Number(id), method, returnUrl })
      if (!created?.paymentUrl) {
        throw new Error('Cong thanh toan khong tra URL — thu lai sau')
      }
      // Ghi lai de khi quay ve co the doi chieu (khong bat buoc)
      try { sessionStorage.setItem(`vivu_payment_${id}`, created.paymentUrl) } catch {}
      window.location.href = created.paymentUrl
    } catch (e) {
      setError(e.message)
      setPaying(false)
    }
  }

  if (loading) {
    return (
      <div style={{ maxWidth: 900, margin: '0 auto', padding: '40px 24px', textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    )
  }

  if (error && !booking) {
    return (
      <div style={{ maxWidth: 900, margin: '0 auto', padding: '24px' }}>
        <Card style={{ borderRadius: 16 }}>
          <Result status="warning" title="Không tải được đặt phòng" subTitle={error} extra={<Button type="primary" onClick={() => navigate('/bookings')}>Về đặt phòng của tôi</Button>} />
        </Card>
      </div>
    )
  }

  const nights = dayjs(booking.checkoutDate).diff(dayjs(booking.checkinDate), 'day')
  const countdown = countdownFrom(booking.holdExpiresAt, nowMs)
  const holdExpired = !countdown && (booking.status === 'HOLD' || booking.status === 'PENDING_PAYMENT')
  const confirmed = booking.status === 'CONFIRMED' || booking.status === 'COMPLETED'
  const canPay = !confirmed && !holdExpired && booking.status !== 'CANCELLED' && booking.status !== 'EXPIRED' && booking.status !== 'REFUNDED'

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '16px 24px 40px' }}>
      <PageHeader title="Thanh toán đặt phòng" description={`Mã đặt phòng ${booking.bookingCode}`} />

      {payment?.status === 'SUCCESS' && (
        <Alert type="success" showIcon message="Thanh toán thành công" description={`Giao dịch ${payment.gatewayTransactionRef || ''} · ${formatDateTime(payment.paidAt)}`} style={{ marginBottom: 16, borderRadius: 12 }} />
      )}
      {payment?.status === 'FAILED' && (
        <Alert type="error" showIcon message="Thanh toán thất bại" description="Bạn có thể thử lại khi chỗ vẫn còn được giữ, hoặc hủy và giữ chỗ lại." style={{ marginBottom: 16, borderRadius: 12 }} />
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 16, alignItems: 'start' }}>
        <Card style={{ borderRadius: 16 }} bodyStyle={{ padding: 20 }}>
          <Title level={5} style={{ marginTop: 0 }}>Thông tin đặt phòng</Title>
          <Descriptions column={1} size="small" labelStyle={{ color: '#888', width: 140 }}>
            <Descriptions.Item label="Phòng">
              <Space size={8}>
                <Text strong>{booking.roomName}</Text>
                <Tag>{booking.roomCode}</Tag>
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Nhận / Trả phòng">
              <span><CalendarOutlined /> {formatDate(booking.checkinDate)} → {formatDate(booking.checkoutDate)}</span>
            </Descriptions.Item>
            <Descriptions.Item label="Số đêm">{nights > 0 ? `${nights} đêm` : '-'}</Descriptions.Item>
            <Descriptions.Item label="Số khách"><span><TeamOutlined /> {booking.guestsCount}</span></Descriptions.Item>
            <Descriptions.Item label="Voucher">{booking.voucherCode || 'Không dùng'}</Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color={bookingStatusColor[booking.status]} style={{ borderRadius: 999 }}>{bookingStatusLabel(booking.status)}</Tag>
            </Descriptions.Item>
          </Descriptions>
          <Divider style={{ margin: '16px 0' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <Text type="secondary">Tổng thanh toán</Text>
            <Text strong style={{ fontSize: 24, color: '#ff3b30' }}>{formatPrice(Number(booking.totalPrice))}</Text>
          </div>
        </Card>

        <Card style={{ borderRadius: 16, position: 'sticky', top: 80 }} bodyStyle={{ padding: 20 }}>
          {confirmed ? (
            <Result status="success" title="Đã xác nhận" subTitle="Đặt phòng của bạn đã được thanh toán và xác nhận." extra={<Button type="primary" onClick={() => navigate('/bookings')}>Xem đặt phòng</Button>} />
          ) : holdExpired ? (
            <Result status="warning" title="Giữ chỗ đã hết hạn" subTitle="Phòng đã được trả lại. Vui lòng giữ chỗ lại từ trang phòng." extra={<Button type="primary" onClick={() => navigate(`/rooms/${booking.roomId}`)}>Chọn lại ngày</Button>} />
          ) : (
            <>
              <Space direction="vertical" size={10} style={{ width: '100%' }}>
                <div>
                  <Text strong><ClockCircleOutlined /> Thời gian còn lại</Text>
                  <div style={{ fontSize: 22, fontWeight: 700, color: countdown ? '#fa8c16' : '#999', marginTop: 4 }}>
                    {countdown || 'Hết hạn'}
                  </div>
                </div>
                <Divider style={{ margin: '12px 0' }} />
                <div>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>Phương thức thanh toán</Text>
                  <Radio.Group value={method} onChange={(e) => setMethod(e.target.value)} style={{ width: '100%' }}>
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <Radio.Button value="CREDIT_CARD" style={{ width: '100%', height: 44, display: 'flex', alignItems: 'center', borderRadius: 10 }}>
                        <CreditCardOutlined /> Thẻ (fake-bank demo)
                      </Radio.Button>
                      <Radio.Button value="VNPAY" style={{ width: '100%', height: 44, display: 'flex', alignItems: 'center', borderRadius: 10 }}>
                        <WalletOutlined /> VNPay
                      </Radio.Button>
                    </Space>
                  </Radio.Group>
                  <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 8 }}>
                    Thẻ demo: 4242 4242 4242 4242 (thành công), 4000 0000 0000 0002 (thất bại) — hạn bất kỳ, CVV 3 số.
                  </Text>
                </div>
                {error && <Alert type="error" showIcon message={error} style={{ borderRadius: 10 }} />}
                {polling && !payment && <Alert type="info" showIcon message="Đang kiểm tra kết quả thanh toán..." style={{ borderRadius: 10 }} />}
                <Button type="primary" size="large" block loading={paying} disabled={!canPay} onClick={onPay} style={{ height: 48, borderRadius: 12, fontWeight: 700 }}>
                  Thanh toán {formatPrice(Number(booking.totalPrice))}
                </Button>
                <Button block style={{ borderRadius: 10 }} onClick={() => navigate('/bookings')}>Xem tất cả đặt phòng</Button>
              </Space>
            </>
          )}
        </Card>
      </div>
    </div>
  )
}
