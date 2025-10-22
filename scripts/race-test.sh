#!/bin/bash

# Test script to verify race condition fix for readable_id assignment
# This script performs concurrent notebook uploads to test for duplicate key violations
#
# Environment variables:
#   NOTEBOOK_SIZE_KB: Size of test notebooks in KB (default: 10)
#                     Use larger values (e.g., 10000) for OOM stress testing

set -e

BASE_URL="${API_URL:-http://localhost:8080/api/v1}"
CONCURRENT_UPLOADS=10
NOTEBOOK_SIZE_KB="${NOTEBOOK_SIZE_KB:-10}"
EXTRA_AUTH_HEADER_NAME="${EXTRA_AUTH_HEADER_NAME:-X-Extra-Auth}"
EXTRA_AUTH_HEADER_SECRET="${EXTRA_AUTH_HEADER_SECRET:-secret}"

echo "=== Testing Race Condition Fix for Readable ID Assignment ==="
echo ""
echo "API URL: $BASE_URL"
echo "Notebook size: ${NOTEBOOK_SIZE_KB}KB"
echo "Concurrent uploads: $CONCURRENT_UPLOADS"
echo ""

# Wait for service to be ready (migrations can take a while, especially V3 which populates readable IDs)
echo "Waiting for service to be ready (this can take 2-3 minutes for migrations)..."
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

# Generate padding text to reach target notebook size
generate_padding_text() {
    local target_kb=$1
    local target_bytes=$((target_kb * 1024))

    # Base notebook structure is ~500 bytes
    # Account for JSON overhead (~20% for encoding, escaping, etc.)
    local padding_needed=$((target_bytes - 600))

    if [ $padding_needed -le 0 ]; then
        echo ""
        return
    fi

    # Use dd for fast padding generation (much faster than bash loops)
    dd if=/dev/zero bs=1 count="$padding_needed" 2>/dev/null | tr '\000' 'X'
}

# Create a test notebook payload
create_notebook_payload() {
    local id=$1
    local padding
    padding=$(generate_padding_text "$NOTEBOOK_SIZE_KB")

    # If we have padding, add it as a markdown cell
    local padding_cell=""
    if [ -n "$padding" ]; then
        # Escape the padding for JSON (replace backslash and quotes)
        local escaped_padding="${padding//\\/\\\\}"
        escaped_padding="${escaped_padding//\"/\\\"}"

        padding_cell=",
      {
        \"cell_type\": \"markdown\",
        \"source\": [\"Padding data for testing: ${escaped_padding}\"],
        \"metadata\": {}
      }"
    fi

    cat <<EOF
{
  "password": "",
  "notebook": {
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
        "source": ["print('Test notebook $id')"],
        "metadata": {},
        "outputs": [],
        "execution_count": null
      }${padding_cell}
    ]
  }
}
EOF
}

# Function to upload a notebook
upload_notebook() {
    local id=$1
    local result
    local body
    local status

    # Use stdin to avoid "Argument list too long" error with large payloads
    result=$(create_notebook_payload "$id" | curl -s -w "\n%{http_code}" -X POST "$BASE_URL/notebooks" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" \
        --data-binary @-)

    body=$(echo "$result" | sed '$d')
    status=$(echo "$result" | tail -n 1)

    echo "$status|$body"
}

echo "Starting $CONCURRENT_UPLOADS concurrent uploads..."
echo ""

# Array to store background PIDs
declare -a pids

# Clean up any existing results file
rm -f /tmp/upload_results.txt

# Start concurrent uploads
for i in $(seq 1 "$CONCURRENT_UPLOADS"); do
    {
        result=$(upload_notebook "$i")
        echo "$i|$result" >> /tmp/upload_results.txt
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
duplicate_key_count=0

while IFS='|' read -r upload_id status body; do
    if [ "$status" = "201" ]; then
        readable_id=$(echo "$body" | jq -r '.notebook.readable_id')
        echo "✅ Upload $upload_id: SUCCESS (status $status, readable_id: $readable_id)"
        ((success_count++))
    else
        echo "❌ Upload $upload_id: FAILED (status $status)"
        echo "   Response: $body"
        ((error_count++))

        # Check if it's a duplicate key error
        if echo "$body" | grep -qi "duplicate.*readable.*id"; then
            echo "   ⚠️  DUPLICATE KEY ERROR DETECTED!"
            ((duplicate_key_count++))
        fi
    fi
done < /tmp/upload_results.txt

echo ""
echo "=== Summary ==="
echo "Total uploads: $CONCURRENT_UPLOADS"
echo "Successful: $success_count"
echo "Failed: $error_count"
echo "Duplicate key errors: $duplicate_key_count"
echo ""

if [ $duplicate_key_count -gt 0 ]; then
    echo "❌ RACE CONDITION FIX FAILED: Duplicate key errors detected"
    exit 1
elif [ $success_count -eq $CONCURRENT_UPLOADS ]; then
    echo "✅ RACE CONDITION FIX VERIFIED: All concurrent uploads succeeded with unique readable IDs"
    exit 0
else
    echo "⚠️  Some uploads failed, but no duplicate key errors detected"
    exit 1
fi
