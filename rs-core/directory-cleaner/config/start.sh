#!/bin/sh
exec java $jvm_flags_global -Xmx256m -Djava.security.egd=file:/dev/./urandom -jar /app/s1pdgs-directory-cleaner.jar --spring.config.location=/app/config/application.yml
