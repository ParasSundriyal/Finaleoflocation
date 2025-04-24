# TehriMap

A web application for managing and tracking occurrences, markers, and emergency contacts in the Tehri region.

## Features

- Interactive map with custom markers
- Occurrence reporting and management
- Emergency contact management
- Admin dashboard
- User authentication and authorization
- Real-time location tracking
- Photo upload capabilities

## Tech Stack

- Backend: Spring Boot (Java 17)
- Frontend: HTML, CSS (Tailwind CSS), JavaScript
- Database: MySQL
- Authentication: JWT
- Map Integration: Leaflet.js
- File Storage: Local file system

## Prerequisites

- Java 17 or higher
- Maven
- MySQL

## Local Development Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/tehrimap.git
cd tehrimap
```

2. Configure the database:
- Create a MySQL database
- Update `src/main/resources/application.properties` with your database credentials

3. Build and run the application:
```bash
mvn clean install
java -jar target/*.jar
```

4. Access the application at `http://localhost:8080`

## Deployment

### Render Deployment

1. Create a new Web Service on Render
2. Connect your GitHub repository
3. Configure the following:
   - Build Command: `mvn clean package -DskipTests`
   - Start Command: `java -jar target/*.jar`
   - Environment Variables:
     - `SPRING_PROFILES_ACTIVE=prod`
     - `MYSQL_URL=your_mysql_url`
     - `MYSQL_USER=your_mysql_user`
     - `MYSQL_PASSWORD=your_mysql_password`

## Environment Variables

- `SPRING_PROFILES_ACTIVE`: Application profile (dev/prod)
- `MYSQL_URL`: MySQL database URL
- `MYSQL_USER`: MySQL username
- `MYSQL_PASSWORD`: MySQL password
- `JWT_SECRET`: Secret key for JWT token generation
- `FILE_UPLOAD_DIR`: Directory for file uploads

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. 