# WebProtégé GitHub Ontology Clone Service

A Spring Boot microservice that analyzes GitHub repositories containing ontology files, extracts their change history, and transforms it into WebProtégé-compatible revision documents stored in cloud storage.

## Overview

The WebProtégé GitHub Ontology Clone Service is a specialized microservice designed to bridge the gap between Git-based ontology development and WebProtégé collaborative editing. It analyzes the evolution of ontology files (OWL, RDF, TTL) within GitHub repositories by tracking axiom-level changes across commit history, then converts this information into WebProtégé's internal revision format for seamless integration.

This service enables teams to import existing ontology projects from GitHub into WebProtégé while preserving the complete development history, making it valuable for collaborative ontology editing, research projects, and ontology maintenance workflows.

## Key Features

- **Git Repository Analysis**: Traverses GitHub repository commit history using the github-commit-navigator library
- **Ontology File Processing**: Loads and parses ontology files using the OWL API
- **Axiom-Level Change Tracking**: Identifies individual axiom additions and removals between consecutive commits
- **WebProtégé Integration**: Converts change history into WebProtégé-compatible revision documents
- **Cloud Storage**: Stores generated revision documents in MinIO-compatible object storage
- **Message-Driven Architecture**: Processes requests via RabbitMQ for scalable async operations

## Requirements

### Runtime Requirements
- **Java 17 or higher**: Required for modern language features and Spring Boot 3.x
- **Docker**: For containerized deployment (optional but recommended)
- **RabbitMQ**: Message broker for request/response handling
- **MinIO or S3-compatible storage**: For storing generated revision documents

### Build Requirements
- **Maven 3.6 or higher**: For building and dependency management
- **Docker** (optional): For building container images

## Development Commands

### Building and Testing
```bash
# Clean and compile
mvn clean compile

# Run all tests (unit + integration)
mvn test

# Run only unit tests (excludes integration tests)
mvn test -Dtest="!*Integration*"

# Build the application
mvn clean package

# Build with unit tests only (CI/CD pattern)
mvn --batch-mode clean package -Dtest="!*Integration*"

# Skip tests during build
mvn clean package -DskipTests
```

### Code Quality and Formatting
```bash
# Auto-format code with Google Java Style
mvn spotless:apply

# Check code formatting
mvn spotless:check

# Run static analysis with SpotBugs
mvn spotbugs:check
```

### Running the Application
```bash
# Run with Maven
mvn spring-boot:run
```

### Docker Operations
```bash
# Build Docker image (automatically done during package phase)
mvn clean package

# Manual Docker build
docker build -f Dockerfile --build-arg JAR_FILE=webprotege-gh-ontology-clone-service-1.0.0.jar -t protegeproject/webprotege-gh-ontology-clone-service:1.0.0 .
```

### Testing Strategy

- **Unit Tests**: Fast, isolated tests that run in CI/CD pipeline
- **Integration Tests**: Slower tests that require external dependencies (Docker containers)
  - Excluded from CI/CD using `-Dtest="!*Integration*"` pattern
  - Should be run locally during development: `mvn test`

### Local Development vs CI/CD

| Environment | Command | Tests Run | Duration |
|-------------|---------|-----------|----------|
| Local Dev | `mvn test` | All (unit + integration) | ~30-60 seconds |
| CI/CD | `mvn test -Dtest="!*Integration*"` | Unit tests only | ~10-15 seconds |