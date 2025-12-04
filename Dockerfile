FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR (workflow copies build/libs/*.jar to the server)
# Use a wildcard to match the jar name and normalize to app.jar
COPY *.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
