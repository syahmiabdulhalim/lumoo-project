# ================================
# Stage 1: Build
# ================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
 
WORKDIR /app
 
# Copy pom.xml dulu — supaya dependency cache tak rebuild setiap kali code berubah
COPY pom.xml .
RUN mvn dependency:go-offline -q
 
# Copy source code & build
COPY src ./src
RUN mvn clean package -DskipTests -q
 
# ================================
# Stage 2: Run
# ================================
FROM eclipse-temurin:21-jre-alpine
 
WORKDIR /app
 
# Salin JAR dari stage build
COPY --from=builder /app/target/*.jar app.jar

# Non-root user for security
RUN addgroup -S lumoo && adduser -S -G lumoo lumoo \
    && mkdir -p /app/uploads/products /app/uploads/avatars /app/uploads/kyc \
    && chown -R lumoo:lumoo /app
USER lumoo

# Port Spring Boot
EXPOSE 8080

# Jalankan app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

 