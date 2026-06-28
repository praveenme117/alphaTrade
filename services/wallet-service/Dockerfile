FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml ./pom.xml
COPY shared/pom.xml ./shared/pom.xml
COPY shared/src ./shared/src
COPY services/wallet-service/pom.xml ./services/wallet-service/pom.xml
RUN mvn dependency:go-offline -pl services/wallet-service -am -B --no-transfer-progress
COPY services/wallet-service/src ./services/wallet-service/src
RUN mvn clean package -pl services/wallet-service -am -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S trading && adduser -S trading -G trading
WORKDIR /app
COPY --from=builder /build/services/wallet-service/target/wallet-service-*.jar app.jar
USER trading
EXPOSE 8085
ENTRYPOINT ["java", "-XX:+UseVirtualThreads", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
