FROM eclipse-temurin:17-jre-alpine

ADD https://dtdg.co/latest-java-tracer dd-java-agent.jar

VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV DD_ENV=dev \
    DD_SERVICE=auth-service \
    DD_VERSION=1.0.0 \
    DD_TRACE_ENABLED=true \
    DD_AGENT_HOST=datadog-agent \
    DD_TRACE_AGENT_PORT=8126

ENTRYPOINT ["java", "-javaagent:/dd-java-agent.jar", "-jar", "/app.jar"]
