FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY search-service/pom.xml search-service/pom.xml
COPY indexer-service/pom.xml indexer-service/pom.xml
RUN mvn -B -pl indexer-service -am dependency:go-offline
COPY indexer-service/src indexer-service/src
RUN mvn -B -pl indexer-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 qbalways
COPY --from=build /workspace/indexer-service/target/indexer-service-*.jar app.jar
USER qbalways
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
