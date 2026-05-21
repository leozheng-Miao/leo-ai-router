FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY leo-ai-router-sdk/pom.xml leo-ai-router-sdk/pom.xml
COPY leo-ai-router-sdk/src leo-ai-router-sdk/src
RUN mvn -f leo-ai-router-sdk/pom.xml -DskipTests install

COPY pom.xml .
COPY src src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --create-home --home-dir /app leo

COPY --from=build /workspace/target/leo-ai-router-backend-0.0.1-SNAPSHOT.jar /app/app.jar

USER leo

EXPOSE 8123

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
