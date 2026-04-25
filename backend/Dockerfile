FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S teenyurl && adduser -S teenyurl -G teenyurl

COPY --from=build /workspace/target/*.jar app.jar

USER teenyurl

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
