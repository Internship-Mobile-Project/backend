
FROM gradle:8.5-jdk21-alpine AS build

WORKDIR /app

# Copy file Gradle trước (tối ưu cache Docker)
COPY --chown=gradle:gradle build.gradle settings.gradle gradlew ./
COPY --chown=gradle:gradle gradle/ gradle/

# Tải Gradle dependencies (cache)
RUN ./gradlew --no-daemon dependencies || true

# Copy toàn bộ source code
COPY --chown=gradle:gradle . .

# Build JAR (bỏ test cho nhanh)
RUN ./gradlew clean bootJar --no-daemon -x test


# ============================
# 🚀 STAGE 2 — RUN APP
# ============================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy file jar từ stage build
COPY --from=build /home/gradle/app/build/libs/*.jar app.jar

# Expose port trong container
EXPOSE 8080

# Chạy app
ENTRYPOINT ["java", "-jar", "app.jar"]
