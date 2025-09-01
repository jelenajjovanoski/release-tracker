
# Release Tracker

Release Tracker is a Spring Boot application for tracking software releases throughout their lifecycle.
It provides endpoints to create, update, delete, and retrieve releases with validation and filtering.


## Features

- POST create a new release
- GET list and filter releases (by name, status, release date range)
- GET get a single release by ID
- PUT update existing release
- DELETE delete a release
- Validation (unique release name, valid statuses, release date ≥ today)
- Swagger/OpenAPI documentation

## Domain & Validation Rules

### Status
- Releases are stored in the database with enum values:
  - `CREATED`
  - `IN_DEVELOPMENT`
  - `ON_DEV`
  - `QA_DONE_ON_DEV`
  - `ON_STAGING`
  - `QA_DONE_ON_STAGING`
  - `ON_PROD`
  - `DONE`

- API requests/responses use labels (case-sensitive):
  - "Created"
  - "In Development"
  - "On DEV"
  - "QA Done on DEV"
  - "On STAGING"
  - "QA Done on STAGING"
  - "On PROD"
  - "Done"

### Release Request Validation
- `name` is **required** (max length 255 characters)
- `status` is **required**
- `description` is optional (max length 5000 characters)
- `releaseDate` is optional but:
  - Must follow format `yyyy-MM-dd` if provided
  - Must be today or a future date
  - If status = **Done** and `releaseDate` is not provided → it will be set automatically to today’s date

### Timestamps
- `createdAt` is set automatically when a release is created
- `lastUpdateAt` is updated automatically each time a release is modified  


## Tech Stack

- Java 17
- Spring Boot 3 (Web, Validation, Data JPA)
- PostgreSQL (database)
- Liquibase (database migrations)
- Docker & Docker Compose
- JUnit 5 & Mockito (unit testing)
- Testcontainers (integration testing with PostgreSQL)
- Maven (build tool)
- Swagger/OpenAPI (API docs)

## Project Structure

```
src/main/java/io/github/jelenajjovanoski/releasetracker
 ├── controller/    # REST API endpoints
 ├── service/       # Business logic
 ├── repository/    # Database access
 ├── dto/           # Request/Response models
 ├── model/         # Entities and enums
 ├── mapper/        # Entity <-> DTO mapping
 └── exception/     # Custom exception classes

docker/             # Docker-related configuration
 ├── Dockerfile
 ├── docker-compose.yml
 └── .env.example

```

## Running the Application

### Local (H2 in-memory database)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Docker (Postgres + application)
Run the following command from inside the `docker/` folder:
```bash
docker-compose up --build
```

## Configuration

Environment variables are defined in the `.env.example` file inside the `docker/` folder.  
Before running the application with Docker, copy this file to `.env` and adjust values as needed.

Application will be available at: 

http://localhost:8080/swagger-ui

http://localhost:8080/v3/api-docs


## Testing

### Run all tests (unit + integration)
Using Maven Wrapper:
```bash
./mvnw clean install
```
Or, if Maven is installed locally:
```bash
mvn clean install
```

- Unit tests with JUnit 5 & Mockito  
- Integration tests with Testcontainers + PostgreSQL  



## API Example

### Create a Release  
**POST** `/api/v1/releases`

Request:
```json
{
  "name": "Release 1.0",
  "description": "Initial release",
  "status": "CREATED",
  "releaseDate": "2025-09-15"
}
```

Response:
```json
{
  "id": "b1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Release 1.0",
  "description": "Initial release",
  "status": "CREATED",
  "releaseDate": "2025-09-15",
  "createdAt": "2025-09-01T18:00:00Z",
  "lastUpdateAt": "2025-09-01T18:00:00Z"
}
```

## Database Migrations

- Managed with Liquibase  
- Migration scripts are located in:  
  `src/main/resources/db/changelog`  


## Swagger

- Swagger UI: http://localhost:8080/swagger-ui  
- OpenAPI JSON: http://localhost:8080/v3/api-docs    
