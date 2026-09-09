import { useMemo, useState } from 'react'

// ============================================================
// Chart — bieu do SVG viet tay, KHONG them dependency moi.
// package.json khong co chart lib; vite 8 + antd 6 du de ve 3 loai
// bieu do dashboard doanh thu: cot (bar), duong (line), vong (donut).
//
// Nguyen tac:
//  - viewBox + width="100%" -> tu co gian theo container, khong can resize listener.
//  - <title> trong moi phan tu = tooltip trinh duyet, khong can JS rieng.
//  - Truc Y luon bat dau tu 0 (truc cat ngan lam sai lech ty le truc quan).
//  - Mau lay tu palette dang dung o StatCard/AppLayout de dong nhat.
// ============================================================

const PAD = { top: 16, right: 16, bottom: 34, left: 62 }
const TICKS = 4

function niceMax(v) {
  if (!v || v <= 0) return 1
  const pow = Math.pow(10, Math.floor(Math.log10(v)))
  const n = v / pow
  const step = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10
  return step * pow
}

function fmtCompact(v) {
  const n = Number(v) || 0
  if (Math.abs(n) >= 1e9) return (n / 1e9).toFixed(1).replace('.0', '') + 'B'
  if (Math.abs(n) >= 1e6) return (n / 1e6).toFixed(1).replace('.0', '') + 'M'
  if (Math.abs(n) >= 1e3) return (n / 1e3).toFixed(1).replace('.0', '') + 'k'
  return String(Math.round(n))
}

function shortLabel(s, max = 6) {
  if (!s) return ''
  const t = String(s)
  // "2026-09-12" -> "12/09", "2026-09-01" -> "09/2026"
  const m = t.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (m) return m[3] === '01' ? `${m[2]}/${m[1].slice(2)}` : `${m[3]}/${m[2]}`
  return t.length > max ? t.slice(0, max) : t
}

/**
 * Bieu do cot.
 * @param data      mang object
 * @param xKey      khoa truc X (thuong la "period")
 * @param yKey      khoa truc Y (thuong la "revenue")
 * @param labelKey  khoa hien tooltip (mac dinh gop X + Y)
 */
export function BarChart({ data = [], xKey = 'period', yKey = 'revenue', color = '#1968f5', height = 240, labelKey }) {
  const [w] = useState(720)
  const max = useMemo(() => niceMax(Math.max(0, ...data.map((d) => Number(d[yKey]) || 0))), [data, yKey])
  const innerW = w - PAD.left - PAD.right
  const innerH = height - PAD.top - PAD.bottom
  const step = data.length ? innerW / data.length : innerW
  const barW = Math.max(4, Math.min(38, step * 0.62))

  return (
    <svg viewBox={`0 0 ${w} ${height}`} width="100%" height={height} role="img"
         aria-label={`Bieu do cot ${yKey} theo ${xKey}`}>
      {Array.from({ length: TICKS + 1 }).map((_, i) => {
        const y = PAD.top + (innerH * i) / TICKS
        const val = max * (1 - i / TICKS)
        return (
          <g key={i}>
            <line x1={PAD.left} y1={y} x2={w - PAD.right} y2={y} stroke="#eef0f4" strokeWidth="1" />
            <text x={PAD.left - 8} y={y + 4} textAnchor="end" fontSize="11" fill="#8c8c8c">{fmtCompact(val)}</text>
          </g>
        )
      })}
      {data.map((d, i) => {
        const v = Number(d[yKey]) || 0
        const h = max ? (v / max) * innerH : 0
        const x = PAD.left + i * step + (step - barW) / 2
        const y = PAD.top + innerH - h
        const skipX = data.length > 14 && i % Math.ceil(data.length / 10) !== 0
        return (
          <g key={i}>
            <rect x={x} y={y} width={barW} height={Math.max(0, h)} rx="4" fill={color} opacity={v ? 1 : 0.25}>
              <title>{labelKey ? d[labelKey] : `${d[xKey]}: ${v.toLocaleString('vi-VN')}`}</title>
            </rect>
            {!skipX && (
              <text x={x + barW / 2} y={height - PAD.bottom + 16} textAnchor="middle" fontSize="10" fill="#8c8c8c">
                {shortLabel(d[xKey])}
              </text>
            )}
          </g>
        )
      })}
      <line x1={PAD.left} y1={PAD.top + innerH} x2={w - PAD.right} y2={PAD.top + innerH} stroke="#d9d9d9" />
    </svg>
  )
}

/**
 * Bieu do duong nhieu series.
 * @param series [{ key, color, label }]
 */
