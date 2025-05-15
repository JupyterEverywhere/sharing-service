FROM eclipse-temurin:17-jre-ubi9-minimal

# Set the location for the Python Virtual Environment
ENV VIRTUAL_ENV=/opt/venv/sharing-service
ENV PYTHON_INTERPRETER_PATH="${VIRTUAL_ENV}/bin/python3"

WORKDIR /app

# install uv for Python management
COPY --from=ghcr.io/astral-sh/uv:latest /uv /bin/
# Enable bytecode compilation
ENV UV_COMPILE_BYTECODE=1
# Copy from the cache instead of linking since it's a mounted volume
ENV UV_LINK_MODE=copy

# install Python and dependencies
RUN --mount=type=cache,target=/root/.cache/uv \
    uv venv --python 3.13 "${VIRTUAL_ENV}" && uv pip install nbformat

# copy the built application files to the image
COPY build/libs/ckhubapi-0.0.1-SNAPSHOT.jar /app/ckhub-api.jar
COPY --chmod=0755 src/main/java/org/coursekata/script/validate_notebook.py /app/scripts/validate_notebook.py

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/ckhub-api.jar"]
