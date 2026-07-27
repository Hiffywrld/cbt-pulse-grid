FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/examination-service-0.0.1-SNAPSHOT.jar /app/examination-service.jar
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8082/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/examination-service.jar"]