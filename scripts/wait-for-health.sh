#!/bin/bash

# Wait for service health endpoint to be ready
#
# Environment variables:
#   API_URL: Base API URL (default: http://localhost:8080/api/v1)
#   HEALTH_TIMEOUT: Max seconds to wait (default: 180)
#   EXTRA_AUTH_HEADER_NAME: Auth header name (default: X-Extra-Auth)
#   EXTRA_AUTH_HEADER_SECRET: Auth header secret (default: secret)

set -e

BASE_URL="${API_URL:-http://localhost:8080/api/v1}"
TIMEOUT="${HEALTH_TIMEOUT:-180}"
EXTRA_AUTH_HEADER_NAME="${EXTRA_AUTH_HEADER_NAME:-X-Extra-Auth}"
EXTRA_AUTH_HEADER_SECRET="${EXTRA_AUTH_HEADER_SECRET:-secret}"

echo "Waiting for service at ${BASE_URL} to be ready..."
echo "Timeout: ${TIMEOUT}s"

waited=0
while ! curl -sf "${BASE_URL}/health" -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" > /dev/null 2>&1; do
    if [ $waited -ge "$TIMEOUT" ]; then
        echo ""
        echo "[ERROR] Service did not become ready after ${TIMEOUT}s"
        exit 1
    fi
    echo -n "."
    sleep 2
    waited=$((waited + 2))
done

echo ""
echo "[OK] Service is ready"
