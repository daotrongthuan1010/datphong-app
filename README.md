# booking-app — VIVU (Dat phong)

```
booking-app/
  be/ -> booking-app-be (Servlet + Hibernate JPA-style, WAR, Tomcat 10.1, Java 21)
  fe/ -> frontend (tu chon, goi API be)
```

## Chay BE local (khong Docker)
```bash
cd be
mvn package
cp target/booking-app-be.war $TOMCAT/webapps/ROOT.war
$TOMCAT/bin/catalina.sh run
# -> http://localhost:8080/api/health
```

## Chay BE bang Docker
```bash
cd be
docker build -t vivu-booking-be .
docker run --rm -p 8080:8080 vivu-booking-be
```

## Cau hinh
`be/src/main/resources/application.properties` chua san host/port/password cho PostgreSQL/Redis/MinIO. Co the override bang env var khi chay Docker.
Bucket MinIO: tao `vivu-bucket` qua http://103.216.117.40:9001 truoc khi upload.
