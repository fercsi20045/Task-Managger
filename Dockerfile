# --- Build stage ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Függőségek előre letöltése (cache optimalizálás)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/taskmanager-1.0.0.jar app.jar

# Frontend statikus fájlok
COPY frontend /app/static

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
