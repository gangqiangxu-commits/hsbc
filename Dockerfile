# Use OpenJDK 17 JRE base image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the Spring Boot JAR
COPY target/hsbc-springboot-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy the entrypoint script
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Expose the application port
EXPOSE 8080

# Use the entrypoint script
ENTRYPOINT ["/app/entrypoint.sh"]