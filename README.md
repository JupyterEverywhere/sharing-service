# CKHub API

## Introduction

The CKHub API is designed to provide a secure and efficient way to manage various educational documents, particularly Jupyter Notebooks, related to data science and statistics. The primary goal is to facilitate the storage, validation, and management of these documents, ensuring scalability and reliability by utilizing cloud-based storage solutions. The API enhances workflows by validating notebook formatting, preventing duplication, and storing them anonymously, making it easier to integrate with other services without requiring user enrollment associations.

## Prerequisites

- **Java 17+**
- ***Python 3+***
- **Docker**
- **Gradle**
- **direnv**
- **Secrets file** in `.aws-secrets/jupyter-s3.json` containing S3 credentials for local development.

### S3 Credentials Example

Create a file at `.aws-secrets/jupyter-s3.json` with the following content:

```json
{
  "access_key": "FAKE_JUPYTER_ACCESS_KEY",
  "secret_key": "FAKE_JUPYTER_SECRET_KEY",
  "url": "FAKE_JUPYTER_SECRET_KEY"
}
```

## Docker Setup

The CKHub API utilizes its own PostgreSQL database. For making this possible we use a Docker container that implements a postgres:15-alpine image. 

### Docker Compose Configuration
You can find the docker configuration in the docker-compose.yml file.


### Starting the Docker Services

To start the PostgreSQL container use this command in your terminal:

```bash
docker-compose up
```

Or to run it in a detached mode (runs containers in the background):

```bash
docker-compose up -d
```

You can also stop this service using:

```bash
docker-compose down
```

### IMPORTANT

Ensure that the PostgreSQL database is fully initialized before starting the Spring Boot application. You can verify the readiness by checking the container logs:

```bash
docker logs ckhubapi-postgres-db
```

Look for a message indicating that PostgreSQL is ready to accept connections.

## Data Migration
Flyway is integrated to manage database migrations, ensuring that the database schema is always up-to-date with the application's codebase.

### Flyway Configuration
Flyway is already configured in the project. Migration scripts are located in src/main/resources/db/migration. Ensure that all migrations are correctly versioned and applied.

### Adding Necessary Extensions
Some migrations require specific PostgreSQL extensions. For example, to use uuid_generate_v4(), the uuid-ossp extension must be created in the database. This is handled within Flyway migration scripts.



## Database connection

To connect to the local database using a terminal command or IDE, you can use the following credentials:

```sql
DATABASE=ckhubapi
PORT=5433
USERNAME=ckhub
PASSWORD=ckhub
```

## Create a Python virtual env

We need Python in this project to run a script that uses the nbformat library, ensuring that each Jupyter notebook is valid.

1. In your home directory execute the following commands, to create a virtual environment. 

```bash
mkdir envs 
python3 -m venv envs/ckhubapi
source envs/ckhubapi/bin/activate
pip install nbformat
deactivate
```

2. Create a `.envrc` file in the project. 

3. Export the following variable 
```bash
export PYTHON_INTERPRETER_PATH= PATH
```
4. Replace PATH with the location of the /bin directory of your virtual environment. It should look like this:
`/Users/youruser/envs/ckhubapi/bin/`

5. Test your variable was created using the command  `echo $PYTHON_INTERPRETER_PATH`. 

6. If the terminal does not return a value, execute: `direnv allow`


## Run Secrets Script
It is recommended to run the secrets setup script before starting the application. Add the jupyter-s3.json file to the root of the project in the .aws-secrets/ directory. This secret file is necessary to mock AWS S3 locally for testing.

```bash
./init-local-stack.sh
```

## Run the API

```bash
./start.sh
```

or by default:

```bash
./gradlew bootRun
```

If something is wrong, or you are receiving any exception try to solve this with:

1. Install gradle in your machine, you can use brew install gradle command
2. Run gradle wrapper command in the project root.
```bash
gradle wrapper
```
3. Run ./gradlew clean command
```bash
gradle clean
```
4. Then you can use ./gradlew bootRun or ./start.sh command to start the project normally.


## Run the Tests

You can run the test by using this command:

```bash
./gradlew test
```

you can also run this to generate a coverage report:

```bash
./gradlew test jacocoTestReport
```

## API Documentation

You can have a description of the API in the oficial Postman Workspace. Request permissions to access here:

https://coursekata.postman.co/workspace/Coursekata-Workspace~53361d14-07d1-4d64-b2b1-ffb4f863018b/collection/29284750-84b30f20-5147-45ab-ab40-6eaefaf276c6?action=share&creator=29284750
