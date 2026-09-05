import { Card, Row, Col, Input, Select, Button, Space, Tag, Typography } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'

// ============================================================
// FilterBar — bo loc/search chung cho moi man hinh admin.
// filters: [{ key, placeholder, type:'select', options:[{value,label}], value, onChange, allLabel }]
// q/setQ: ô tìm kiếm chính (tương ứng tham số ?q= của BE)
// ============================================================
export default function FilterBar({ filters = [], q, setQ, onSearch, onReset, totalText, quickTags = [], onQuickTag, extra }) {
  const { Text } = Typography
  return (
    <Card size="small" style={{ borderRadius: 12, marginBottom: 16 }} bodyStyle={{ padding: 12 }}>
      <Row gutter={[8, 8]} align="middle">
        {q !== undefined && (
          <Col xs={24} md={8}>
            <Input
              prefix={<SearchOutlined />}
              placeholder="Tìm kiếm (q)"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onPressEnter={onSearch}
              allowClear
            />
          </Col>
        )}
        {filters.map((f) => (
          <Col key={f.key} xs={12} md={5}>
            <Select
              placeholder={f.placeholder}
              value={f.value ?? null}
              onChange={(v) => f.onChange(v || null)}
              options={[{ value: null, label: f.allLabel || 'Tất cả' }, ...(f.options || [])]}
              style={{ width: '100%' }}
              allowClear
            />
          </Col>
        ))}
        <Col xs={24} md={filters.length > 0 ? 6 : 8} style={{ display: 'flex', gap: 8 }}>
          <Button type="primary" icon={<SearchOutlined />} onClick={onSearch} style={{ flex: 1, fontWeight: 600 }}>
            Tìm
          </Button>
          <Button icon={<ReloadOutlined />} onClick={onReset} />
          {extra}
        </Col>
      </Row>
      {(quickTags.length > 0 || totalText) && (
        <div style={{ marginTop: 8, display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
          {quickTags.length > 0 && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              Gợi ý:
            </Text>
          )}
          {quickTags.map((k) => (
            <Tag key={k} style={{ cursor: 'pointer', borderRadius: 999 }} onClick={() => onQuickTag?.(k)}>
              {k}
            </Tag>
          ))}
          {totalText && (
            <Text type="secondary" style={{ fontSize: 12, marginLeft: 'auto' }}>
              {totalText}
            </Text>
          )}
        </div>
      )}
    </Card>
  )
}
