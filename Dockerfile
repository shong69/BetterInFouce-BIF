FROM gradle:8.11.1-jdk17-alpine AS builder

WORKDIR /app

ENV GRADLE_USER_HOME=/home/gradle/.gradle

COPY settings.gradle build.gradle ./
COPY gradle gradle

RUN --mount=type=cache,target=/home/gradle/.gradle gradle dependencies --no-daemon

COPY src src
RUN --mount=type=cache,target=/home/gradle/.gradle gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup

RUN apk add --no-cache tzdata curl
ENV TZ=Asia/Seoul

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
