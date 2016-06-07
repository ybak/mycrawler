#!/usr/bin/env bash

eval "$(docker-machine env default)"

docker-compose -f docker-compose-es.yml up