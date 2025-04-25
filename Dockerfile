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
ENV PORT=8080
EXPOSE ${PORT}
CMD ["java", "-jar", "app.jar"]