export function LineChart({ data = [], xKey = 'period', series = [], height = 240 }) {
  const [w] = useState(720)
  const innerW = w - PAD.left - PAD.right
  const innerH = height - PAD.top - PAD.bottom
  const max = useMemo(
    () => niceMax(Math.max(0, ...data.flatMap((d) => series.map((s) => Number(d[s.key]) || 0)))),
    [data, series],
  )
  const px = (i) => PAD.left + (data.length <= 1 ? innerW / 2 : (innerW * i) / (data.length - 1))
  const py = (v) => PAD.top + innerH - (max ? (Number(v) / max) * innerH : 0)

  return (
    <svg viewBox={`0 0 ${w} ${height}`} width="100%" height={height} role="img" aria-label="Bieu do duong">
      {Array.from({ length: TICKS + 1 }).map((_, i) => {
        const y = PAD.top + (innerH * i) / TICKS
        return (
          <g key={i}>
            <line x1={PAD.left} y1={y} x2={w - PAD.right} y2={y} stroke="#eef0f4" />
            <text x={PAD.left - 8} y={y + 4} textAnchor="end" fontSize="11" fill="#8c8c8c">
              {fmtCompact(max * (1 - i / TICKS))}
            </text>
          </g>
        )
      })}
      {series.map((s) => (
        <polyline
          key={s.key}
          fill="none"
          stroke={s.color}
          strokeWidth="2.2"
          strokeLinejoin="round"
          strokeLinecap="round"
          points={data.map((d, i) => `${px(i)},${py(d[s.key])}`).join(' ')}
        />
      ))}
      {data.map((d, i) =>
        series.map((s) => (
          <circle key={`${s.key}-${i}`} cx={px(i)} cy={py(d[s.key])} r="3" fill="#fff" stroke={s.color} strokeWidth="2">
            <title>{`${d[xKey]} — ${s.label}: ${(Number(d[s.key]) || 0).toLocaleString('vi-VN')}`}</title>
          </circle>
        )),
      )}
      {data.map((d, i) => {
        const skipX = data.length > 14 && i % Math.ceil(data.length / 10) !== 0
        if (skipX) return null
        return (
          <text key={i} x={px(i)} y={height - PAD.bottom + 16} textAnchor="middle" fontSize="10" fill="#8c8c8c">
            {shortLabel(d[xKey])}
          </text>
        )
      })}
      <g>
        {series.map((s, i) => (
          <g key={s.key} transform={`translate(${PAD.left + i * 150}, 6)`}>
            <rect width="10" height="10" rx="2" fill={s.color} />
            <text x="16" y="9" fontSize="11" fill="#595959">{s.label}</text>
          </g>
        ))}
      </g>
    </svg>
  )
}

/**
 * Bieu do vong (phan bo theo loai phong).
 * @param data      [{ label, value }]
 */
export function DonutChart({ data = [], labelKey = 'roomType', valueKey = 'revenue', size = 220, colors }) {
  const palette = colors || ['#1968f5', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2', '#faad14']
  const total = data.reduce((s, d) => s + (Number(d[valueKey]) || 0), 0)
  const cx = size / 2
  const cy = size / 2
  const r = size / 2 - 14
  const ir = r * 0.58
  let acc = 0

  const arc = (startFrac, endFrac, color) => {
    const a0 = startFrac * Math.PI * 2 - Math.PI / 2
    const a1 = endFrac * Math.PI * 2 - Math.PI / 2
    const large = endFrac - startFrac > 0.5 ? 1 : 0
    const x0 = cx + r * Math.cos(a0), y0 = cy + r * Math.sin(a0)
    const x1 = cx + r * Math.cos(a1), y1 = cy + r * Math.sin(a1)
    const xi1 = cx + ir * Math.cos(a1), yi1 = cy + ir * Math.sin(a1)
    const xi0 = cx + ir * Math.cos(a0), yi0 = cy + ir * Math.sin(a0)
    return (
      <path
        d={`M ${x0} ${y0} A ${r} ${r} 0 ${large} 1 ${x1} ${y1} L ${xi1} ${yi1} A ${ir} ${ir} 0 ${large} 0 ${xi0} ${yi0} Z`}
        fill={color}
      />
    )
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
      <svg viewBox={`0 0 ${size} ${size}`} width={size} height={size} role="img" aria-label="Phan bo doanh thu theo loai phong">
        {total === 0 ? (
          <circle cx={cx} cy={cy} r={(r + ir) / 2} fill="none" stroke="#eef0f4" strokeWidth={r - ir} />
        ) : (
          data.map((d, i) => {
            const frac = (Number(d[valueKey]) || 0) / total
            const el = arc(acc, acc + frac, palette[i % palette.length])
            acc += frac
            return (
              <g key={i}>
                {el}
                <title>{`${d[labelKey]}: ${(Number(d[valueKey]) || 0).toLocaleString('vi-VN')} (${(frac * 100).toFixed(1)}%)`}</title>
              </g>
            )
          })
        )}
        <text x={cx} y={cy - 2} textAnchor="middle" fontSize="12" fill="#8c8c8c">Tong</text>
        <text x={cx} y={cy + 16} textAnchor="middle" fontSize="15" fontWeight="700" fill="#1a1a2e">{fmtCompact(total)}</text>
      </svg>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {data.map((d, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12 }}>
            <span style={{ width: 10, height: 10, borderRadius: 3, background: palette[i % palette.length], flexShrink: 0 }} />
            <span style={{ color: '#595959', minWidth: 70 }}>{d[labelKey]}</span>
            <span style={{ fontWeight: 600 }}>{fmtCompact(d[valueKey])}</span>
            <span style={{ color: '#8c8c8c' }}>
              {total ? `${(((Number(d[valueKey]) || 0) / total) * 100).toFixed(1)}%` : '0%'}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default { BarChart, LineChart, DonutChart }
