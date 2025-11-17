@echo off
REM Run Spring Boot application with 'test' profile
java -jar -Dspring.profiles.active=test target/hsbc-springboot-0.0.1-SNAPSHOT.jar
