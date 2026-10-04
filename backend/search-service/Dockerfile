FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY search-service/pom.xml search-service/pom.xml
COPY indexer-service/pom.xml indexer-service/pom.xml
RUN mvn -B -pl search-service -am dependency:go-offline
COPY search-service/src search-service/src
RUN mvn -B -pl search-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 qbalways
COPY --from=build /workspace/search-service/target/search-service-*.jar app.jar
USER qbalways
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
