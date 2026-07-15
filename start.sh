#!/bin/bash
if [[ -z "${JWT_SECRET_KEY:-}" ]]; then
  echo "JWT_SECRET_KEY is required; set it to at least 32 UTF-8 bytes before starting." >&2
  exit 1
fi

./gradlew bootRun --no-daemon
