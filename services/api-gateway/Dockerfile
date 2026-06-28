# ── Stage 1: Build ────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

RUN apk add --no-cache maven

COPY pom.xml ./pom.xml
COPY shared/pom.xml ./shared/pom.xml
COPY shared/src ./shared/src
COPY services/api-gateway/pom.xml ./services/api-gateway/pom.xml

RUN mvn dependency:go-offline -pl services/api-gateway -am -B --no-transfer-progress

COPY services/api-gateway/src ./services/api-gateway/src
RUN mvn clean package -pl services/api-gateway -am -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S trading && adduser -S trading -G trading

WORKDIR /app
COPY --from=builder /build/services/api-gateway/target/api-gateway-*.jar app.jar
USER trading

EXPOSE 8080

ENTRYPOINT ["java", \
            "-XX:+UseVirtualThreads", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
