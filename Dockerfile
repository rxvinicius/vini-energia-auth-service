FROM maven:3.8.8-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/auth-service-0.0.1-SNAPSHOT.jar /app/auth-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/auth-service.jar"]
