
# Use a lightweight Java 21+ base image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy built JAR (adjust path if needed)
COPY target/ai-triage-assistant-*.jar app.jar

# Expose port (matches render.yaml PORT)
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
