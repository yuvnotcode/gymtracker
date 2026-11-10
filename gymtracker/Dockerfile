FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build /workspace/target/gymtracker-0.0.1-SNAPSHOT.jar /app/gymtracker.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/gymtracker.jar"]
