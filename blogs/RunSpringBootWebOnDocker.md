
mvn package docker:build

docker run -p 8080:8080 -t ybak/crawler-search-web

spring multi module docker No qualifying bean of type 问题解决.

参考:
https://spring.io/guides/gs/spring-boot-docker/