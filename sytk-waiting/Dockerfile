# 1
FROM gradle:8.12-jdk17 AS builder
WORKDIR /app

COPY gradlew settings.gradle ./
COPY gradle ./gradle
COPY build.gradle ./
COPY sytk-waiting/build.gradle ./sytk-waiting/

RUN ./gradlew :sytk-waiting:dependencies --no-daemon

# .dockerignore 사용
COPY . .

RUN ./gradlew :sytk-waiting:bootJar -x test --no-daemon

# 2
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

COPY --from=builder --chown=app:app /app/sytk-waiting/build/libs/*.jar app.jar

USER app

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]