# syntax=docker/dockerfile:1.7

# ---- build stage ----
# Compiles the Spring Boot fat jar against the project's pom.xml. The dependency
# layer is cached separately from the source so day-to-day code edits don't
# re-download the world.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package && \
    mv target/*.jar target/app.jar

# ---- runtime stage ----
# JRE-only image, non-root user, healthcheck against the Actuator endpoint at
# the configured context path (/api). Listens on 8080.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S hmis && adduser -S hmis -G hmis
COPY --from=build /workspace/target/app.jar /app/app.jar
RUN chown -R hmis:hmis /app
USER hmis

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=8 \
    CMD wget -q -O- http://localhost:8080/api/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
