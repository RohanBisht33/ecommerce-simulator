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

# -XX:TieredStopAtLevel=1 skips C2 JIT compilation during startup — trades a small
# amount of steady-state throughput for meaningfully faster cold boot, which is
# what actually matters for passing Azure's container startup probe.
# -Xshare:auto lets the JVM use Class Data Sharing if a shared archive is present,
# and falls back gracefully if it isn't (safe default, no extra build step required).
# -XX:+UseSerialGC reduces GC-thread setup overhead at boot; fine for a single-core
# App Service plan, revisit if you move to a plan with more cores under real load.
ENTRYPOINT ["java", \
            "-XX:TieredStopAtLevel=1", \
            "-Xshare:auto", \
            "-XX:+UseSerialGC", \
            "-jar", "app.jar"]