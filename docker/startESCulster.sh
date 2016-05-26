#!/usr/bin/env bash

eval "$(docker-machine env default)"

docker run -p 9200:9200 -p 9300:9300 -v "$home/.esdata/node1":/usr/share/elasticsearch/data elasticsearch:2.3.3 -Des.node.name="node1" -Des.discovery.zen.ping.unicast.hosts=192.168.99.100

docker run -p 9201:9200 -p 9301:9300 -v "$home/.esdata/node2":/usr/share/elasticsearch/data elasticsearch:2.3.3 -Des.node.name="node2" -Des.discovery.zen.ping.unicast.hosts=192.168.99.100
