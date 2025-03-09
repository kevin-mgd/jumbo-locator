# Jumbo Store Locator

A REST API application that finds the 5 closest Jumbo stores to a given position based on geographic coordinates.

## Overview

This Spring Boot application provides a RESTful API to:
- Find the nearest Jumbo stores to a given latitude and longitude position
- Retrieve detailed information about a specific store by its ID

## Technologies

- Kotlin 2.1.0
- Spring Boot 3.4.3
- PostgreSQL with PostGIS extension for spatial data
- Flyway for database migrations
- Spring Data JPA with Hibernate Spatial
- OpenAPI/Swagger for API documentation
- Docker Compose for local development
- Arrow-kt for functional programming constructs

## Prerequisites

To build and run this application, you need:
- JDK 21
- Docker and Docker Compose
- Git

## Running the Application

### Running the Application

1. Clone the repository
   ```
   git clone https://github.com/kevin-mgd/jumbo-locator 
   cd jumbo-store-locator
   ```

2. Build and run the application
   ```
   ./gradlew bootRun
   ```

   The application uses Spring Boot Docker Compose integration (`spring-boot-docker-compose`) which will automatically start the required Docker containers.

### Using IntelliJ IDEA or other IDE

1. Import the project as a Gradle project
2. Run the main application class `JumboLocatorApplication.kt`

   Spring Boot will automatically manage the Docker containers.

## API Endpoints

### Find Nearest Stores

```
GET /api/v1/stores/nearest?latitude={lat}&longitude={lng}&limit={limit}
```

Parameters:
- `latitude`: Latitude coordinate (between -90 and 90)
- `longitude`: Longitude coordinate (between -180 and 180)
- `limit`: Maximum number of stores to return (1-20, default: 5)

### Get Store Details

```
GET /api/v1/stores/{storeId}
```

Parameters:
- `storeId`: The unique identifier of the store

## API Documentation

Once the application is running, you can access the Swagger UI documentation at:
```
http://localhost:8980/swagger-ui.html
```

## Database

The application uses PostgreSQL with PostGIS extension for efficient geographic queries. The database schema is managed with Flyway migrations.

## Configuration

The application configuration is defined in `application.yml` and can be overridden using environment variables or command-line arguments.

## Testing

Run the tests using:
```
./gradlew test
```

The tests use TestContainers to spin up a PostgreSQL instance with PostGIS for integration testing.
