FROM openjdk:17-jdk-slim

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-venv && \
    python3 -m venv /opt/venv/ckhubapi && \
    /opt/venv/ckhubapi/bin/pip install --no-cache-dir --upgrade pip nbformat && \
    rm -rf /var/lib/apt/lists/*

COPY build/libs/ckhubapi-0.0.1-SNAPSHOT.jar /app/ckhub-api.jar
COPY src/main/java/org/coursekata/script/validate_notebook.py /app/scripts/validate_notebook.py

RUN chmod +x /app/scripts/validate_notebook.py

ENV PYTHON_INTERPRETER_PATH=/opt/venv/ckhubapi/bin/python

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/ckhub-api.jar"]
