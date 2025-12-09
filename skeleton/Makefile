# Makefile for ${{values.name}}
# Common developer commands

.PHONY: help build test run clean docker-build docker-run deps-up deps-down lint security-check quality all

# Default target
.DEFAULT_GOAL := help

# Variables
APP_NAME := ${{values.name}}
JAVA_VERSION := ${{values.javaVersion | default("21", true)}}
DOCKER_IMAGE := $(APP_NAME):latest

## help: Show this help message
help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@grep -E '^## ' $(MAKEFILE_LIST) | sed 's/## /  /'

## build: Compile the application
build:
	./mvnw clean package -DskipTests

## build-all: Compile with tests and quality checks
build-all:
	./mvnw clean verify

## test: Run unit tests
test:
	./mvnw test

## test-integration: Run integration tests
test-integration:
	./mvnw verify -Pintegration-tests

## test-all: Run all tests with coverage report
test-all:
	./mvnw clean verify jacoco:report
	@echo "Coverage report: target/site/jacoco/index.html"

## run: Run the application locally
run:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local

## run-debug: Run with debug enabled (port 5005)
run-debug:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

## clean: Clean build artifacts
clean:
	./mvnw clean
	rm -rf target/
	rm -rf .gradle/

## docker-build: Build Docker image
docker-build:
	docker build -t $(DOCKER_IMAGE) .

## docker-run: Run Docker container
docker-run: docker-build
	docker run --rm -p 8080:8080 -p 8081:8081 \
		-e SPRING_PROFILES_ACTIVE=local \
		$(DOCKER_IMAGE)

## deps-up: Start local dependencies (databases, caches, etc.)
deps-up:
	docker-compose up -d
	@echo "Waiting for services to be healthy..."
	@sleep 5
	docker-compose ps

## deps-down: Stop local dependencies
deps-down:
	docker-compose down

## deps-logs: View dependency logs
deps-logs:
	docker-compose logs -f

## deps-clean: Stop and remove all local dependency data
deps-clean:
	docker-compose down -v

## lint: Run code style checks
lint:
	./mvnw checkstyle:check

## lint-fix: Auto-format code (using Spotless if configured)
lint-fix:
	./mvnw spotless:apply || echo "Spotless not configured, skipping auto-format"

## security-check: Run security vulnerability scans
security-check:
	./mvnw dependency-check:check
	@echo "Security report: target/dependency-check-report.html"

## spotbugs: Run SpotBugs static analysis
spotbugs:
	./mvnw spotbugs:check

## pmd: Run PMD static analysis
pmd:
	./mvnw pmd:check

## quality: Run all code quality checks
quality: lint spotbugs pmd
	@echo "All quality checks passed!"

## all: Build, test, and run quality checks
all: clean build-all quality
	@echo "Full build completed successfully!"

## deps-tree: Show dependency tree
deps-tree:
	./mvnw dependency:tree

## deps-updates: Check for dependency updates
deps-updates:
	./mvnw versions:display-dependency-updates

## api-docs: Open API documentation
api-docs: run
	@echo "API docs available at: http://localhost:8080/swagger-ui.html"

## pre-commit: Install pre-commit hooks
pre-commit:
	pre-commit install
	pre-commit run --all-files

## sbom: Generate Software Bill of Materials
sbom:
	./mvnw cyclonedx:makeAggregateBom
	@echo "SBOM generated: target/bom.json"

## version: Show current version
version:
	@./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout

## release-prepare: Prepare for release (update version, run checks)
release-prepare:
	@echo "Running full quality checks..."
	$(MAKE) all
	@echo "Ready for release!"

# Development shortcuts
dev: deps-up run
dev-docker: deps-up docker-run
restart: deps-down deps-up run
