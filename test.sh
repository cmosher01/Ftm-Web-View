#!/bin/sh

docker-compose down -v
./gradlew build
docker-compose up --no-start
docker-compose start webserver
sleep 5
docker-compose start proxy
docker logs -f ftm-web-view_webserver_1
