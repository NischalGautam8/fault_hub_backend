# Leaders Fault — Backend

Small Spring Boot backend for the notification/fault system. This README explains how to run the project locally using Docker Compose or Maven, and how to provide required environment variables via a `.env` file.

## Prerequisites

- Docker & Docker Compose
- Java JDK 17+ (or the version defined in `pom.xml`)
- Maven
- A PostgreSQL database (the repo includes docker-compose that can provide one)

## Quick start

1. Create a local `.env` file (do NOT commit secrets to git). See "Environment" below for required variables.
2. Start services with Docker Compose (recommended for full environment):
```bash
docker compose up -d
```
This will start any services defined in `docker-compose.yml` (database, etc). After the services are up, the backend can be started with Maven.

3. Build with Maven:
```bash
mvn clean install
```

4. Run the Spring Boot application:
```bash
mvn spring-boot:run
```
Or run the built jar:
```bash
java -jar target/*.jar
```

## Environment

Create a `.env` at the project root with the following variables (placeholders shown). The application reads configuration from `src/main/resources/application.yml`, which uses these environment variables. Replace placeholder values with your real secrets/URLs.

```bash
# JWT / auth
JWT_SECRET=your-jwt-secret
JWT_PRIVATE_KEY_BASE64=base64-encoded-rs256-private-key
JWT_PUBLIC_KEY_BASE64=base64-encoded-rs256-public-key

# OIDC / server
SERVER_URL=http://localhost:8080

# Database (Postgres)
DB_URL=jdbc:postgresql://host:5432/dbname?sslmode=require
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# Cloudinary (optional)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Azure Application Insights (optional)
APPINSIGHTS_INSTRUMENTATIONKEY=your_instrumentation_key
```

Important: keep `.env` out of version control. Add it to `.gitignore` if not already present.

## Docker

- Build images (if a Dockerfile is present and build is required by docker-compose):
```bash
docker compose build
```
- Start containers:
```bash
docker compose up -d
```
- Tail logs:
```bash
docker compose logs -f
```
- Stop and remove:
```bash
docker compose down
```

## Maven

- Run tests and build:
```bash
mvn clean install
```
- Start app:
```bash
mvn spring-boot:run
```


## Useful commands

```bash
docker compose up -d
mvn clean install
mvn spring-boot:run
