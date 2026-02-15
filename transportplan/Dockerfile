# ===== transportplan/Dockerfile =====
# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Install dos2unix to handle Windows line endings (CRLF to LF)
RUN apt-get update && apt-get install -y dos2unix && rm -rf /var/lib/apt/lists/*

# Copy the entire project context
COPY . .

# Fix line endings and permissions for the Gradle wrapper
RUN dos2unix ./gradlew && chmod +x ./gradlew

# Build the transportplan module
RUN ./gradlew :transportplan:bootJar -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /workspace/transportplan/build/libs/*.jar app.jar

# gRPC Port
EXPOSE 9093

# JVM options with UTF-8 encoding
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]