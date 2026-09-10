FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh mvnw -B -ntp dependency:go-offline
COPY src src
RUN sh mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S smartcare && adduser -S smartcare -G smartcare
WORKDIR /app
COPY --from=build /workspace/target/smartcare-backend-*.jar app.jar
USER smartcare
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
