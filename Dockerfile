# Etapa de Build
FROM maven:3.8.8-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia o arquivo pom.xml e instala as dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código-fonte e compila o projeto
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de Execução
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o arquivo .jar da etapa de build
COPY --from=build /app/target/auth-service-0.0.1-SNAPSHOT.jar /app/auth-service.jar

# Porta em que o serviço será exposto
EXPOSE 8080

# Comando para iniciar o Spring Boot
ENTRYPOINT ["java", "-jar", "/app/auth-service.jar"]
