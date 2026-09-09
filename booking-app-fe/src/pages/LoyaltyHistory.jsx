import { useEffect, useState, useCallback } from 'react'
import { Card, Tag, Skeleton, Typography, Space, Divider, message } from 'antd'
import { CrownOutlined, TrophyOutlined, HistoryOutlined } from '@ant-design/icons'
import PageHeader from '../components/common/PageHeader'
import DataTable from '../components/common/DataTable'
import { loyaltyApi } from '../api/loyalty'
import { formatDateTime } from '../utils/format'

const { Text, Title } = Typography

const RANK_COLOR = { MEMBER: 'default', SILVER: '#8c8c8c', GOLD: 'gold', PLATINUM: '#597ef7', DIAMOND: '#722ed1' }

function RankGrid({ ranks }) {
  if (!ranks || ranks.length === 0) return <Text type="secondary">Chưa có hạng nào.</Text>
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 12, marginBottom: 16 }}>
      {ranks.map((r) => (
        <Card key={r.name} size="small" style={{ borderRadius: 12, borderColor: RANK_COLOR[r.name] || '#f0f0f0' }}>
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <Tag color={r.name === 'MEMBER' ? 'default' : 'gold'} style={{ borderRadius: 999 }}>{r.name}</Tag>
            <Text strong style={{ fontSize: 13 }}>Từ {r.minPoints} điểm</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>Giảm {r.discountPercent ?? 0}%</Text>
            {r.benefits && <Text type="secondary" style={{ fontSize: 11 }}>{JSON.stringify(r.benefits)}</Text>}
          </Space>
        </Card>
      ))}
    </div>
  )
}

export default function LoyaltyHistory() {
  const [profile, setProfile] = useState(null)
  const [ranks, setRanks] = useState([])
  const [history, setHistory] = useState({ content: [], page: 0, size: 20, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [historyLoading, setHistoryLoading] = useState(false)

  const load = useCallback((page = 0) => {
    setHistoryLoading(true)
    Promise.allSettled([loyaltyApi.myProfile(), loyaltyApi.myHistory({ page, size: 20 })]).then(([a, b]) => {
      if (a.status === 'fulfilled') setProfile(a.value)
      if (b.status === 'fulfilled') setHistory(b.value)
      else if (b.status === 'rejected') message.error(b.reason?.message)
    }).finally(() => setHistoryLoading(false))
  }, [])

  useEffect(() => {
    setLoading(true)
    Promise.allSettled([loyaltyApi.listRanks({ page: 0, size: 20 }), loyaltyApi.myProfile()]).then(([a, b]) => {
      if (a.status === 'fulfilled') {
        const data = a.value
        setRanks(Array.isArray(data) ? data : (data?.content || []))
      }
      if (b.status === 'fulfilled') setProfile(b.value)
    }).finally(() => {
      setLoading(false)
      load(0)
    })
  }, [load])

  const columns = [
    { title: 'Thời gian', dataIndex: 'createdAt', width: 160, render: formatDateTime },
    {
      title: 'Điểm', dataIndex: 'pointsChange', width: 100,
      render: (v) => <Tag color={v > 0 ? 'green' : 'red'} style={{ borderRadius: 999 }}>{v > 0 ? `+${v}` : v}</Tag>,
    },
    { title: 'Lý do', dataIndex: 'reason', render: (v) => v || '-' },
    { title: 'Mã đặt phòng', dataIndex: 'bookingCode', width: 140, render: (v) => v || '-' },
  ]

  if (loading) return <div style={{ maxWidth: 1100, margin: '0 auto', padding: 24 }}><Card><Skeleton active /></Card></div>

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '16px 24px 32px' }}>
      <PageHeader title={<><TrophyOutlined /> Hạng & lịch sử điểm</>} description="Các hạng thành viên và lịch sử tích/trừ điểm của bạn." />

      {profile && (
        <Card size="small" style={{ borderRadius: 12, marginBottom: 16, background: '#fafcff' }}>
          <Space size={12} wrap>
            <CrownOutlined style={{ fontSize: 18, color: '#faad14' }} />
            <Text>
              Tổng điểm: <Text strong style={{ color: '#faad14' }}>{profile.totalPoints}</Text>
            </Text>
            {profile.currentRank && <Tag color="gold" style={{ borderRadius: 999 }}>{profile.currentRank.name}</Tag>}
            {profile.nextRank ? (
              <Text type="secondary" style={{ fontSize: 13 }}>
                Còn <Text strong>{profile.pointsToNextRank}</Text> điểm để lên <Text strong>{profile.nextRank.name}</Text>
              </Text>
            ) : profile.currentRank ? (
              <Text type="secondary">Đã đạt hạng cao nhất</Text>
            ) : null}
          </Space>
        </Card>
      )}

      <Title level={5} style={{ marginBottom: 8 }}><CrownOutlined /> Các hạng & quyền lợi</Title>
      <RankGrid ranks={ranks} />

      <Divider style={{ margin: '16px 0' }} />

      <Title level={5} style={{ marginBottom: 8 }}><HistoryOutlined /> Lịch sử điểm</Title>
      <DataTable
        columns={columns}
        dataSource={history.content}
        loading={historyLoading}
        page={history.page}
        size={history.size}
        totalElements={history.totalElements}
        onPageChange={load}
        rowKey="id"
        emptyText="Chưa có lịch sử điểm"
      />
    </div>
  )
}
