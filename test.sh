#!/bin/sh -e

docker-compose down -v
docker-compose pull
docker-compose build --no-cache
docker-compose up --no-start
docker-compose start webserver
sleep 5
docker-compose start proxy
docker-compose logs -f
