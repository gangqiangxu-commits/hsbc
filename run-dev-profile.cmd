@echo off
REM Run Spring Boot application with 'dev' profile
java -jar -Dspring.profiles.active=dev target/hsbc-springboot-0.0.1-SNAPSHOT.jar
