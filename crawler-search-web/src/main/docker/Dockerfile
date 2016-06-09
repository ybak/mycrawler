FROM frolvlad/alpine-oraclejdk8
VOLUME /tmp
ADD crawler-search-web-1.0-SNAPSHOT.war app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]