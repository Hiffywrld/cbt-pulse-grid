FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/identity-service-0.0.1-SNAPSHOT.jar /app/identity-service.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/identity-service.jar"]
