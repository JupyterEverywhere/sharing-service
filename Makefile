.PHONY: help build clean test start stop restart docker-up docker-down smoke-test stress-test

#==========================================
# Help (Default Target)
#==========================================

help:
	@echo "Available targets:"
	@echo ""
	@echo "Build & Test:"
	@echo "  build         - Build the application with Gradle"
	@echo "  clean         - Clean build artifacts"
	@echo "  test          - Run unit and integration tests"
	@echo ""
	@echo "Local Development:"
	@echo "  start         - Start infrastructure (db+localstack) and run app locally"
	@echo "  stop          - Stop infrastructure and local app"
	@echo "  restart       - Restart local services"
	@echo ""
	@echo "Docker Operations:"
	@echo "  docker-up     - Start full stack with Docker Compose"
	@echo "  docker-down   - Stop all Docker Compose services"
	@echo ""
	@echo "Integration Testing:"
	@echo "  smoke-test    - Run smoke tests against running services"
	@echo "  stress-test   - Start services and run comprehensive stress test battery"

#==========================================
# Build & Test
#==========================================

build:
	./gradlew clean build

clean:
	./gradlew clean
	rm -f scripts/test-notebook.ipynb

test:
	./gradlew test

#==========================================
# Local Development
#==========================================

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
	AWS_S3_REGION=us-east-1 \
	AWS_S3_BUCKET=test-bucket \
	AWS_S3_ENDPOINT_OVERRIDE=http://localhost:4567 \
	AWS_S3_ACCESS_KEY=test \
	AWS_S3_SECRET_KEY=test \
	STORAGE_TYPE=s3 \
	JWT_SECRET_KEY=test-secret-key-for-local-development-only \
	./gradlew bootRun

stop:
	@echo "Stopping Spring Boot application..."
	-pkill -f "gradle.*bootRun" || true
	@echo "Stopping infrastructure services..."
	docker compose down

restart: stop start

#==========================================
# Docker Operations
#==========================================

docker-up:
	docker compose up -d --build

docker-down:
	docker compose down

#==========================================
# Integration Testing
#==========================================

# Detect if API_URL is set to a remote (non-localhost) URL
# If remote, skip docker-up; if local or unset, run docker-up
IS_REMOTE_URL := $(shell \
	if [ -z "$(API_URL)" ]; then \
		echo "false"; \
	elif echo "$(API_URL)" | grep -qE "localhost|127\.0\.0\.1|0\.0\.0\.0"; then \
		echo "false"; \
	else \
		echo "true"; \
	fi)

wait-for-health:
ifeq ($(IS_REMOTE_URL),false)
	@echo "Testing local service - starting Docker stack..."
	@$(MAKE) docker-up
endif
	@echo
	@API_URL="$(API_URL)" ./scripts/wait-for-health.sh

smoke-test: wait-for-health
	@echo
	@API_URL="$(API_URL)" ./scripts/smoke-test.sh

stress-test: wait-for-health
	@echo
	@API_URL="$(API_URL)" CONTAINER_ID=$$(docker compose ps -q api) ./scripts/stress-test-battery.sh
