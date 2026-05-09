# 1
FROM gradle:8.12-jdk17 AS builder
WORKDIR /app

COPY gradlew settings.gradle ./
COPY gradle ./gradle
COPY build.gradle ./
COPY sytk-payment/build.gradle ./sytk-payment/

RUN ./gradlew :sytk-payment:dependencies --no-daemon

# .dockerignore 사용
COPY . .

RUN ./gradlew :sytk-payment:bootJar -x test --no-daemon

# 2
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/sytk-payment/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]