FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace/backend

COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle ./gradle

RUN chmod +x ./gradlew

COPY backend/src ./src

RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

USER spring

COPY --from=build /workspace/backend/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
