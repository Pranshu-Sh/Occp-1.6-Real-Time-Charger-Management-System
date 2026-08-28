# --- Build stage ---
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-jammy
RUN groupadd -r ocpp && useradd -r -g ocpp ocpp
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER ocpp

EXPOSE 9093
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
