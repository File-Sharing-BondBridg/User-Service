# Stage 1: Build the application
FROM gradle:8.14.3-jdk17-alpine AS build
WORKDIR /home/gradle/src

# Copy Gradle files first (for better layer caching)
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Build the JAR (skip tests if desired with -x test)
RUN ./gradlew bootJar -x test

# Stage 2: Create runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Download Datadog Java agent
ADD https://dtdg.co/latest-java-tracer dd-java-agent.jar

# Copy the built JAR from the previous stage
# Adjust the filename if needed (common patterns: *.jar, app-name-*.jar, or !*-plain.jar to exclude plain JAR)
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Datadog environment variables
ENV DD_ENV=staging \
    DD_SERVICE=user-service \
    DD_VERSION=1.0.0 \
    DD_TRACE_ENABLED=true \
    DD_LOGS_INJECTION=true \
    DD_AGENT_HOST=datadog-agent \
    DD_TRACE_AGENT_PORT=8126

ENTRYPOINT ["java", "-javaagent:/app/dd-java-agent.jar", "-jar", "/app/app.jar"]