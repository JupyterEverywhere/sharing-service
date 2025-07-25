# Contributing to the JupyterEverywhere Sharing Service

## Introduction

The JupyterEverywhere Sharing Service is designed to provide a secure and efficient way to manage various educational documents, particularly Jupyter Notebooks, related to data science and statistics. The primary goal is to facilitate the storage, validation, and management of these documents, ensuring scalability and reliability by utilizing cloud-based storage solutions. The API enhances workflows by validating notebook formatting, preventing duplication, and storing them anonymously, making it easier to integrate with other services without requiring user enrollment associations.

## Development and Branching Strategy

This project follows git flow branching strategy. The main branches are:

- `main`: This is the main branch where the stable code resides. All production-ready code should be merged into this branch.
- `develop`: This is the development branch where all the new features and bug fixes are merged before they are released to production. This branch should always be in a deployable state.
- `feature/*`: These branches are used for developing new features. They should be branched off from `develop` and merged back into `develop` when the feature is complete.
- `hotfix/*`: These branches are used for fixing critical bugs in production. They should be branched off from `main` and merged back into both `main` and `develop` when the bug is fixed.
- `support/*`: These branches are used for supporting older versions of the code. They should be branched off from `main` and merged back into `main` when the support is no longer needed.

## Prerequisites

Note that there are two ways to get set up to contribute to this project. You can either run the entire service and infrastructure using Docker, or you can run the infrastructure using Docker while you iterate on the Java code.

In both cases, you will need to have the following prerequisites installed on your machine:

- **Java 17+**
- **Gradle**
- [**uv**](https://docs.astral.sh/uv/#installation)
- [**Docker and Docker Compose**](https://docs.docker.com/get-docker/)

## Fully Containerized Deployment

If you want to run the entire service, including the Java code, you can use Docker. This is the full E2E setup, which includes the Java code, LocalStack, and PostgreSQL.

```bash
docker compose up --build
```

This command will build the Docker images and start the containers. The service will be available at `http://localhost:8080`, the PostgreSQL database will be available at `localhost:5433`, and LocalStack will be available at `http://localhost:4566`. See more about the database connection in the [Database](#database) section.

## Local Development

If you want to develop and iterate on the Java code locally you need to do a little extra legwork to get the Python and Java code to work together. The following instructions will guide you through the setup process.

1. Start just the infrastructure (PostgreSQL and LocalStack) using Docker

   ```bash
   docker compose up --build db localstack
   ```

2. Use a tool like [`direnv`](https://direnv.net/) or just export environment variables that will specify where the Python environment is setup. You can modify `VIRTUAL_ENV` as you wish, the default is `.venv` in the root of the project. The `PYTHON_INTERPRETER_PATH` should point to the Python interpreter in the virtual environment.

   ```bash
   export VIRTUAL_ENV=.venv
   export PYTHON_INTERPRETER_PATH=$VIRTUAL_ENV/bin/python
   ```

3. Create the virtual environment and install the required Python packages. You can do this by running the following command:

   ```bash
   uv venv $VIRTUAL_ENV
   uv pip install nbformat
   ```

4. Start the Java service using Gradle. You can do this directly, but there is a pre-configured script here:

   ```bash
   ./start.sh
   ```

### Troubleshooting Gradle

If something is wrong, or you are seeing exceptions try to solve this by cleaning Gradle cache and re-running the project.

1. Install `gradle` in your machine, you can use `brew install gradle` on MacOS
2. Create the Gradle wrapper by running the following command in the root directory of the project:

   ```bash
   gradle wrapper
   ```

3. Clean the Gradle cache by running the following command:

   ```bash
   gradle clean
   ```

4. Then you can use `./gradlew bootRun` or `./start.sh` again to start the Java service.

### Run the Tests

You can run the test by using this command:

```bash
./gradlew test
```

you can also run this to generate a coverage report:

```bash
./gradlew test jacocoTestReport
```

## API Documentation

A Postman collection is available for testing the API endpoints in [docs/api](docs/api). You can import the collection into Postman to explore the available endpoints and their functionalities. There is also a couple of sample `curl` commands in `docs/api/example.http`.

## Database

### Database connection

To connect to the local database using a terminal command or IDE, you can use the following credentials:

```sql
USERNAME=jupytereverywhere
PASSWORD=jupytereverywhere
DATABASE=sharingservice
PORT=5433
```

### Data Migration

Flyway is integrated to manage database migrations, ensuring that the database schema is always up-to-date with the application's codebase.

#### Flyway Configuration

Flyway is already configured in the project. Migration scripts are located in src/main/resources/db/migration. Ensure that all migrations are correctly versioned and applied.

#### Adding Necessary Extensions

Some migrations require specific PostgreSQL extensions. For example, to use `uuid_generate_v4()`, the uuid-ossp extension must be created in the database. This is handled within Flyway migration scripts.

## Developer links and references

For further reference, please consider the following documentation:

- [Official Gradle documentation](https://docs.gradle.org)
- [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.3.3/gradle-plugin)
- [Create an OCI image](https://docs.spring.io/spring-boot/3.3.3/gradle-plugin/packaging-oci-image.html)
- [Spring Web](https://docs.spring.io/spring-boot/docs/3.3.3/reference/htmlsingle/index.html#web)
- [Spring Data JPA](https://docs.spring.io/spring-boot/docs/3.3.3/reference/htmlsingle/index.html#data.sql.jpa-and-spring-data)

### Guides

The following guides illustrate how to use some features concretely:

- [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
- [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
- [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
- [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

### Additional Links

These additional references should also help you:

- [Gradle Build Scans - insights for your project's build](https://scans.gradle.com#gradle)
