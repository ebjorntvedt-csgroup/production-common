FROM registry.geohub.space/wo7/repo-maven-external:master as build
WORKDIR /app
COPY pom.xml /app
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
COPY src/ /app/src/
COPY dev/ /app/dev/
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml package

FROM openjdk:8-jre-alpine
RUN mkdir -p /app/libs/spdgs-sdk/commons/0.0.1
COPY --from=build /app/pom.xml /app/libs/spdgs-sdk/commons/0.0.1/commons-0.0.1.pom
COPY --from=build /app/target/commons-0.0.1.jar /app/libs/spdgs-sdk/commons/0.0.1/commons-0.0.1.jar
ENTRYPOINT /bin/sh