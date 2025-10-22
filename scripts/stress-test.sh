#!/bin/bash

# Stress test with 10 concurrent 10MB notebook uploads
# Tests JVM memory configuration under heavy load

set -e

BASE_URL="${API_URL:-http://localhost:8080/api/v1}"
CONCURRENT_UPLOADS=10
NOTEBOOK_SIZE_KB=10000  # 10MB
EXTRA_AUTH_HEADER_NAME="${EXTRA_AUTH_HEADER_NAME:-X-Extra-Auth}"
EXTRA_AUTH_HEADER_SECRET="${EXTRA_AUTH_HEADER_SECRET:-secret}"

echo "=== 10MB Notebook Stress Test ==="
echo ""
echo "API URL: $BASE_URL"
echo "Concurrent uploads: $CONCURRENT_UPLOADS"
echo "Notebook size: ${NOTEBOOK_SIZE_KB}KB (~10MB)"
echo ""

# Wait for service to be ready
echo "Waiting for service to be ready..."
max_wait=180
waited=0
while ! curl -sf "${BASE_URL}/health" -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" > /dev/null 2>&1; do
    if [ $waited -ge $max_wait ]; then
        echo "❌ Service did not become ready after ${max_wait}s"
        exit 1
    fi
    echo -n "."
    sleep 2
    waited=$((waited + 2))
done
echo ""
echo "✅ Service is ready"
echo ""

# Get auth token
echo "Getting auth token..."
TOKEN=$(curl -sf -X POST "$BASE_URL/auth/issue" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" | jq -r '.token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "❌ Failed to get auth token"
    exit 1
fi
echo "✅ Got auth token"
echo ""

# Generate 10MB notebook once
echo "Generating ${NOTEBOOK_SIZE_KB}KB notebook..."
TARGET_BYTES=$((NOTEBOOK_SIZE_KB * 1024))
BASE_SIZE=600  # Approximate size of base notebook structure
PADDING_BYTES=$((TARGET_BYTES - BASE_SIZE))

# Generate padding efficiently using dd + tr
PADDING=$(dd if=/dev/zero bs=1 count="$PADDING_BYTES" 2>/dev/null | tr '\000' 'X')

# Create notebook JSON (stored in memory)
NOTEBOOK_JSON=$(cat <<EOF
{
  "nbformat": 4,
  "nbformat_minor": 4,
  "metadata": {
    "kernelspec": {
      "display_name": "Python 3",
      "language": "python",
      "name": "python3"
    },
    "language_info": {
      "name": "python",
      "version": "3.9.0"
    }
  },
  "cells": [
    {
      "cell_type": "code",
      "source": ["print('10MB stress test notebook')"],
      "metadata": {},
      "outputs": [],
      "execution_count": null
    },
    {
      "cell_type": "markdown",
      "source": ["Padding data: ${PADDING}"],
      "metadata": {}
    }
  ]
}
EOF
)

echo "✅ Generated notebook ($(echo "$NOTEBOOK_JSON" | wc -c | xargs) bytes)"
echo ""

# Function to upload a notebook (uses temp file to avoid "Argument list too long")
upload_notebook() {
    local id=$1
    local result
    local body
    local status
    local temp_request
    temp_request=$(mktemp)

    # Wrap notebook in request object using temp file
    echo "$NOTEBOOK_JSON" | jq -s '{password: "", notebook: .[0]}' > "$temp_request"

    result=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/notebooks" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" \
        --data-binary @"$temp_request")

    body=$(echo "$result" | sed '$d')
    status=$(echo "$result" | tail -n 1)

    rm -f "$temp_request"

    echo "$id|$status|$body"
}

echo "Starting $CONCURRENT_UPLOADS concurrent 10MB uploads..."
echo ""

# Array to store background PIDs
declare -a pids

# Clean up results file
rm -f /tmp/stress_test_results.txt

# Start concurrent uploads
for i in $(seq 1 "$CONCURRENT_UPLOADS"); do
    {
        result=$(upload_notebook "$i")
        echo "$result" >> /tmp/stress_test_results.txt
    } &
    pids+=($!)
done

# Wait for all uploads to complete
echo "Waiting for uploads to complete..."
for pid in "${pids[@]}"; do
    wait "$pid"
done

echo ""
echo "=== Results ==="
echo ""

# Parse results
success_count=0
error_count=0

while IFS='|' read -r upload_id status body; do
    if [ "$status" = "201" ]; then
        readable_id=$(echo "$body" | jq -r '.notebook.readable_id')
        echo "✅ Upload $upload_id: SUCCESS (readable_id: $readable_id)"
        ((success_count++))
    else
        echo "❌ Upload $upload_id: FAILED (status $status)"
        echo "   Response: $body"
        ((error_count++))
    fi
done < /tmp/stress_test_results.txt

echo ""
echo "=== Summary ==="
echo "Total uploads: $CONCURRENT_UPLOADS"
echo "Successful: $success_count"
echo "Failed: $error_count"
echo ""

if [ $success_count -eq $CONCURRENT_UPLOADS ]; then
    echo "✅ STRESS TEST PASSED: All $CONCURRENT_UPLOADS concurrent 10MB uploads succeeded"
    exit 0
else
    echo "❌ STRESS TEST FAILED: Some uploads did not succeed"
    exit 1
fi
