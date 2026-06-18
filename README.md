# User Profile Service

A Spring Boot microservice for managing user profiles in the Job Seeker Copilot application.

## Features

- CRUD operations for user profiles
- RESTful API endpoints
- Exception handling with custom exceptions
- Integration and unit tests

## Technology Stack

- Java 17+
- Spring Boot
- Maven
- JUnit 5

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Running the Application

```bash
mvn spring-boot:run
```

### Running Tests

```bash
mvn test
```

## API Endpoints

- `GET /api/profiles/{id}` - Get user profile by ID
- `POST /api/profiles` - Create new user profile
- `PUT /api/profiles/{id}` - Update user profile
- `DELETE /api/profiles/{id}` - Delete user profile

## Project Structure

```
src/
├── main/
│   ├── java/com/jobseekercopilot/userprofileservice/
│   │   ├── controller/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
│   │   └── service/
│   └── resources/
└── test/
    └── java/com/jobseekercopilot/userprofileservice/
        ├── controller/
        └── service/
```

## License

MIT