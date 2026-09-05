import { Typography, Space } from 'antd'

const { Title, Text } = Typography

// PageHeader — tieu de man hinh + mo ta + cum nut hanh dong ben phai (them moi, xuat excel...).
export default function PageHeader({ title, description, extra }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap', marginBottom: 16, alignItems: 'flex-start' }}>
      <div>
        <Title level={4} style={{ margin: 0, color: '#1a1a2e' }}>
          {title}
        </Title>
        {description && (
          <Text type="secondary" style={{ fontSize: 13 }}>
            {description}
          </Text>
        )}
      </div>
      <Space wrap>{extra}</Space>
    </div>
  )
}
