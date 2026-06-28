# ── Stage 1: Build ────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy parent POM and shared module first (better layer caching)
COPY pom.xml ./pom.xml
COPY shared/pom.xml ./shared/pom.xml
COPY shared/src ./shared/src
COPY services/auth-service/pom.xml ./services/auth-service/pom.xml

# Download dependencies offline (cached layer)
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -pl services/auth-service -am -B --no-transfer-progress

# Copy source and build
COPY services/auth-service/src ./services/auth-service/src
RUN mvn clean package -pl services/auth-service -am -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -S trading && adduser -S trading -G trading

WORKDIR /app

# Copy built JAR
COPY --from=builder /build/services/auth-service/target/auth-service-*.jar app.jar

# Switch to non-root
USER trading

EXPOSE 8081

# Enable virtual threads (Java 21), set reasonable JVM flags
ENTRYPOINT ["java", \
            "-XX:+UseVirtualThreads", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-jar", "app.jar"]
