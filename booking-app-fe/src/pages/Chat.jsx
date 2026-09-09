import { useEffect, useState, useCallback, useRef } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { Card, Input, Button, List, Typography, Space, Tag, Empty, Skeleton, message, Divider } from 'antd'
import { SendOutlined, MessageOutlined, ArrowLeftOutlined } from '@ant-design/icons'
import PageHeader from '../components/common/PageHeader'
import { conversationApi } from '../api/conversations'
import { formatDateTime } from '../utils/format'

const { Text } = Typography

function MessageBubble({ msg, isMe }) {
  return (
    <div style={{ display: 'flex', justifyContent: isMe ? 'flex-end' : 'flex-start', marginBottom: 8 }}>
      <div style={{
        maxWidth: '72%',
        background: isMe ? '#1677ff' : '#f5f5f5',
        color: isMe ? '#fff' : '#1a1a2e',
        borderRadius: 12,
        padding: '8px 12px',
        fontSize: 14,
        lineHeight: 1.5,
        wordBreak: 'break-word',
        whiteSpace: 'pre-wrap',
      }}>
        {msg.content}
        <div style={{ fontSize: 10, marginTop: 4, opacity: 0.7 }}>{formatDateTime(msg.createdAt)}</div>
      </div>
    </div>
  )
}

function ConversationList({ conversations, selectedId, onSelect }) {
  if (!conversations || conversations.length === 0) return <Empty description="Chưa có hội thoại" />
  return (
    <List
      dataSource={conversations}
      renderItem={(c) => {
        const active = String(c.id) === String(selectedId)
        return (
          <div
            onClick={() => onSelect(c.id)}
            style={{
              padding: '10px 12px',
              cursor: 'pointer',
              background: active ? '#e6f4ff' : '#fff',
              borderLeft: active ? '3px solid #1677ff' : '3px solid transparent',
              borderBottom: '1px solid #f0f0f0',
            }}
          >
            <Text strong style={{ fontSize: 13 }} ellipsis>
              {c.hostFullName || c.userFullName || `Hội thoại #${c.id}`}
              {c.roomName && <Tag style={{ marginLeft: 6, borderRadius: 999 }}>{c.roomName}</Tag>}
            </Text>
            <br />
            <Text type="secondary" style={{ fontSize: 12 }} ellipsis>
              {c.lastMessagePreview || 'Chưa có tin nhắn'}
            </Text>
          </div>
        )
      }}
    />
  )
}

export default function Chat() {
  const { id } = useParams() // conversationId khi vào /messages/:id
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [conversations, setConversations] = useState([])
  const [loading, setLoading] = useState(true)
  const [messages, setMessages] = useState({ content: [], page: 0, totalElements: 0 })
  const [msgLoading, setMsgLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [input, setInput] = useState('')
  const [currentUserId, setCurrentUserId] = useState(null)
  const bottomRef = useRef(null)

  const loadConversations = useCallback(() => {
    conversationApi.list({ page: 0, size: 50 }).then((data) => {
      const content = Array.isArray(data) ? data : (data?.content || [])
      setConversations(content)
    }).catch(() => message.error('Không tải được hội thoại')).finally(() => setLoading(false))
  }, [])

  const loadMessages = useCallback((cid) => {
    if (!cid) return
    setMsgLoading(true)
    conversationApi.listMessages(cid, { page: 0, size: 50 }).then((data) => {
      setMessages(data)
      setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 100)
    }).catch((e) => message.error(e.message)).finally(() => setMsgLoading(false))
  }, [])

  useEffect(() => {
    try {
      const auth = JSON.parse(localStorage.getItem('vivu_auth') || 'null')
      setCurrentUserId(auth?.user?.id || null)
    } catch {}
    loadConversations()
  }, [loadConversations])

  useEffect(() => {
    if (id) loadMessages(id)
  }, [id, loadMessages])

  // Poll tin nhắn mỗi 5s khi đang mở hội thoại
  useEffect(() => {
    if (!id) return
    const t = setInterval(() => loadMessages(id), 5000)
    return () => clearInterval(t)
  }, [id, loadMessages])

  const onSend = async () => {
    const content = input.trim()
    if (!content) return
    if (!id) { message.warning('Chọn hội thoại trước'); return }
    setSending(true)
    try {
      await conversationApi.sendMessage(id, content)
      setInput('')
      loadMessages(id)
      loadConversations()
    } catch (e) {
      message.error(e.message)
    } finally {
      setSending(false)
    }
  }

  const selectedConversation = id ? conversations.find((c) => String(c.id) === String(id)) : null

  const roomId = params.get('roomId')
  const hostId = params.get('hostId')

  const startConversation = async () => {
    if (!hostId) { message.warning('Thiếu hostId'); return }
    try {
      const conv = await conversationApi.getOrCreate({ hostId: Number(hostId), roomId: roomId ? Number(roomId) : undefined })
      navigate(`/messages/${conv.id}`)
      loadConversations()
    } catch (e) {
      message.error(e.message)
    }
  }

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '16px 24px 32px' }}>
      <PageHeader
        title={<><MessageOutlined /> Tin nhắn</>}
        description={id ? `Hội thoại #${id}${selectedConversation?.roomName ? ` · ${selectedConversation.roomName}` : ''}` : 'Trao đổi với chủ nhà / khách — poll 5s tự cập nhật.'}
        extra={id ? <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/messages')}>Danh sách</Button> : null}
      />

      {!id && hostId && (
        <Card size="small" style={{ borderRadius: 12, marginBottom: 16 }}>
          <Space>
            <Button type="primary" onClick={startConversation}>Bắt đầu hội thoại với host #{hostId}{roomId ? ` · phòng #${roomId}` : ''}</Button>
            <Text type="secondary" style={{ fontSize: 12 }}>Tạo hoặc mở hội thoại hiện có.</Text>
          </Space>
        </Card>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: id ? '320px 1fr' : '1fr', gap: 16 }}>
        <Card bodyStyle={{ padding: 0, maxHeight: 560, overflow: 'auto' }} style={{ borderRadius: 12 }}>
          {loading ? <Skeleton active style={{ padding: 16 }} /> : (
            <ConversationList
              conversations={conversations}
              selectedId={id}
              onSelect={(cid) => navigate(`/messages/${cid}`)}
            />
          )}
        </Card>

        {id && (
          <Card style={{ borderRadius: 12, display: 'flex', flexDirection: 'column', minHeight: 560 }}>
            <div style={{ flex: 1, overflow: 'auto', maxHeight: 420, padding: '8px 0' }}>
              {msgLoading ? <Skeleton active /> : messages.content.length === 0 ? (
                <Empty description="Chưa có tin nhắn — gửi lời chào đầu tiên" />
              ) : (
                <>
                  {[...messages.content].reverse().map((m) => (
                    <MessageBubble key={m.id} msg={m} isMe={currentUserId != null && String(m.sender?.id) === String(currentUserId)} />
                  ))}
                  <div ref={bottomRef} />
                </>
              )}
            </div>
            <Divider style={{ margin: '12px 0' }} />
            <Space.Compact style={{ width: '100%' }}>
              <Input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onPressEnter={onSend}
                placeholder="Nhập tin nhắn..."
                disabled={sending}
                maxLength={4000}
              />
              <Button type="primary" icon={<SendOutlined />} onClick={onSend} loading={sending}>Gửi</Button>
            </Space.Compact>
          </Card>
        )}
      </div>
    </div>
  )
}
