# NOTE: This file is used to build local container images for development only. Do not get overzealous and use it in production.
FROM azul/zulu-openjdk:17.0.0

COPY build/libs/kafka-consumer-throughput-*-all.jar /app/app.jar

WORKDIR /app

ENTRYPOINT ["java" , "-jar",  "/app/app.jar"]