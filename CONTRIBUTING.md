# Contributing Guide

## Development Environment

### Prerequisites

Install the following applications:

- [Pixi](https://pixi.sh/dev/): for package management
- [`direnv`](https://direnv.net): for environment variable management

### Installation

1. Clone or download this repository.
2. Navigate to the project root directory.
3. Make a copy of the `.envrc.example` file and rename it to `.envrc`. These variables are used to configure the environment.
4. Create a file at `.aws-secrets/jupyter-s3.json` with the following content:

   ```json
   {
     "access_key": "FAKE_JUPYTER_ACCESS_KEY",
     "secret_key": "FAKE_JUPYTER_SECRET_KEY",
     "url": "FAKE_JUPYTER_SECRET_KEY"
   }
   ```

5. Install and build the application with Pixi: `pixi run build`.

This will install the following dependencies before building the application:

- [Java 17+](https://openjdk.org)
- [Gradle](https://gradle.org)
- [Python 3](https://www.python.org)
- [`awslocal`](https://github.com/localstack/awscli-local)

## Run the Application

Use the pre-configured Pixi `tasks` to run the application. The following tasks are available:

- `pixi run build`: Build the application. This will initialize LocalStack, ensure Gradle dependencies are installed, and then build the application.
- `pixi run start`: Start the application. This will use Docker Compose to start the PostgreSQL database and then start the Spring Boot application.
  - `pixi run start-services`: Start the database and other services. This will start the PostgreSQL database and other services needed for the application, but will **not** start the Spring Boot application. After running this task, you can start the Spring Boot application with `./start.sh`.
- `pixi run stop`: Stop the application. This will stop the PostgreSQL database and the Spring Boot application.

### Activating the Environment

If you are doing something other than using the Pixi commands you need to ensure that your environment is properly activated. Every time you start a new terminal session, you need to initialize the environment. This is done by running the following two commands in the project root directory:

```bash
# activate the virtual environment
pixi shell

# load the environment variables
direnv allow
```

> [!IMPORTANT]
> Always sure you have the Pixi environment activated before running any commands. If you ever run `direnv allow` without the Pixi environment activated, you will need to run `direnv reload` to reload the environment variables after you activate the Pixi environment with `pixi shell`.

## Database

> [!IMPORTANT]
> PostgreSQL must be fully initialized before starting the Spring Boot application. The Pixi scripts utilize Docker Compose to manage the PostgreSQL database, and Docker Compose ensures that the database is fully initialized (via health checks) before starting the API container.

### Connection

To connect to the local database using a terminal command or IDE, you can use the following credentials:

```sql
DATABASE=ckhubapi
PORT=5433
USERNAME=ckhub
PASSWORD=ckhub
```

### Migrations

Flyway is integrated to manage database migrations, ensuring that the database schema is always up-to-date with the application's codebase.

### Flyway Configuration

Flyway is already configured in the project. Migration scripts are located in `src/main/resources/db/migration`. Ensure that all migrations are correctly versioned and applied.

### Adding Necessary Extensions

Some migrations require specific PostgreSQL extensions. For example, to use `uuid_generate_v4()`, the `uuid-ossp` extension must be created in the database. This is handled within Flyway migration scripts.

## Testing and Coverage

The project uses JUnit 5 for testing and Jacoco for coverage. To run the tests and generate a coverage report, use the following commands (make sure you have activated the Pixi environment with `pixi shell`):

**Test**:

```bash
./gradlew test
```

**Coverage**:

```bash
./gradlew jacocoTestReport
```

## Helpful Links

### Reference Documentation

For further reference, please consider the following sections:

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

- [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
