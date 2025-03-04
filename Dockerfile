FROM ubuntu:24.04

LABEL authors="chalkim"

ENV TZ=Asia/Shanghai

RUN apt update

# install java 17
RUN apt install -y openjdk-17-jdk

# install gstreamer
RUN apt install -y gstreamer1.0-tools \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad \
    gstreamer1.0-plugins-ugly

ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /visionflow
COPY "build/libs/VisionFlow-0.0.1-SNAPSHOT.jar" app.jar
CMD ["java", "-jar", "app.jar"]

EXPOSE 8080
