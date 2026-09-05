import { Card, Button, Space, Typography } from 'antd'
import { StarFilled, TeamOutlined, EnvironmentOutlined, HeartOutlined, HeartFilled } from '@ant-design/icons'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { formatPrice, roomTypeLabel } from '../../utils/format'

// RoomCard — thẻ phòng dùng chung cho trang Home và danh sách gợi ý
export const MOCK_ROOM_IMAGES = [
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
  'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&q=80',
  'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&q=80',
  'https://images.unsplash.com/photo-1551882547-b79c4176354d?w=800&q=80',
]

export default function RoomCard({ room, index = 0 }) {
  const [liked, setLiked] = useState(false)
  const navigate = useNavigate()
  const img = room.imageUrl || MOCK_ROOM_IMAGES[index % MOCK_ROOM_IMAGES.length]
  const rating = 4.3 + ((index * 7) % 5) / 10
  const reviews = (index + 12) * 3

  return (
    <Card
      hoverable
      onClick={() => navigate(`/rooms/${room.id}`)}
      style={{ borderRadius: 16, overflow: 'hidden', border: '1px solid #eef0f5', height: '100%' }}
      bodyStyle={{ padding: 14 }}
      cover={
        <div style={{ position: 'relative', height: 180, overflow: 'hidden', background: '#f0f2f5' }}>
          <img src={img} alt={room.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} loading="lazy" />
          <div style={{ position: 'absolute', top: 10, left: 10, display: 'flex', gap: 6 }}>
            <span
              style={{
                background: room.status === 'AVAILABLE' ? '#00b578' : '#ff9f43',
                color: '#fff',
                padding: '2px 10px',
                borderRadius: 999,
                fontSize: 11,
                fontWeight: 600,
              }}
            >
              {room.status === 'AVAILABLE' ? 'Còn phòng' : 'Hết phòng'}
            </span>
            <span style={{ background: 'rgba(0,0,0,0.55)', color: '#fff', padding: '2px 10px', borderRadius: 999, fontSize: 11, fontWeight: 600 }}>
              {roomTypeLabel(room.type)}
            </span>
          </div>
          <Button
            size="small"
            shape="circle"
            icon={liked ? <HeartFilled style={{ color: '#ff4d4f' }} /> : <HeartOutlined />}
            onClick={(e) => {
              e.stopPropagation()
              setLiked(!liked)
            }}
            style={{ position: 'absolute', top: 10, right: 10, background: 'rgba(255,255,255,0.95)' }}
          />
          <div
            style={{
              position: 'absolute',
              bottom: 8,
              left: 10,
              background: 'rgba(0,0,0,0.55)',
              color: '#fff',
              padding: '2px 8px',
              borderRadius: 999,
              fontSize: 12,
              display: 'flex',
              alignItems: 'center',
              gap: 4,
            }}
          >
            <StarFilled style={{ color: '#ffd666', fontSize: 11 }} /> {rating.toFixed(1)} · {reviews} đánh giá
          </div>
        </div>
      }
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 6 }}>
        <Typography.Text strong style={{ fontSize: 15, lineHeight: 1.3, flex: 1 }} ellipsis>
          {room.name}
        </Typography.Text>
        <Typography.Text type="secondary" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>
          {room.code}
        </Typography.Text>
      </div>
      <Space size={6} style={{ color: '#888', fontSize: 12, marginBottom: 8 }}>
        <EnvironmentOutlined /> Quận 1, TP.HCM · <TeamOutlined /> {room.capacity} khách
      </Space>
      {room.description && (
        <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 10 }} ellipsis>
          {room.description}
        </Typography.Text>
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div>
          <Typography.Text strong style={{ fontSize: 16, color: '#ff3b30' }}>
            {formatPrice(room.pricePerNight)}
          </Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {' '}
            / đêm
          </Typography.Text>
        </div>
        <Button type="primary" size="small" style={{ borderRadius: 999, fontWeight: 600 }} onClick={(e) => { e.stopPropagation(); navigate(`/rooms/${room.id}`) }}>
          Đặt ngay
        </Button>
      </div>
    </Card>
  )
}
