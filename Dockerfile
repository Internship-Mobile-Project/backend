# --- Giai đoạn 1: Build ứng dụng bằng Gradle ---
FROM gradle:jdk21-alpine AS build
WORKDIR /app
COPY . .

# Cấp quyền chạy cho file gradlew (Bước này rất quan trọng để tránh lỗi Permission denied)
RUN chmod +x ./gradlew

# Chạy lệnh build (bỏ qua test để build nhanh hơn)
RUN ./gradlew build -x test --no-daemon

# --- Giai đoạn 2: Chạy ứng dụng (Run) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar từ folder build/libs sang và đổi tên thành app.jar
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
