# Generic build for any module in this Gradle multi-module monorepo.
# Usage: docker build --build-arg SERVICE_MODULE=core-service -t smartteca/core-service .

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
COPY common/build.gradle common/build.gradle
COPY core-service/build.gradle core-service/build.gradle
COPY search-service/build.gradle search-service/build.gradle
COPY storage-service/build.gradle storage-service/build.gradle
COPY notification-service/build.gradle notification-service/build.gradle
COPY chatbot-service/build.gradle chatbot-service/build.gradle
COPY api-gateway/build.gradle api-gateway/build.gradle

ARG SERVICE_MODULE
RUN chmod +x gradlew && ./gradlew :${SERVICE_MODULE}:dependencies --configuration runtimeClasspath -q || true

COPY . .
RUN chmod +x gradlew && ./gradlew :${SERVICE_MODULE}:bootJar -x test

FROM eclipse-temurin:21-jre
ARG SERVICE_MODULE
ENV SERVICE_MODULE=${SERVICE_MODULE}
WORKDIR /app
COPY --from=build /workspace/${SERVICE_MODULE}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
