#!/bin/sh
exec java -Xmx1024m -Djava.security.egd=file:/dev/./urandom -jar /app/s1pdgs-compression-trigger.jar --spring.config.location=/app/config/application.yml
