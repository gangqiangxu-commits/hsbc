#!/bin/bash
# Run the Docker container for the app with 'prod' profile and ensure host.docker.internal is resolvable
# This script adds the required host mapping for Linux Docker containers
docker run --name hsbc-app-prod --rm -p 8080:8080 --add-host=host.docker.internal:host-gateway -e SPRING_PROFILES_ACTIVE=prod hsbc-springboot-app:latest
