#!/usr/bin/env bash

eval "$(docker-machine env default)"

docker run -u 1000 -p 9200:9200 -p 9300:9300 -v "/Users/isaac/work/data":/usr/share/elasticsearch/data elasticsearch:2.3.3 -Des.node.name="node1"