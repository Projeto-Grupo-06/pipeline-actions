FROM ubuntu:latest AS builder
LABEL authors="DenilsonReis"

WORKDIR /build

COPY . .

RUN mvn clean package -DskipTests=true

FROM openjdk:17-slim

WORKDIR /app

COPY --from=builder /build/target/*.jar /app/app.jar

CMD ["java", "-jar", "app.jar"]
