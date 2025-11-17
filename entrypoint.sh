#!/bin/sh

# Set default profile to 'staging' if not provided
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-staging}

# Start the Spring Boot application with the selected profile
exec java -jar /app/app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE