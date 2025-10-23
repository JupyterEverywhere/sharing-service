#!/bin/bash

# Memory leak test for JSON schema validator
# Tests for memory leaks by repeatedly validating notebooks against a running container
# The old version (1.5.3) should show memory growth, while the fixed version (1.5.9) should remain stable
#
# Usage:
#   ./scripts/memory-leak-test.sh <container_id_or_name>
#
# Example:
#   ./scripts/memory-leak-test.sh sharing-service-api-1
#   ./scripts/memory-leak-test.sh $(docker compose ps -q api)

set -e

# Check for required container argument
if [ $# -eq 0 ]; then
    echo "Error: Container ID or name required"
    echo ""
    echo "Usage: $0 <container_id_or_name>"
    echo ""
    echo "Example:"
    echo "  $0 sharing-service-api-1"
    echo "  $0 \$(docker compose ps -q api)"
    exit 1
fi

CONTAINER_ID="$1"
BASE_URL="${API_URL:-http://localhost:8080/api/v1}"
ITERATIONS="${ITERATIONS:-100}"
NOTEBOOK_SIZE_KB="${NOTEBOOK_SIZE_KB:-100}"  # Smaller notebooks, more iterations
EXTRA_AUTH_HEADER_NAME="${EXTRA_AUTH_HEADER_NAME:-X-Extra-Auth}"
EXTRA_AUTH_HEADER_SECRET="${EXTRA_AUTH_HEADER_SECRET:-secret}"

# Verify container exists (by ID or name)
CONTAINER_NAME=""
if docker ps -q -f id="$CONTAINER_ID" | grep -q .; then
    # Found by ID
    CONTAINER_NAME=$(docker ps --format '{{.Names}}' -f id="$CONTAINER_ID")
elif docker ps -q -f name="$CONTAINER_ID" | grep -q .; then
    # Found by name
    CONTAINER_NAME="$CONTAINER_ID"
    CONTAINER_ID=$(docker ps -q -f name="$CONTAINER_ID")
else
    echo "ERROR: Container '$CONTAINER_ID' not found"
    exit 1
fi

# Check if container is running
if ! docker ps -q -f id="$CONTAINER_ID" -f status=running | grep -q .; then
    echo "ERROR: Container '$CONTAINER_NAME' ($CONTAINER_ID) is not running"
    exit 1
fi

echo "=== Memory Leak Test ==="
echo ""
echo "Container: $CONTAINER_NAME ($CONTAINER_ID)"
echo "Iterations: $ITERATIONS"
echo "Notebook size: ${NOTEBOOK_SIZE_KB}KB"
echo ""

# Wait for service to be ready
echo "Waiting for service to be ready..."
max_wait=30
waited=0
while ! curl -sf "${BASE_URL}/health" -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" > /dev/null 2>&1; do
    if [ $waited -ge $max_wait ]; then
        echo "ERROR: Service did not become ready after ${max_wait}s"
        exit 1
    fi
    echo -n "."
    sleep 2
    waited=$((waited + 2))
done
echo ""
echo "SUCCESS: Service is ready"
echo ""
CONTAINER_NAME=$(docker ps --format '{{.Names}}' -f id="$CONTAINER_ID")

# Get initial memory usage
INITIAL_MEM=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" | awk '{print $1}')
echo "Initial memory usage: $INITIAL_MEM"
echo ""

# Get auth token
echo "Getting auth token..."
TOKEN=$(curl -sf -X POST "$BASE_URL/auth/issue" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" | jq -r '.token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "ERROR: Failed to get auth token"
    exit 1
fi
echo "SUCCESS: Got auth token"
echo ""

# Generate test notebook
echo "Generating ${NOTEBOOK_SIZE_KB}KB notebook..."
TARGET_BYTES=$((NOTEBOOK_SIZE_KB * 1024))
BASE_SIZE=600
PADDING_BYTES=$((TARGET_BYTES - BASE_SIZE))

PADDING=$(dd if=/dev/zero bs=1 count="$PADDING_BYTES" 2>/dev/null | tr '\000' 'X')

NOTEBOOK_JSON=$(cat <<EOF
{
  "nbformat": 4,
  "nbformat_minor": 5,
  "metadata": {
    "kernelspec": {
      "display_name": "Python 3",
      "language": "python",
      "name": "python3"
    },
    "language_info": {
      "name": "python",
      "version": "3.9.0",
      "codemirror_mode": {
        "name": "ipython",
        "version": 3
      },
      "file_extension": ".py",
      "mimetype": "text/x-python"
    }
  },
  "cells": [
    {
      "id": "test-cell-001",
      "cell_type": "code",
      "source": ["print('Memory leak test notebook')"],
      "metadata": {},
      "outputs": [],
      "execution_count": null
    },
    {
      "id": "test-cell-002",
      "cell_type": "markdown",
      "source": ["Padding: ${PADDING}"],
      "metadata": {}
    }
  ]
}
EOF
)

echo "SUCCESS: Generated notebook ($(echo "$NOTEBOOK_JSON" | wc -c | xargs) bytes)"
echo ""

# Array to store memory samples
declare -a memory_samples_mb

# Upload function
upload_notebook() {
    local temp_request
    temp_request=$(mktemp)
    echo "$NOTEBOOK_JSON" | jq -s '{password: "", notebook: .[0]}' > "$temp_request"

    result=$(curl -sf -X POST "$BASE_URL/notebooks" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" \
        --data-binary @"$temp_request")

    rm -f "$temp_request"
    echo "$result"
}

echo "Starting $ITERATIONS upload iterations (with memory monitoring)..."
echo ""

success_count=0
fail_count=0
sample_interval=5  # Sample memory every N iterations

for i in $(seq 1 "$ITERATIONS"); do
    if result=$(upload_notebook 2>&1); then
        ((success_count++))
    else
        echo "ERROR: Upload $i failed"
        ((fail_count++))

        # Check if container is still running by name (handles restarts)
        current_id=$(docker ps -q -f name="$CONTAINER_NAME")
        if [ -z "$current_id" ]; then
            echo "ERROR: Container '$CONTAINER_NAME' no longer exists!"
            echo ""
            echo "=== Last Known Logs ==="
            docker logs "$CONTAINER_ID" 2>&1 | tail -n 50
            exit 1
        elif [ "$current_id" != "$CONTAINER_ID" ]; then
            echo "WARNING: Container '$CONTAINER_NAME' was restarted (OOM killed?)"
            echo "   Old ID: $CONTAINER_ID"
            echo "   New ID: $current_id"
            echo ""
            echo "=== Exit Code Check ==="
            # Check last container with this name for exit code 137 (OOM)
            exit_code=$(docker inspect "$CONTAINER_ID" --format='{{.State.ExitCode}}' 2>/dev/null || echo "unknown")
            if [ "$exit_code" = "137" ]; then
                echo "SUCCESS: Confirmed: Container was OOM killed (exit code 137)"
                echo "   OOM occurred after $success_count successful uploads"
            else
                echo "Exit code: $exit_code"
            fi
            echo ""
            echo "=== Container Logs Before Restart ==="
            docker logs "$CONTAINER_ID" 2>&1 | tail -n 50
            exit 1
        fi
    fi

    # Sample memory periodically
    if [ $((i % sample_interval)) -eq 0 ]; then
        mem_usage=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" | awk '{print $1}')
        mem_mb="${mem_usage%MiB}"
        memory_samples_mb+=("$mem_mb")

        # Show progress with memory
        echo "Progress: $i/$ITERATIONS uploads | Memory: $mem_usage | Success: $success_count | Failed: $fail_count"

        # Check if memory is approaching limit (if limit is numeric)
        if [[ "$MEMORY_LIMIT" =~ ^[0-9]+[mg]$ ]]; then
            mem_limit_mb="${MEMORY_LIMIT%[mg]}"
            if (( $(echo "$mem_mb > $mem_limit_mb * 0.95" | bc -l) )); then
                echo "WARNING: WARNING: Memory usage approaching limit ($mem_usage / $MEMORY_LIMIT)"
            fi
        fi
    fi
done

echo ""
echo "=== Final Memory Check ==="

# Get final memory usage
FINAL_MEM=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" | awk '{print $1}')
echo "Initial memory: $INITIAL_MEM"
echo "Final memory:   $FINAL_MEM"
echo ""

# Calculate memory growth
if [ ${#memory_samples_mb[@]} -gt 0 ]; then
    first_sample=${memory_samples_mb[0]}
    last_index=$((${#memory_samples_mb[@]} - 1))
    last_sample=${memory_samples_mb[$last_index]}
    growth=$(echo "scale=2; $last_sample - $first_sample" | bc)

    echo "Memory samples collected: ${#memory_samples_mb[@]}"
    echo "First sample: ${first_sample}MiB"
    echo "Last sample:  ${last_sample}MiB"
    echo "Growth:       ${growth}MiB"
    echo ""

    # Check for excessive memory growth (>100MB for this test)
    if (( $(echo "$growth > 100" | bc -l) )); then
        echo "WARNING: WARNING: Significant memory growth detected (${growth}MiB)"
        echo "This may indicate a memory leak"
    fi
fi

echo "=== Summary ==="
echo "Total iterations: $ITERATIONS"
echo "Successful:       $success_count"
echo "Failed:           $fail_count"
echo ""

# Idle monitoring to check if GC cleans up memory
IDLE_DURATION="${IDLE_DURATION:-120}"  # Default 2 minutes
if [ "$IDLE_DURATION" -gt 0 ]; then
    echo "=== Idle Memory Monitoring ==="
    echo "Waiting ${IDLE_DURATION}s to observe GC behavior..."
    echo ""

    idle_start_mem=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" | awk '{print $1}')
    idle_start_mb="${idle_start_mem%MiB}"

    declare -a idle_samples_mb
    idle_samples_mb+=("$idle_start_mb")

    # Sample every 30 seconds during idle period
    sample_count=$((IDLE_DURATION / 30))
    for i in $(seq 1 "$sample_count"); do
        sleep 30
        idle_mem=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" | awk '{print $1}')
        idle_mem_mb="${idle_mem%MiB}"
        idle_samples_mb+=("$idle_mem_mb")

        elapsed=$((i * 30))
        echo "Idle ${elapsed}s: ${idle_mem}"
    done

    echo ""
    echo "=== GC Behavior Analysis ==="
    idle_end_index=$((${#idle_samples_mb[@]} - 1))
    idle_end_mb=${idle_samples_mb[$idle_end_index]}
    idle_change=$(echo "scale=2; $idle_end_mb - $idle_start_mb" | bc)

    echo "Memory at idle start: ${idle_start_mb}MiB"
    echo "Memory at idle end:   ${idle_end_mb}MiB"
    echo "Change during idle:   ${idle_change}MiB"
    echo ""

    if (( $(echo "$idle_change < -10" | bc -l) )); then
        echo "SUCCESS: Memory decreased during idle (GC working properly)"
    elif (( $(echo "$idle_change > 10" | bc -l) )); then
        echo "WARNING: Memory increased during idle period"
    else
        echo "INFO: Memory remained stable (may indicate leak if elevated)"
    fi
    echo ""
fi

if [ "$success_count" -eq "$ITERATIONS" ]; then
    echo "SUCCESS: MEMORY LEAK TEST PASSED"
    echo "All uploads succeeded without OOM"
    exit 0
else
    echo "ERROR: MEMORY LEAK TEST FAILED"
    echo "Some uploads failed or service was OOM killed"
    exit 1
fi
