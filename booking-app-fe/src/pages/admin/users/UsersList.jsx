import { useEffect, useState, useCallback } from 'react'
import { Button, Tag, Modal, Form, Input, Select, Switch, message, Popconfirm, Space, Typography, Avatar, Upload, Alert } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, TeamOutlined, UploadOutlined, DownloadOutlined, FileAddOutlined } from '@ant-design/icons'
import PageHeader from '../../../components/common/PageHeader'
import FilterBar from '../../../components/common/FilterBar'
import DataTable from '../../../components/common/DataTable'
import { userApi } from '../../../api/users'
import client from '../../../api/client'
import { ENDPOINTS, USER_STATUS } from '../../../utils/constants'
import { formatDateTime, userStatusColor, userStatusLabel, genderLabel } from '../../../utils/format'

export default function AdminUsers() {
  const [data, setData] = useState({ content: [], page: 0, size: 10, totalElements: 0 })
  const [loading, setLoading] = useState(false)
  const [roles, setRoles] = useState([])
  const [q, setQ] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [importLoading, setImportLoading] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(
    (page = 0) => {
      setLoading(true)
      userApi
        .list({ q: q || undefined, page, size: 10 })
        .then(setData)
        .catch((e) => message.error(e.message))
        .finally(() => setLoading(false))
    },
    [q],
  )

  useEffect(() => {
    load(0)
    client
      .get(ENDPOINTS.roles)
      .then((r) => {
        const d = r.data?.data ?? r.data
        setRoles(Array.isArray(d) ? d : d?.content || [])
      })
      .catch(() => {})
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setModalOpen(true)
  }
  const openEdit = (record) => {
    setEditing(record)
    // role là Set<RoleResponse> với id/code/name, tiền tô roleId từ ids đó
    const roleIds = (record.role || []).map((r) => r.id).filter(Boolean)
    form.setFieldsValue({ ...record, roleId: roleIds, password: undefined })
    setModalOpen(true)
  }

  const onSubmit = async () => {
    const values = await form.validateFields()
    const { avatarFile, ...rest } = values
    const file = avatarFile?.fileList?.[0]?.originFileObj || null
    // BE chờ field file + user JSON
    const payload = { ...rest }
    if (editing && !payload.password) delete payload.password
    setSaving(true)
    try {
      if (editing) {
        await userApi.update(editing.id, payload, file)
        message.success('Cập nhật người dùng thành công')
      } else {
        await userApi.create(payload, file)
        message.success('Tạo người dùng thành công')
      }
      setModalOpen(false)
      load(data.page)
    } catch (e) {
      message.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  const onDelete = async (id) => {
    try {
      await userApi.remove(id)
      message.success('Đã xóa người dùng (xóa mềm)')
      load(data.page)
    } catch (e) {
      message.error(e.message)
    }
  }

  const onImport = async (file) => {
    setImportLoading(true)
    try {
      await userApi.importExcel(file)
      message.success('Import thành công')
      load(0)
    } catch (e) {
      message.error(e.message || 'Import thất bại')
    } finally {
      setImportLoading(false)
    }
  }

  const columns = [
    {
      title: 'Người dùng',
      render: (_, r) => (
        <Space>
          <Avatar src={r.avatar} size={40}>
            {r.fullName?.[0] || r.username?.[0]}
          </Avatar>
          <div>
            <div style={{ fontWeight: 600 }}>{r.fullName || r.username}</div>
            <Typography.Text type="secondary" style={{ fontSize: 11 }}>
              @{r.username} · {r.email}
            </Typography.Text>
          </div>
        </Space>
      ),
    },
    { title: 'SĐT', dataIndex: 'phone', width: 130 },
    {
      title: 'GT',
      dataIndex: 'gender',
      width: 60,
      render: (g) => genderLabel(g),
    },
    {
      title: 'Role',
      dataIndex: 'role',
      width: 160,
      render: (roles) => (
        <Space size={4} wrap>
          {(roles || []).map((rr) => (
            <Tag key={rr.id || rr.name} color={rr.code === 'admin' ? 'red' : rr.code === 'host' ? 'gold' : 'blue'} style={{ borderRadius: 999, margin: 2 }}>
              {rr.name || rr.code}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 120,
      render: (s) => (
        <Tag color={userStatusColor[s]} style={{ borderRadius: 999 }}>
          {userStatusLabel(s)}
        </Tag>
      ),
    },
    { title: 'Cập nhật', dataIndex: 'updatedAt' in {} ? 'updatedAt' : 'createdAt', width: 145, render: (_, r) => formatDateTime(r.updatedAt || r.createdAt) },
    {
      title: 'Thao tác',
      width: 110,
      render: (_, r) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)} />
          <Popconfirm title="Xóa người dùng này?" onConfirm={() => onDelete(r.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title={
          <span>
            <TeamOutlined /> Quản lý người dùng
          </span>
        }
        description="Quản lý tài khoản — tạo mới, cập nhật hồ sơ và phân quyền cho từng người dùng"
        extra={[
          <Upload key="import" accept=".xlsx,.xls" maxCount={1} showUploadList={false} beforeUpload={(file) => { onImport(file); return false }}>
            <Button icon={<FileAddOutlined />} loading={importLoading}>
              Import Excel
            </Button>
          </Upload>,
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm user
          </Button>,
        ]}
      />

      <FilterBar
        q={q}
        setQ={setQ}
        onSearch={() => load(0)}
        onReset={() => {
          setQ('')
          setTimeout(() => load(0), 0)
        }}
        totalText={`Tổng ${data.totalElements} người dùng · Trang ${data.page + 1}`}
      />

      <DataTable columns={columns} dataSource={data.content} loading={loading} page={data.page} size={data.size} totalElements={data.totalElements} onPageChange={load} emptyText="Chưa có người dùng nào" />

      <Modal title={editing ? `Sửa: ${editing.username}` : 'Thêm người dùng'} open={modalOpen} onOk={onSubmit} onCancel={() => setModalOpen(false)} confirmLoading={saving} okText={editing ? 'Lưu' : 'Tạo'} cancelText="Hủy" width={640} destroyOnClose>
        <Form form={form} layout="vertical" requiredMark={false} initialValues={{ status: 'ACTIVE', active: true, gender: true }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, message: 'Nhập họ tên' }, { max: 150 }]}>
              <Input placeholder="Nguyễn Văn A" />
            </Form.Item>
            <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập username' }, { min: 4 }]} extra={!editing ? 'Dùng để đăng nhập' : 'Không đổi username khi sửa'}>
              <Input placeholder="username" disabled={!!editing} />
            </Form.Item>
            <Form.Item name="email" label="Email" rules={[{ required: true, message: 'Nhập email' }, { type: 'email' }]}>
              <Input placeholder="ban@vidu.com" />
            </Form.Item>
            <Form.Item name="phone" label="Số điện thoại" rules={[{ required: true }, { pattern: /^0[0-9]{9}$/, message: '10 số bắt đầu bằng 0' }]}>
              <Input placeholder="0912345678" maxLength={10} />
            </Form.Item>
            <Form.Item name="password" label={editing ? 'Mật khẩu mới (để trống giữ nguyên)' : 'Mật khẩu'} rules={editing ? [{ min: 6 }] : [{ required: true, message: 'Nhập mật khẩu' }, { min: 6 }]}>
              <Input.Password placeholder="••••••••" />
            </Form.Item>
            <Form.Item name="gender" label="Giới tính" rules={[{ required: true }]}>
              <Select options={[{ value: true, label: 'Nam' }, { value: false, label: 'Nữ' }]} />
            </Form.Item>
            <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
              <Select options={USER_STATUS.map((s) => ({ value: s, label: userStatusLabel(s) }))} />
            </Form.Item>
            <Form.Item name="roleId" label="Vai trò" rules={[{ required: true, message: 'Chọn ít nhất 1 role' }]}>
              <Select mode="multiple" placeholder="Chọn role" options={roles.map((r) => ({ value: r.id, label: `${r.code} — ${r.name}` }))} />
            </Form.Item>
            <Form.Item name="active" label="Kích hoạt" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
          <Form.Item name="avatarFile" label="Ảnh đại diện (tùy chọn)">
            <Upload listType="picture" maxCount={1} accept="image/*" beforeUpload={() => false}>
              <Button icon={<UploadOutlined />}>Chọn ảnh</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
