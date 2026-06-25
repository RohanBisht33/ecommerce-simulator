FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 🚀 Stage 2: Run the compiled application using a lightweight JRE image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create a permanent folder inside the container for our H2 file database
RUN mkdir -p data

EXPOSE 8080

# Run the app and point the H2 database to our data directory
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:h2:file:./data/nexstore"]