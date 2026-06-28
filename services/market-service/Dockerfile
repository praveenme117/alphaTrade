# ── Stage 1: Build ────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven

COPY pom.xml ./pom.xml
COPY shared/pom.xml ./shared/pom.xml
COPY shared/src ./shared/src
COPY services/market-service/pom.xml ./services/market-service/pom.xml
RUN mvn dependency:go-offline -pl services/market-service -am -B --no-transfer-progress

COPY services/market-service/src ./services/market-service/src
RUN mvn clean package -pl services/market-service -am -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S trading && adduser -S trading -G trading
WORKDIR /app
COPY --from=builder /build/services/market-service/target/market-service-*.jar app.jar
USER trading
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseVirtualThreads", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
