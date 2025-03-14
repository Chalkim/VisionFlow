FROM openjdk:17-jdk-slim

LABEL authors="chalkim"

ENV TZ=Asia/Shanghai

ENV SERVER_NAME=localhost

WORKDIR /visionflow

COPY "build/libs/VisionFlow-0.0.1-SNAPSHOT.jar" app.jar

RUN apt-get update && \
    apt-get install -y nginx openssl && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /etc/nginx/ssl

RUN openssl req -x509 -newkey rsa:4096 -keyout /etc/nginx/ssl/selfsigned.key -out /etc/nginx/ssl/selfsigned.crt -days 3650 -nodes -subj "/CN=${SERVER_NAME}"

COPY nginx.conf.template /etc/nginx/nginx.conf

CMD ["sh", "-c", "java -jar app.jar & nginx -g 'daemon off;'"]

EXPOSE 80 443
