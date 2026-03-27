# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre

ENV APP_HOME=/app
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/wordai;DB_CLOSE_ON_EXIT=FALSE
ENV SPRING_H2_CONSOLE_ENABLED=false

WORKDIR ${APP_HOME}

RUN addgroup --system wordai && adduser --system --ingroup wordai wordai

COPY --from=build /workspace/target/wordai-*.jar /app/app.jar
COPY --from=build /workspace/src/main/resources/dictionaries /app/dictionaries
COPY docker/wordai.properties /app/wordai.properties

RUN mkdir -p /app/data /app/logs && chown -R wordai:wordai /app

EXPOSE 8080

USER wordai

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
