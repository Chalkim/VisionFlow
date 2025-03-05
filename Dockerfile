FROM openjdk:17-jdk-slim

LABEL authors="chalkim"

ENV TZ=Asia/Shanghai

WORKDIR /visionflow

COPY "build/libs/VisionFlow-0.0.1-SNAPSHOT.jar" app.jar

CMD ["java", "-jar", "app.jar"]

EXPOSE 8080
