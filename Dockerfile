# ══ STAGE 1: Build the compiled multi-layer executable ══
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ══ STAGE 2: Run the production application using a lightweight JRE image ══
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the compiled executable fat JAR asset from the build container stage
COPY --from=build /app/target/*.jar app.jar

# Create a persistent directory for locally uploaded product images
RUN mkdir -p uploads

EXPOSE 8080

# Clean entrypoint that allows Spring to safely bind configurations from Azure environment variables
ENTRYPOINT ["java", "-jar", "app.jar"]