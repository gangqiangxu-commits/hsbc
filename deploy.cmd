@echo off
REM build and deploy hsbc project

REM Step 1: Build the hsbc jar
call mvn clean package -DskipTests -Dspring.profiles.active=prod
if %errorlevel% neq 0 (
    echo Maven build failed!
    exit /b %errorlevel%
)

REM Step 2: Rebuild the docker image, name it as hsbc-springboot-app:latest
docker build --no-cache -t hsbc-springboot-app:latest .
if %errorlevel% neq 0 (
    echo Docker build failed!
    exit /b %errorlevel%
)

REM Step 3: Tag and push the docker image to Alibaba Cloud
docker tag hsbc-springboot-app:latest crpi-72qfiuq4hwm1chuo.cn-shanghai.personal.cr.aliyuncs.com/jack-xu/hsbc:latest
if %errorlevel% neq 0 (
    echo Docker tag failed!
    exit /b %errorlevel%
)

docker push crpi-72qfiuq4hwm1chuo.cn-shanghai.personal.cr.aliyuncs.com/jack-xu/hsbc:latest
if %errorlevel% neq 0 (
    echo Docker push failed!
    exit /b %errorlevel%
)

echo Deployment completed successfully.