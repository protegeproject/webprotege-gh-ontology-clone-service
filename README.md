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

## Workflow

1. **Request Processing**: Receives GitHub repository analysis requests via RabbitMQ
2. **Repository Navigation**: Traverses Git commit history backwards from HEAD
4. **Change Detection**: Compares consecutive ontology versions to identify axiom-level differences
5. **Revision Generation**: Converts changes into WebProtégé revision document format
6. **Storage**: Stores the generated revision document in MinIO cloud storage
7. **Response**: Returns storage location and metadata via RabbitMQ response queue

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

# Run tests
mvn test

# Build the application
mvn clean package

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