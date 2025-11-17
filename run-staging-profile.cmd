@echo off
REM Run the Docker container for the app with 'staging' profile and ensure host.docker.internal is resolvable
REM This script adds the required host mapping for Linux Docker containers

java -jar -Dspring.profiles.active=staging target/hsbc-springboot-0.0.1-SNAPSHOT.jar
