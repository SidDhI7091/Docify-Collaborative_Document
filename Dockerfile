# ─────────────────────────────────────────────
# Stage 1 – Build
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build (skip tests here; tests run in CI)
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2 – Runtime (minimal JRE image)
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Security: run as non-root user
RUN addgroup -S docify && adduser -S docify -G docify
USER docify

WORKDIR /app

# Copy compiled jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose app port
EXPOSE 8080

# Health check for Docker / orchestrators
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
