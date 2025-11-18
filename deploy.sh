#!/bin/bash
# build and deploy hsbc project

# Step 1: Build the hsbc jar
mvn clean package -DskipTests -Dspring.profiles.active=prod
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit $?
fi

# Step 2: Rebuild the docker image, name it as hsbc-springboot-app:latest
docker build --no-cache -t hsbc-springboot-app:latest .
if [ $? -ne 0 ]; then
    echo "Docker build failed!"
    exit $?
fi

# Step 3: Tag and push the docker image to Alibaba Cloud
docker tag hsbc-springboot-app:latest crpi-72qfiuq4hwm1chuo.cn-shanghai.personal.cr.aliyuncs.com/jack-xu/hsbc:latest
if [ $? -ne 0 ]; then
    echo "Docker tag failed!"
    exit $?
fi

docker push crpi-72qfiuq4hwm1chuo.cn-shanghai.personal.cr.aliyuncs.com/jack-xu/hsbc:latest
if [ $? -ne 0 ]; then
    echo "Docker push failed!"
    exit $?
fi

echo "Deployment completed successfully."
