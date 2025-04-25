# # Use OpenJDK 17 as base image
# FROM openjdk:17-jdk-slim

# # Set working directory
# WORKDIR /app

# # Copy the JAR file
# COPY target/*.jar app.jar

# # Expose port 8080
# EXPOSE 8080

# # Run the application
# CMD ["java", "-jar", "app.jar"]

FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory and set permissions
RUN mkdir -p /app/uploads && \
    chmod -R 777 /app/uploads

ENV PORT=8080
ENV file.upload-dir=/app/uploads
EXPOSE ${PORT}
CMD ["java", "-jar", "app.jar"]