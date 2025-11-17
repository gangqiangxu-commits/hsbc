@echo off
REM Run the Docker container for the app with 'staging' profile and ensure host.docker.internal is resolvable
REM This script adds the required host mapping for Linux Docker containers

docker run --name hsbc-app-staging --rm -p 8080:8080 --add-host=host.docker.internal:host-gateway -e SPRING_PROFILES_ACTIVE=staging hsbc-springboot-app:latest