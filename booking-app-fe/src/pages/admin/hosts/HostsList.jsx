import { useEffect, useState, useCallback } from 'react'
import { Button, Tag, Modal, Form, Input, Select, message, Popconfirm, Space, Typography, Switch, Avatar } from 'antd'
import { CheckOutlined, CloseOutlined, CrownOutlined, DeleteOutlined, EditOutlined, PauseOutlined } from '@ant-design/icons'
import PageHeader from '../../../components/common/PageHeader'
import FilterBar from '../../../components/common/FilterBar'
import DataTable from '../../../components/common/DataTable'
import { hostApi } from '../../../api/vouchers'
import { HOST_STATUS } from '../../../utils/constants'
import { hostStatusColor, hostStatusLabel } from '../../../utils/format'

export default function AdminHosts() {
  const [data, setData] = useState({ content: [], page: 0, size: 10, totalElements: 0 })
  const [loading, setLoading] = useState(false)
  const [q, setQ] = useState('')
  const [status, setStatus] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(
    (page = 0) => {
      setLoading(true)
      hostApi
        .list({ status: status || undefined, q: q || undefined, page, size: 10 })
        .then(setData)
        .catch((e) => message.error(e.message))
        .finally(() => setLoading(false))
    },
    [q, status],
  )

  useEffect(() => { load(0) }, [])

  const changeStatus = async (host, hostStatus, msg) => {
    try {
      await hostApi.update(host.id, { hostStatus })
      message.success(msg)
      load(data.page)
    } catch (e) { message.error(e.message) }
  }

  const openEdit = (r) => { setEditing(r); form.setFieldsValue(r); setModalOpen(true) }
  const onSubmit = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      await hostApi.update(editing.id, values)
      message.success('Cập nhật hồ sơ chủ nhà thành công')
      setModalOpen(false)
      load(data.page)
    } catch (e) { message.error(e.message) }
    finally { setSaving(false) }
  }

  const onDelete = async (id) => {
    try { await hostApi.remove(id); message.success('Đã vô hiệu hóa hồ sơ'); load(data.page) }
    catch (e) { message.error(e.message) }
  }

  const columns = [
    {
      title: 'Chủ nhà', render: (_, r) => (
        <Space>
          <Avatar style={{ background: '#722ed1' }} icon={<CrownOutlined />} />
          <div>
            <div style={{ fontWeight: 600 }}>{r.displayName || r.username}</div>
            <Typography.Text type="secondary" style={{ fontSize: 11 }}>{r.businessName || 'Cá nhân'}</Typography.Text>
          </div>
        </Space>
      ),
    },
    { title: 'Username', dataIndex: 'username', width: 140 },
    { title: 'Bio', dataIndex: 'bio', ellipsis: true },
    {
      title: 'Trạng thái', dataIndex: 'hostStatus', width: 130,
      render: (s) => <Tag color={hostStatusColor[s]} style={{ borderRadius: 999 }}>{hostStatusLabel(s)}</Tag>,
    },
    {
      title: 'Thao tác', width: 230,
      render: (_, r) => (
        <Space size={4} wrap>
          {r.hostStatus === 'PENDING' && (
            <>
              <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => changeStatus(r, 'APPROVED', 'Đã duyệt — BE gán role HOST')}>Duyệt</Button>
              <Button size="small" danger icon={<CloseOutlined />} onClick={() => changeStatus(r, 'REJECTED', 'Đã từ chối')}>Từ chối</Button>
            </>
          )}
          {r.hostStatus === 'APPROVED' && (
            <Button size="small" icon={<PauseOutlined />} onClick={() => changeStatus(r, 'SUSPENDED', 'Đã tạm khóa')}>Khóa</Button>
          )}
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)} />
          <Popconfirm title="Vô hiệu hóa hồ sơ này?" onConfirm={() => onDelete(r.id)}><Button size="small" danger icon={<DeleteOutlined />} /></Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title={<span><CrownOutlined /> Hồ sơ chủ nhà</span>} description="Duyệt hồ sơ đối tác — phê duyệt hồ sơ đạt yêu cầu hoặc tạm khóa khi cần rà soát" />
      <FilterBar
        q={q} setQ={setQ} onSearch={() => load(0)} onReset={() => { setQ(''); setStatus(null); setTimeout(() => load(0), 0) }}
        filters={[{ key: 'status', placeholder: 'Trạng thái', options: HOST_STATUS.map((s) => ({ value: s, label: hostStatusLabel(s) })), value: status, onChange: setStatus }]}
        totalText={`Tổng ${data.totalElements} hồ sơ · Trang ${data.page + 1}`}
      />
      <DataTable columns={columns} dataSource={data.content} loading={loading} page={data.page} size={data.size} totalElements={data.totalElements} onPageChange={load} emptyText="Chưa có hồ sơ chủ nhà nào" />
      <Modal title={`Sửa hồ sơ: ${editing?.displayName || ''}`} open={modalOpen} onOk={onSubmit} onCancel={() => setModalOpen(false)} confirmLoading={saving} destroyOnClose>
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item name="displayName" label="Tên hiển thị" rules={[{ required: true, message: 'Nhập tên hiển thị' }]}><Input /></Form.Item>
          <Form.Item name="businessName" label="Tên doanh nghiệp"><Input /></Form.Item>
          <Form.Item name="bio" label="Giới thiệu"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item name="autoBookingDefault" label="Tự động nhận booking" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="active" label="Kích hoạt" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
