.PHONY: help build test clean start stop restart perf-test docker-up docker-down

# Default target
help:
	@echo "Available targets:"
	@echo "  build         - Build the application with Gradle"
	@echo "  test          - Run all tests"
	@echo "  clean         - Clean build artifacts"
	@echo "  start         - Start infrastructure and run app locally"
	@echo "  stop          - Stop all services"
	@echo "  restart       - Restart all services"
	@echo "  perf-test     - Run performance test (10 iterations, 5MB notebook)"
	@echo "  docker-up     - Start all services with Docker Compose"
	@echo "  docker-down   - Stop all Docker Compose services"

# Build the application
build:
	./gradlew clean build

# Run tests
test:
	./gradlew test

# Clean build artifacts
clean:
	./gradlew clean
	rm -f scripts/test-notebook.ipynb

# Start infrastructure (db + localstack) and run app locally
start:
	@echo "Starting infrastructure services..."
	docker compose up -d db localstack
	@echo "Waiting for services to be ready..."
	@sleep 10
	@echo "Starting Spring Boot application..."
	DB_USERNAME=jupytereverywhere \
	DB_PASSWORD=jupytereverywhere \
	DB_HOST=localhost \
	DB_PORT=5433 \
	DB_NAME=sharingservice \
	AWS_ACCESS_KEY_ID=test \
	AWS_SECRET_ACCESS_KEY=test \
	AWS_REGION=us-east-1 \
	S3_BUCKET_NAME=test-bucket \
	S3_ENDPOINT_OVERRIDE=http://localhost:4566 \
	STORAGE_TYPE=s3 \
	JWT_SECRET_KEY=test-secret-key-for-local-development-only \
	./gradlew bootRun

# Stop all services
stop:
	@echo "Stopping Spring Boot application..."
	-pkill -f "gradle.*bootRun" || true
	@echo "Stopping infrastructure services..."
	docker compose down

# Restart services
restart: stop start

# Run performance test with locally running services
perf-test:
	@echo "Running performance test..."
	./scripts/performance-test.sh 10 5120

# Start all services with Docker Compose (full stack)
docker-up:
	docker compose up -d --build

# Stop Docker Compose services
docker-down:
	docker compose down -v
