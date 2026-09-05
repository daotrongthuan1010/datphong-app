import { Table, Empty, Skeleton } from 'antd'
import { useEffect } from 'react'

// ============================================================
// DataTable — bang duy nhat cho TOAN BO man hinh admin (Rooms,
// Users, Vouchers, Hosts...). Moi trang chi khai columns khac nhau,
// con cau truc bang / phan trang / loading / empty la GIONG NHAU.
//
// Phan trang dong bo BE PageResponse: page (0-based), size, totalElements.
// ============================================================
export default function DataTable({
  columns,
  dataSource = [],
  loading = false,
  page = 0,
  size = 10,
  totalElements,
  onPageChange,
  rowKey = 'id',
  scroll,
  emptyText = 'Chưa có dữ liệu',
}) {
  return (
    <div className="vivu-table">
      <Table
        columns={columns}
        dataSource={dataSource}
        rowKey={rowKey}
        loading={loading}
        scroll={scroll}
        locale={{
          emptyText: loading ? <Skeleton active paragraph={{ rows: 2 }} /> : <Empty description={emptyText} />,
        }}
        pagination={
          totalElements == null
            ? false
            : {
                current: page + 1,
                pageSize: size,
                total: totalElements,
                showSizeChanger: false,
                showTotal: (t) => `Tổng ${t} dòng`,
                onChange: (p) => onPageChange?.(p - 1),
              }
        }
        style={{ background: '#fff', borderRadius: 12, overflow: 'hidden' }}
        size="middle"
      />
    </div>
  )
}
