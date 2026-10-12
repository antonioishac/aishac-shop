# Build: Maven Wrapper com JDK 25
FROM eclipse-temurin:25.0.4.1_1-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/config-server.jar extract --destination /workspace/extracted

# Runtime: JRE 25 enxuta, usuário não root
FROM eclipse-temurin:25.0.4.1_1-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/extracted ./
USER app
ENV CONFIG_REPO_LOCATION=file:/config-repo/ \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8888
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=12 \
    CMD curl -fsS http://localhost:8888/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "config-server.jar"]
