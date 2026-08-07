FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/inventory-service-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
