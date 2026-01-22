# --- Giai đoạn 1: Build ứng dụng ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
# Lệnh build tạo file .jar (bỏ qua test để chạy nhanh hơn)
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Chạy ứng dụng (Run) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy file .jar từ giai đoạn 1 sang giai đoạn 2
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
