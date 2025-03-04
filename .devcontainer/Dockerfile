FROM mcr.microsoft.com/devcontainers/java:1-17-bookworm
LABEL authors="chalkim"

ENV TZ=Asia/Shanghai

RUN apt update
RUN apt install -y gstreamer1.0-tools \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad \
    gstreamer1.0-plugins-ugly
