import { useEffect, useState, useCallback } from 'react'
import { Button, Tag, Modal, Form, Input, Select, InputNumber, DatePicker, message, Popconfirm, Space, Typography } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, GiftOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import PageHeader from '../../../components/common/PageHeader'
import FilterBar from '../../../components/common/FilterBar'
import DataTable from '../../../components/common/DataTable'
import { voucherApi } from '../../../api/vouchers'
import { VOUCHER_OWNER_TYPES, DISCOUNT_TYPES } from '../../../utils/constants'
import { discountTypeLabel, discountTypeColor, formatDiscountValue, voucherOwnerLabel, formatDateTime } from '../../../utils/format'

export default function AdminVouchers() {
  const [data, setData] = useState({ content: [], page: 0, size: 10, totalElements: 0 })
  const [loading, setLoading] = useState(false)
  const [q, setQ] = useState('')
  const [ownerType, setOwnerType] = useState(null)
  const [discountType, setDiscountType] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(
    (page = 0) => {
      setLoading(true)
      // BE VoucherServlet: type=VoucherOwnerType, usage=DiscountTypeEnum
      voucherApi
        .list({ type: ownerType || undefined, usage: discountType || undefined, q: q || undefined, page, size: 10 })
        .then(setData)
        .catch((e) => message.error(e.message))
        .finally(() => setLoading(false))
    },
    [q, ownerType, discountType],
  )

  useEffect(() => { load(0) }, [])

  const openCreate = () => { setEditing(null); form.resetFields(); setModalOpen(true) }
  const openEdit = (r) => {
    setEditing(r)
    form.setFieldsValue({ ...r, validFrom: r.validFrom ? dayjs(r.validFrom) : null, validTo: r.validTo ? dayjs(r.validTo) : null })
    setModalOpen(true)
  }

  const toPayload = (values) => ({
    ...values,
    // BE chờ type (VoucherOwnerType) + discountType/discountValue/validFrom/validTo
    type: values.ownerType || values.type,
    validFrom: values.validFrom ? values.validFrom.toISOString() : undefined,
    validTo: values.validTo ? values.validTo.toISOString() : undefined,
  })

  const onSubmit = async () => {
    const values = await form.validateFields()
    const payload = toPayload(values)
    setSaving(true)
    try {
      if (editing) { await voucherApi.update(editing.id, payload); message.success('Cập nhật voucher thành công') }
      else { await voucherApi.create(payload); message.success('Tạo voucher thành công') }
      setModalOpen(false)
      load(data.page)
    } catch (e) { message.error(e.message) }
    finally { setSaving(false) }
  }

  const onDelete = async (id) => {
    try { await voucherApi.remove(id); message.success('Đã xóa voucher'); load(data.page) }
    catch (e) { message.error(e.message) }
  }

  const columns = [
    { title: 'Mã voucher', dataIndex: 'code', render: (c) => <Typography.Text strong style={{ fontFamily: 'monospace' }}>{c}</Typography.Text> },
    { title: 'Loại giảm', dataIndex: 'discountType', width: 110, render: (t) => <Tag color={discountTypeColor[t]} style={{ borderRadius: 999 }}>{discountTypeLabel(t)}</Tag> },
    { title: 'Giá trị', width: 130, render: (_, r) => formatDiscountValue(r.discountType, r.discountValue) },
    { title: 'Chủ sở hữu', dataIndex: 'ownerType', width: 110, render: (t) => t ? voucherOwnerLabel(t) : '-' },
    { title: 'Min nights', dataIndex: 'minNights', width: 90, align: 'center' },
    { title: 'Hiệu lực từ', dataIndex: 'validFrom', width: 150, render: formatDateTime },
    { title: 'Đến', dataIndex: 'validTo', width: 150, render: formatDateTime },
    { title: 'Tạo lúc', dataIndex: 'createdAt', width: 150, render: formatDateTime },
    {
      title: 'Thao tác', width: 110,
      render: (_, r) => (
        <Space><Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)} /><Popconfirm title="Xóa voucher này?" onConfirm={() => onDelete(r.id)}><Button size="small" danger icon={<DeleteOutlined />} /></Popconfirm></Space>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title={<span><GiftOutlined /> Quản lý voucher</span>} description="Quản lý mã ưu đãi — tạo và điều chỉnh chương trình khuyến mãi dành cho khách hàng" extra={[<Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm voucher</Button>]} />
      <FilterBar
        q={q} setQ={setQ} onSearch={() => load(0)} onReset={() => { setQ(''); setOwnerType(null); setDiscountType(null); setTimeout(() => load(0), 0) }}
        filters={[
          { key: 'ownerType', placeholder: 'Chủ sở hữu', options: VOUCHER_OWNER_TYPES.map((t) => ({ value: t, label: voucherOwnerLabel(t) })), value: ownerType, onChange: setOwnerType },
          { key: 'discountType', placeholder: 'Loại giảm', options: DISCOUNT_TYPES.map((t) => ({ value: t, label: discountTypeLabel(t) })), value: discountType, onChange: setDiscountType },
        ]}
        totalText={`Tổng ${data.totalElements} voucher · Trang ${data.page + 1}`}
        quickTags={['SALE10', 'SYSTEM']} onQuickTag={(k) => { setQ(k); setTimeout(() => load(0), 0) }}
      />
      <DataTable columns={columns} dataSource={data.content} loading={loading} page={data.page} size={data.size} totalElements={data.totalElements} onPageChange={load} emptyText="Chưa có voucher nào" />
      <Modal title={editing ? `Sửa voucher: ${editing.code}` : 'Thêm voucher'} open={modalOpen} onOk={onSubmit} onCancel={() => setModalOpen(false)} confirmLoading={saving} okText={editing ? 'Lưu' : 'Tạo'} cancelText="Hủy" width={640} destroyOnClose>
        <Form form={form} layout="vertical" requiredMark={false} initialValues={{ discountType: 'PERCENT', ownerType: 'SYSTEM', minNights: 1, usageLimitTotal: 100, usageLimitPerUser: 1, validFrom: dayjs(), validTo: dayjs().add(30, 'day') }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="code" label="Mã voucher" rules={[{ required: !editing, message: 'Nhập mã voucher' }, { max: 30 }]}><Input placeholder="SALE10" disabled={!!editing} /></Form.Item>
            <Form.Item name="ownerType" label="Chủ sở hữu" rules={[{ required: !editing }]}><Select placeholder="Chọn" options={VOUCHER_OWNER_TYPES.map((t) => ({ value: t, label: voucherOwnerLabel(t) }))} /></Form.Item>
            <Form.Item name="discountType" label="Loại giảm"><Select options={DISCOUNT_TYPES.map((t) => ({ value: t, label: discountTypeLabel(t) }))} /></Form.Item>
            <Form.Item name="discountValue" label="Giá trị giảm" rules={[{ required: !editing }]}><InputNumber min={0.01} style={{ width: '100%' }} placeholder="10 (%) hoặc 50000 (VND)" /></Form.Item>
            <Form.Item name="validFrom" label="Hiệu lực từ" rules={[{ required: !editing }]}><DatePicker showTime style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="validTo" label="Hiệu lực đến" rules={[{ required: !editing }]}><DatePicker showTime style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="minNights" label="Số đêm tối thiểu"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="minOrderValue" label="Giá trị đơn tối thiểu (VND)"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="usageLimitTotal" label="Giới hạn tổng"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="usageLimitPerUser" label="Giới hạn / user"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  )
}
