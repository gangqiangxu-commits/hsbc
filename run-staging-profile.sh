#!/bin/bash
# Run Spring Boot application with 'staging' profile
java -jar -Dspring.profiles.active=staging target/hsbc-springboot-0.0.1-SNAPSHOT.jar
