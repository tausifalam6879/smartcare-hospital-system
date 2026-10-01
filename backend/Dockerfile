FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh mvnw -B -ntp dependency:go-offline
COPY src src
RUN sh mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S smartcare && adduser -S smartcare -G smartcare
WORKDIR /app
COPY --from=build /workspace/target/smartcare-backend-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]

# Explicit staging target. A mounted persistent volume must separately grant
# this user's write access; image ownership cannot change a mounted volume.
FROM runtime AS staging
RUN mkdir -p /var/lib/smartcare/private-documents \
    && chown smartcare:smartcare /var/lib/smartcare/private-documents
ENV SPRING_PROFILES_ACTIVE=staging
ENV SMARTCARE_DOCUMENT_STORAGE_PATH=/var/lib/smartcare/private-documents
USER smartcare

# Preserve the existing local volume behavior until its ownership is migrated.
FROM runtime AS local
