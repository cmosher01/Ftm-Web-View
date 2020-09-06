#!/bin/sh -e

docker-compose down -v
./gradlew build
docker-compose pull
docker-compose build
docker-compose up --no-start
docker-compose start webserver
sleep 5
docker-compose start proxy
docker-compose logs -f
