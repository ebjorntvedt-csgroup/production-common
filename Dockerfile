FROM maven:3.5.3-jdk-8

WORKDIR /app
COPY * /app/

#RUN mvn -B -f /tmp/pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
RUN mvn -B -f /app/pom.xml -s /usr/share/maven/ref/settings-docker.xml package

#ENTRYPOINT ["/usr/local/bin/mvn-entrypoint.sh"]
#CMD ["mvn"]
