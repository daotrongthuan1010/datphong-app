# ---- Stage 1: Build WAR ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Stage 2: Runtime (Tomcat 10.1 + JDK 21) ----
FROM tomcat:10.1-jdk21-temurin
LABEL maintainer="VIVU Booking"

# Remove default apps for clean deploy
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy WAR as ROOT.war so endpoints are at /api/* (not /booking-app-be/api/*)
COPY --from=build /app/target/booking-app-be.war /usr/local/tomcat/webapps/ROOT.war

# Tomcat runs as root by default; expose port
EXPOSE 8080

# Allow overriding config via env vars (Spring-style dotted keys also read from env)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

CMD ["catalina.sh", "run"]
