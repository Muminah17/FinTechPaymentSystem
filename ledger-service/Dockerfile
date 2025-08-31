# Step 1: Use a slim JDK image
FROM eclipse-temurin:17-jdk-jammy AS build

# Set working directory
WORKDIR /app

# Copy jar into the container
COPY target/ledger-service-0.0.1-SNAPSHOT.jar app.jar

# Step 2: Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]