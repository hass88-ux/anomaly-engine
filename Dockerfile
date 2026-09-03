# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
COPY frontend ./frontend
RUN mvn clean package -DskipTests -B

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/Anomaly_Engine-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]