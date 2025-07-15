# Jupyter Everywhere Sharing Service

The sharing service is the back-end of the Jupyter Everywhere project. It is a RESTful API that allows users to store, retrieve, and share Jupyter Notebooks. The service is designed to be scalable and reliable, utilizing cloud-based storage solutions to ensure that users can access their notebooks from anywhere.

The API enhances workflows by validating notebook formatting, preventing duplication, and storing them anonymously, making it easier to integrate with other services without requiring user enrollment associations.

## Running the Service Locally

The service is built using Java + Spring Boot and Python, but it is fully containerized using Docker. To run the service, you need to have [Docker and Docker Compose](https://docs.docker.com/get-docker/) installed on your machine.

Then, with Docker running, you can start the service by running the following command in the root directory of the project:

```bash
docker compose up
```

This will start the service and all its dependencies, including a PostgreSQL database and LocalStack. The service will be available at `http://localhost:8080`.

To run a quick smoke test to verify that the service is running, you can use the provided script to hit the healthcheck endpoint, issue a token, and share and retrieve a sample notebook:

```bash
./scripts/smoke-test.sh
```

## API Documentation

A Postman collection is available for testing the API endpoints in [docs/api](docs/api). You can import the collection into Postman to explore the available endpoints and their functionalities. There is also a couple of sample `curl` commands in `docs/api/example.http`.

## Contributing

We welcome contributions to the Jupyter Everywhere project! Please read the [docs/contributing.md](docs/contributing.md) file for more information on how to get started.

## License

This project is licensed under the modified BSD license. See the [LICENSE.md](LICENSE.md) file for details.
