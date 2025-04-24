FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create upload directory and set permissions
RUN mkdir -p /tmp/uploads && \
    chmod -R 777 /tmp/uploads

COPY --from=build /app/target/*.jar app.jar

# Set environment variables
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV file.upload-dir=/tmp/uploads

EXPOSE ${PORT}
CMD ["java", "-jar", "app.jar"]