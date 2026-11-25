# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY contract/pom.xml contract/pom.xml
COPY agent/pom.xml agent/pom.xml
COPY server/pom.xml server/pom.xml
COPY client/pom.xml client/pom.xml
# Центральное приложение (сервер). Клиент JavaFX в образ не входит.
RUN mvn -pl server -am -B -q dependency:go-offline -DskipTests || true
COPY contract contract
COPY agent agent
COPY server server
COPY client client
RUN mvn -pl server -am -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -r -u 10001 ofd
COPY --from=build /src/server/target/ofd-marking-server-*.jar /app/app.jar
USER ofd
EXPOSE 18443
ENV SERVER_PORT=18443 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
