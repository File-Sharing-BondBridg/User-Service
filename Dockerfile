FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ADD https://dtdg.co/latest-java-tracer dd-java-agent.jar

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV DD_ENV=staging \
    DD_SERVICE=user-service \
    DD_VERSION=1.0.0 \
    DD_TRACE_ENABLED=true \
    DD_LOGS_INJECTION=true

ENTRYPOINT ["java", "-javaagent:/app/dd-java-agent.jar", "-jar", "/app/app.jar"]
