# build
FROM docker.io/library/maven:3.9.13-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM docker.io/library/eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN useradd -r appuser
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
