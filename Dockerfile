# NOTE: This file is used to build local container images for testing only.
FROM azul/zulu-openjdk:19.0.2-19.32.13

COPY build/libs/kafka-consumer-performance-*-all.jar /app/app.jar

WORKDIR /app

ENTRYPOINT ["java" , "-jar",  "/app/app.jar"]
