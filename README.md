# booking-app — VIVU (Đặt phòng)

```
booking-app/
  be/              -> booking-app-be (Servlet + Hibernate JPA-style, WAR, Tomcat 10.1, Java 21)
  booking-app-fe/  -> React 19 + Vite + Ant Design + Redux Toolkit + React Router
```

## Chạy BE local (không Docker)

```bash
cd be
mvn package
cp target/booking-app-be.war $TOMCAT/webapps/ROOT.war
$TOMCAT/bin/catalina.sh run
# -> http://localhost:8080/api/health
```

## Chạy BE bằng Docker

```bash
cd be
docker build -t vivu-booking-be .
docker run --rm -p 8080:8080 vivu-booking-be
```

## Chạy FE local

```bash
cd booking-app-fe
cp .env.example .env   # sửa VITE_API_BASE_URL nếu BE không ở localhost:8080
npm install
npm run dev            # http://localhost:3000
```

Đổi IP/port BE: sửa 1 chỗ `VITE_API_BASE_URL` trong `booking-app-fe/.env` (ví dụ `http://103.216.117.40:8080`).

## Chạy cả BE + FE bằng Docker Compose (khuyến nghị)

```bash
cd booking-app          # thư mục chứa docker-compose.yml
docker compose up --build
# FE http://localhost:3000  -> BE http://localhost:8080
```

Đổi IP/port khi build FE image:

```bash
docker compose build --build-arg VITE_API_BASE_URL=http://103.216.117.40:8080 fe
# hoặc sửa booking-app-fe/.env trước khi build
```

## Cấu hình

`be/src/main/resources/application.properties` chứa sẵn host/port/password cho PostgreSQL/Redis/MinIO. Có thể override bằng env var khi chạy Docker.
Bucket MinIO: tạo `vivu-bucket` qua http://103.216.117.40:9001 trước khi upload.
