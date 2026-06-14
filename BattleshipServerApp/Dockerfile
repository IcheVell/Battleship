FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY BattleshipShared ./BattleshipShared

RUN --mount=type=cache,target=/root/.m2 mvn -f BattleshipShared/pom.xml clean install -DskipTests

COPY BattleshipServerApp/pom.xml ./BattleshipServerApp/
COPY BattleshipServerApp/src ./BattleshipServerApp/src

RUN --mount=type=cache,target=/root/.m2 mvn -f BattleshipServerApp/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/BattleshipServerApp/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]