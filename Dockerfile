# -----------------------------------------------------------------------------
# Build stage
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-ubi10-minimal AS build

# Install required build utilities
RUN microdnf update -y --refresh --best --nodocs --noplugins --setopt=install_weak_deps=0 \
 && microdnf install -y findutils which

WORKDIR /app

# Copy Gradle configuration files first for better caching
COPY gradle/ /app/gradle/
COPY build.gradle settings.gradle gradlew gradlew.bat /app/
RUN chmod +x ./gradlew

# Copy source code
COPY src/ /app/src/

# Build the application
RUN ./gradlew clean bootJar --no-daemon --console=plain


# -----------------------------------------------------------------------------
# Runtime stage
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-ubi10-minimal

# Update base image packages to patch security vulnerabilities
RUN microdnf update -y --refresh --best --nodocs --noplugins --setopt=install_weak_deps=0 \
 && microdnf remove -y binutils binutils-gold

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
    uv venv --python 3.13 "${VIRTUAL_ENV}" && \
    uv pip install nbformat

# Copy the built JAR from the build stage
ENV PYTHON_SCRIPT_PATH=/app/scripts/validate_notebook.py
COPY --from=build /app/build/libs/sharing-service-*.jar /app/sharing-service.jar
COPY --chmod=0755 src/main/java/org/jupytereverywhere/script/validate_notebook.py ${PYTHON_SCRIPT_PATH}

EXPOSE 8080

# JVM memory configuration to prevent OutOfMemoryError with large notebooks
# Heap: 1280MB max (leaves 768MB for off-heap: metaspace, direct buffers, thread stacks)
# Optimized for 2GB App Runner instances processing notebooks up to 10MB
# For 4GB instances: -Xmx2560m -Xms768m -XX:MaxMetaspaceSize=384m
ENTRYPOINT ["java", \
    "-Xmx1280m", \
    "-Xms512m", \
    "-XX:MaxMetaspaceSize=256m", \
    "-XX:NativeMemoryTracking=summary", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:G1HeapRegionSize=4m", \
    "-XX:+UseStringDeduplication", \
    "-jar", "/app/sharing-service.jar"]
