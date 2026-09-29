# ---- Build stage: Maven + JDK 21 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies: copy POM first, download, then add sources
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Run stage: JRE only, tuned for small containers ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/target/budgetbuddy-backend-0.1.0.jar app.jar

EXPOSE 8080

# Render provides PORT; default to 8080 locally. Cap heap for 512MB instances.
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar app.jar --server.port=${PORT:-8080}"]
