FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml ./pom.xml
COPY shared/pom.xml ./shared/pom.xml
COPY shared/src ./shared/src
COPY services/notification-service/pom.xml ./services/notification-service/pom.xml
RUN mvn dependency:go-offline -pl services/notification-service -am -B --no-transfer-progress
COPY services/notification-service/src ./services/notification-service/src
RUN mvn clean package -pl services/notification-service -am -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S trading && adduser -S trading -G trading
WORKDIR /app
COPY --from=builder /build/services/notification-service/target/notification-service-*.jar app.jar
USER trading
EXPOSE 8087
ENTRYPOINT ["java", "-XX:+UseVirtualThreads", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
