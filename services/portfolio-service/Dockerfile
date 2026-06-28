FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml ./pom.xml
COPY shared/pom.xml ./shared/pom.xml
COPY shared/src ./shared/src
COPY services/portfolio-service/pom.xml ./services/portfolio-service/pom.xml
RUN mvn dependency:go-offline -pl services/portfolio-service -am -B --no-transfer-progress
COPY services/portfolio-service/src ./services/portfolio-service/src
RUN mvn clean package -pl services/portfolio-service -am -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S trading && adduser -S trading -G trading
WORKDIR /app
COPY --from=builder /build/services/portfolio-service/target/portfolio-service-*.jar app.jar
USER trading
EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseVirtualThreads", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
