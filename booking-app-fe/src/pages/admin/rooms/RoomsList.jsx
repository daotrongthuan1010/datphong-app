import { useEffect, useState, useCallback } from 'react'
import { Button, Tag, Modal, Form, Input, Select, InputNumber, message, Popconfirm, Space, Typography, Avatar, Upload } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, ApartmentOutlined, PictureOutlined, InboxOutlined, VideoCameraOutlined, DeleteFilled } from '@ant-design/icons'
import PageHeader from '../../../components/common/PageHeader'
import FilterBar from '../../../components/common/FilterBar'
import DataTable from '../../../components/common/DataTable'
import { roomApi } from '../../../api/rooms'
import { ROOM_TYPES, ROOM_STATUS } from '../../../utils/constants'
import { formatPrice, formatDateTime, roomTypeLabel, roomStatusColor, roomStatusLabel } from '../../../utils/format'
import { MOCK_ROOM_IMAGES } from '../../../components/common/RoomCard'

export default function AdminRooms() {
  const [data, setData] = useState({ content: [], page: 0, size: 10, totalElements: 0 })
  const [loading, setLoading] = useState(false)
  const [q, setQ] = useState('')
  const [type, setType] = useState(null)
  const [status, setStatus] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [mediaRoom, setMediaRoom] = useState(null)
  const [mediaFiles, setMediaFiles] = useState([])
  const [existingMedia, setExistingMedia] = useState([])
  const [loadingMedia, setLoadingMedia] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(
    (page = 0) => {
      setLoading(true)
      roomApi
        .list({ q: q || undefined, type: type || undefined, status: status || undefined, page, size: 10 })
        .then(setData)
        .catch((e) => message.error(e.message))
        .finally(() => setLoading(false))
    },
    [q, type, status],
  )

  useEffect(() => {
    load(0)
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setModalOpen(true)
  }
  const openEdit = (record) => {
    setEditing(record)
    form.setFieldsValue(record)
    setModalOpen(true)
  }

  const onSubmit = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      if (editing) {
        await roomApi.update(editing.id, values)
        message.success('Cập nhật phòng thành công')
      } else {
        await roomApi.create(values)
        message.success('Tạo phòng thành công')
      }
      setModalOpen(false)
      load(data.page)
    } catch (e) {
      if (e.message) message.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  const onDelete = async (id) => {
    try {
      await roomApi.remove(id)
      message.success('Đã xóa phòng')
      load(data.page)
    } catch (e) {
      message.error(e.message)
    }
  }

  // Thư viện media: xem danh sách hiện có (MinIO) + upload thêm + xoá từng item
  const openMedia = async (room) => {
    setMediaRoom(room)
    setMediaFiles([])
    setLoadingMedia(true)
    try {
      const media = await roomApi.mediaList(room.id)
      setExistingMedia(media || [])
    } catch {
      setExistingMedia(room.media || [])
    } finally {
      setLoadingMedia(false)
    }
  }

  const reloadMedia = async () => {
    if (!mediaRoom) return
    try {
      const media = await roomApi.mediaList(mediaRoom.id)
      setExistingMedia(media || [])
    } catch {}
    load(data.page)
  }

  const uploadMedia = async () => {
    if (!mediaFiles.length) {
      message.warning('Chọn ít nhất 1 ảnh hoặc video')
      return
    }
    setUploading(true)
    let ok = 0
    for (const f of mediaFiles) {
      try {
        await roomApi.uploadMedia(mediaRoom.id, f.originFileObj || f)
        ok += 1
      } catch (e) {
        message.error(`Lỗi tải ${f.name}: ${e.message}`)
      }
    }
    setUploading(false)
    if (ok > 0) {
      message.success(`Đã tải lên ${ok} file — khách sẽ thấy trong trang chi tiết phòng`)
      setMediaFiles([])
      await reloadMedia()
    }
  }

  const deleteOneMedia = async (mediaId) => {
    try {
      await roomApi.deleteMedia(mediaRoom.id, mediaId)
      setExistingMedia((prev) => prev.filter((m) => m.id !== mediaId))
      message.success('Đã xoá')
      load(data.page)
    } catch (e) {
      message.error(e.message)
    }
  }

  const columns = [
    {
      title: 'Phòng',
      dataIndex: 'name',
      render: (_, r) => (
        <Space>
          <Avatar shape="square" size={44} src={r.imageUrl || MOCK_ROOM_IMAGES[r.id % MOCK_ROOM_IMAGES.length]} style={{ flexShrink: 0 }} />
          <div>
            <div style={{ fontWeight: 600 }}>{r.name}</div>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {r.code}
            </Typography.Text>
          </div>
        </Space>
      ),
    },
    { title: 'Loại', dataIndex: 'type', width: 110, render: (t) => roomTypeLabel(t) },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 130,
      render: (s) => (
        <Tag color={roomStatusColor[s]} style={{ borderRadius: 999 }}>
          {roomStatusLabel(s)}
        </Tag>
      ),
    },
    { title: 'Sức chứa', dataIndex: 'capacity', width: 90, align: 'center' },
    { title: 'Giá/đêm', dataIndex: 'pricePerNight', width: 140, render: (v) => <Typography.Text strong style={{ color: '#ff3b30' }}>{formatPrice(v)}</Typography.Text> },
    { title: 'Cập nhật', dataIndex: 'updatedAt', width: 150, render: formatDateTime },
    {
      title: 'Thao tác',
      width: 180,
      render: (_, r) => (
        <Space>
          <Button size="small" icon={<PictureOutlined />} onClick={() => openMedia(r)}>Media</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)} />
          <Popconfirm title="Xóa phòng này?" description="Xóa mềm — phòng sẽ chuyển trạng thái không hoạt động." onConfirm={() => onDelete(r.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const imageCount = existingMedia.filter((m) => m.mediaType === 'IMAGE').length
  const videoCount = existingMedia.filter((m) => m.mediaType === 'VIDEO').length

  return (
    <div>
      <PageHeader
        title={
          <span>
            <ApartmentOutlined /> Quản lý phòng
          </span>
        }
        description="Quản lý danh sách phòng — thêm mới, chỉnh sửa thông tin và ẩn phòng khi tạm ngưng phục vụ"
        extra={[
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm phòng
          </Button>,
        ]}
      />

      <FilterBar
        q={q}
        setQ={setQ}
        onSearch={() => load(0)}
        onReset={() => {
          setQ('')
          setType(null)
          setStatus(null)
          setTimeout(() => load(0), 0)
        }}
        filters={[
          { key: 'type', placeholder: 'Loại phòng', options: ROOM_TYPES.map((t) => ({ value: t, label: roomTypeLabel(t) })), value: type, onChange: setType },
          { key: 'status', placeholder: 'Trạng thái', options: ROOM_STATUS.map((s) => ({ value: s, label: roomStatusLabel(s) })), value: status, onChange: setStatus },
        ]}
        totalText={`Tổng ${data.totalElements} phòng · Trang ${data.page + 1}`}
        quickTags={['Deluxe', 'FAMILY', 'SUITE']}
        onQuickTag={(k) => {
          setQ(k)
          setTimeout(() => load(0), 0)
        }}
      />

      <DataTable columns={columns} dataSource={data.content} loading={loading} page={data.page} size={data.size} totalElements={data.totalElements} onPageChange={load} emptyText="Chưa có phòng nào — bấm Thêm phòng" />

      <Modal title={editing ? `Sửa phòng: ${editing.code}` : 'Thêm phòng mới'} open={modalOpen} onOk={onSubmit} onCancel={() => setModalOpen(false)} confirmLoading={saving} okText={editing ? 'Lưu' : 'Tạo'} cancelText="Hủy" width={640} destroyOnClose>
        <Form form={form} layout="vertical" requiredMark={false} initialValues={{ type: 'SINGLE', status: 'AVAILABLE', capacity: 2, pricePerNight: 500000 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="code" label="Mã phòng" rules={[{ required: true, message: 'Nhập mã phòng' }, { max: 20 }]} extra={!editing ? 'VD: R101' : undefined}>
              <Input placeholder="R101" disabled={!!editing} />
            </Form.Item>
            <Form.Item name="name" label="Tên phòng" rules={[{ required: true, message: 'Nhập tên phòng' }, { max: 150 }]}>
              <Input placeholder="Phòng Deluxe 101" />
            </Form.Item>
            <Form.Item name="type" label="Loại phòng" rules={[{ required: true }]}>
              <Select options={ROOM_TYPES.map((t) => ({ value: t, label: roomTypeLabel(t) }))} />
            </Form.Item>
            <Form.Item name="status" label="Trạng thái">
              <Select options={ROOM_STATUS.map((s) => ({ value: s, label: roomStatusLabel(s) }))} />
            </Form.Item>
            <Form.Item name="capacity" label="Sức chứa" rules={[{ required: true }]}>
              <InputNumber min={1} max={20} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="pricePerNight" label="Giá/đêm (VND)" rules={[{ required: true }]}>
              <InputNumber min={0} step={50000} style={{ width: '100%' }} formatter={(v) => (v ? v.toLocaleString('vi-VN') : v)} />
            </Form.Item>
          </div>
          <Form.Item name="description" label="Mô tả" rules={[{ max: 500 }]}>
            <Input.TextArea rows={3} placeholder="Mô tả chi tiết phòng..." />
          </Form.Item>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 16, fontSize: 13 }}>
            Ảnh và video được quản lý qua nút <strong>Media</strong> ở bảng — tải trực tiếp lên MinIO, tự đặt ảnh bìa.
          </Typography.Text>
          {editing && (
            <Form.Item name="active" label="Kích hoạt" valuePropName="checked">
              <Select options={[{ value: true, label: 'Hoạt động' }, { value: false, label: 'Không hoạt động' }]} />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title={mediaRoom ? `Thư viện media: ${mediaRoom.name}` : 'Thư viện media'}
        open={!!mediaRoom}
        onCancel={() => setMediaRoom(null)}
        onOk={uploadMedia}
        confirmLoading={uploading}
        okText={mediaFiles.length ? `Tải lên ${mediaFiles.length} file` : 'Đóng'}
        cancelText="Hủy"
        width={720}
        destroyOnClose
      >
        {loadingMedia ? (
          <Typography.Text type="secondary">Đang tải danh sách...</Typography.Text>
        ) : existingMedia.length > 0 ? (
          <>
            <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
              Đã lưu trên MinIO — {imageCount} ảnh, {videoCount} video
            </Typography.Text>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 16 }}>
              {existingMedia.map((m) => (
                <div key={m.id} style={{ position: 'relative', width: 120, borderRadius: 10, overflow: 'hidden', background: '#000', border: '1px solid #eee' }}>
                  {m.mediaType === 'VIDEO' ? (
                    <video src={m.url} controls preload="metadata" style={{ width: '100%', height: 90, display: 'block' }} />
                  ) : (
                    <img src={m.url} alt="" style={{ width: '100%', height: 90, objectFit: 'cover', display: 'block' }} />
                  )}
                  <div style={{ position: 'absolute', top: 4, left: 4, background: 'rgba(0,0,0,0.6)', color: '#fff', fontSize: 10, padding: '1px 6px', borderRadius: 999 }}>
                    {m.mediaType === 'VIDEO' ? <><VideoCameraOutlined /> Video</> : <><PictureOutlined /> Ảnh</>}
                  </div>
                  <Popconfirm title="Xoá file này?" onConfirm={() => deleteOneMedia(m.id)} okText="Xoá" cancelText="Hủy">
                    <Button size="small" danger icon={<DeleteFilled />} style={{ position: 'absolute', top: 4, right: 4, width: 26, height: 26, padding: 0 }} />
                  </Popconfirm>
                </div>
              ))}
            </div>
          </>
        ) : (
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
            Chưa có ảnh/video nào — tải lên bên dưới.
          </Typography.Text>
        )}
        <Upload.Dragger
          multiple
          accept="image/*,video/*"
          listType="picture"
          fileList={mediaFiles}
          beforeUpload={() => false}
          onChange={({ fileList }) => setMediaFiles(fileList)}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">Kéo thả hoặc bấm để chọn ảnh &amp; video</p>
          <p className="ant-upload-hint">
            Ảnh (jpg/png/webp) và video (mp4/mov) được lưu vào MinIO — ảnh đầu tự đặt làm bìa phòng
          </p>
        </Upload.Dragger>
      </Modal>
    </div>
  )
}
