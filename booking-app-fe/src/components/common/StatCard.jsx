import { Card, Statistic, Skeleton } from 'antd'

// StatCard — the so lieu cho Dashboard (so phong, user, voucher...).
export default function StatCard({ title, value, loading, icon, color = '#1968f5', suffix, precision }) {
  return (
    <Card style={{ borderRadius: 12 }} bodyStyle={{ padding: 16 }}>
      {loading ? (
        <Skeleton active paragraph={{ rows: 1 }} />
      ) : (
        <Statistic
          title={<span style={{ fontSize: 13 }}>{title}</span>}
          value={value}
          prefix={<span style={{ color, fontSize: 20 }}>{icon}</span>}
          suffix={suffix}
          precision={precision}
        />
      )}
    </Card>
  )
}
