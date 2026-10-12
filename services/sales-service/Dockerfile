# Build: Maven Wrapper com JDK 25
FROM eclipse-temurin:25.0.4.1_1-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml lombok.config ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/sales-service.jar extract --destination /workspace/extracted

# Runtime: JRE 25 enxuta, usuário não root
FROM eclipse-temurin:25.0.4.1_1-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/extracted ./
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8082
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=12 \
    CMD curl -fsS http://localhost:8082/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "sales-service.jar"]
