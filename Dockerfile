FROM registry.geohub.space/wo7/repo-maven-all:latest as build
WORKDIR /app
COPY pom.xml /app
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
COPY build/ /app/build/
COPY src/ /app/src/
COPY config/ /app/config/
COPY test /app/test/
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml package

FROM openjdk:8-jre-alpine
WORKDIR /app
RUN apk update && apk add wget
COPY --from=build /app/target/s1pdgs-archives-1.0.0.jar /app/s1pdgs-archives.jar
COPY /config/start.sh start.sh
ENTRYPOINT "/app/start.sh"
