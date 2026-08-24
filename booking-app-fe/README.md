# booking-app-fe — VIVU Booking Frontend

React 19 + Vite + Ant Design + Redux Toolkit + React Router

## Yêu cầu

- Node 18+ (đang dùng 24.11.1), npm 11+

## Chạy local

```bash
cd booking-app-fe
cp .env.example .env        # sửa VITE_API_BASE_URL nếu BE không ở localhost:8080
npm install
npm run dev                 # http://localhost:3000
npm run build               # build ra dist/
npm run preview             # preview build
```

## Cấu hình BE

Đổi 1 chỗ duy nhất — file `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
# Khi deploy: VITE_API_BASE_URL=http://103.216.117.40:8080
# hoặc https://api.vivu.example.com
```

FE gọi BE qua `src/api/client.js` (axios) với `baseURL = VITE_API_BASE_URL`.  
Vite dev proxy `/api -> VITE_API_BASE_URL` đã cấu hình trong `vite.config.js`; production thì axios gọi thẳng `VITE_API_BASE_URL`.

Endpoints đang dùng:

- `GET /api/health`
- `GET /api/rooms?q=&type=&status=&page=&size=`
- `POST /api/rooms`, `GET /api/rooms/:id`, `PUT /api/rooms/:id`, `DELETE /api/rooms/:id`

## Cấu trúc

```
booking-app-fe/
  public/
  src/
    api/          # client axios + roomApi, healthApi  -> gọi BE (dễ thay baseURL)
    store/        # Redux: authSlice, roomSlice, index.js
    pages/        # Login.jsx, Home.jsx (khởi động theo Traveloka/Agoda)
    components/   # AppLayout.jsx (Header/Footer)
    utils/        # constants.js (API_BASE_URL, ENDPOINTS), format.js
    hooks/        # hooks dùng chung (để trống cho sinh viên thêm)
    assets/       # ảnh tĩnh
    App.jsx       # Router + RequireAuth
    main.jsx      # Provider + BrowserRouter + ConfigProvider
  .env            # VITE_API_BASE_URL (không commit khi đổi IP/port thật)
  .env.example    # mẫu
  vite.config.js  # proxy /api
  Dockerfile      # multi-stage build
```

Quy ước cho BE dễ đọc: `api/` mirror endpoint BE, `store/` theo slice (`room`, `auth`), `pages/` theo route (`/login`, `/`), `components/` tái sử dụng.

## Màn hình

- `/login` — form Ant Design, mock login (bất kỳ user nào cũng vào được), lưu `vivu_auth` vào localStorage + Redux
- `/` — Home cần đăng nhập; hero + search bar (q, type, ngày, khách) + grid cards + phân trang; gọi `GET /api/rooms`; skeleton/empty/error đã xử lý

Lấy cảm hứng UI từ [Traveloka](https://www.traveloka.com) & Agoda.

## Docker

Build riêng FE:

```bash
docker build -t vivu-booking-fe -f booking-app-fe/Dockerfile .
docker run --rm -p 3000:80 -e VITE_API_BASE_URL=http://host.docker.internal:8080 vivu-booking-fe
```

Hoặc chạy cả BE+FE từ root:

```bash
cd ..                 # booking-app/
docker compose up --build
# FE http://localhost:3000  -> BE http://localhost:8080
```

Đổi IP/port thật: sửa `VITE_API_BASE_URL` trong `booking-app-fe/.env` (dev) hoặc build arg `VITE_API_BASE_URL` khi `docker build`.
