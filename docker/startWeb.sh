#!/usr/bin/env bash

eval "$(docker-machine env default)"

docker run -p 80:8080 -t ybak/crawler-search-web