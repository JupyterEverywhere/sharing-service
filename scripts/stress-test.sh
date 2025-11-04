#!/bin/bash

# Stress test script
# Tests notebook uploads in configurable batches (sequential or concurrent)
# Optional: Monitor container memory usage during tests
#
# Environment variables:
#   BATCH_SIZE: Number of concurrent uploads per batch (default: 10)
#   NUM_BATCHES: Number of batches to process sequentially (default: 1)
#   NOTEBOOK_SIZE_KB: Size of test notebooks in KB (default: 10)
#   CONTAINER_ID: Docker container ID/name for memory monitoring (optional)
#   IDLE_DURATION: Seconds to monitor memory after test (default: 0, requires CONTAINER_ID)
#   MEMORY_SAMPLE_INTERVAL: Sample memory every N batches (default: 5, requires CONTAINER_ID)
#
# Examples:
#   Sequential:  BATCH_SIZE=1 NUM_BATCHES=100 ./scripts/stress-test.sh
#   Concurrent:  BATCH_SIZE=10 NUM_BATCHES=1 ./scripts/stress-test.sh
#   Mixed:       BATCH_SIZE=5 NUM_BATCHES=20 ./scripts/stress-test.sh
#   With memory: CONTAINER_ID=$(docker compose ps -q api) ./scripts/stress-test.sh

set -e

# Cleanup function
cleanup() {
    # shellcheck disable=SC2317
    rm -f /tmp/batch_upload_results.txt
}
trap cleanup EXIT INT TERM

# Parse memory value and convert to MiB (numeric only)
# Handles both MiB and GiB units
parse_memory_to_mib() {
    local mem_str="$1"
    local value
    local unit

    # Extract numeric value and unit
    value=$(echo "$mem_str" | grep -oE '[0-9.]+')
    unit=$(echo "$mem_str" | grep -oE '[A-Za-z]+')

    # Convert to MiB
    if [ "$unit" = "GiB" ]; then
        # Convert GiB to MiB (multiply by 1024)
        if command -v bc >/dev/null 2>&1; then
            echo "scale=2; $value * 1024" | bc
        else
            # Fallback without bc
            echo "$value * 1024" | awk '{printf "%.2f", $1 * $3}'
        fi
    elif [ "$unit" = "MiB" ]; then
        echo "$value"
    else
        # Unknown unit, return as-is
        echo "$value"
    fi
}

BASE_URL="${API_URL:-http://localhost:8080/api/v1}"
BATCH_SIZE="${BATCH_SIZE:-10}"
NUM_BATCHES="${NUM_BATCHES:-1}"
NOTEBOOK_SIZE_KB="${NOTEBOOK_SIZE_KB:-10}"
EXTRA_AUTH_HEADER_NAME="${EXTRA_AUTH_HEADER_NAME:-X-Extra-Auth}"
EXTRA_AUTH_HEADER_SECRET="${EXTRA_AUTH_HEADER_SECRET:-secret}"
CONTAINER_ID="${CONTAINER_ID:-}"
IDLE_DURATION="${IDLE_DURATION:-0}"
MEMORY_SAMPLE_INTERVAL="${MEMORY_SAMPLE_INTERVAL:-5}"

TOTAL_UPLOADS=$((BATCH_SIZE * NUM_BATCHES))
MEMORY_MONITORING=false

echo "=== Batch Upload Test ==="
echo ""
echo "API URL: $BASE_URL"
echo "Batch size: $BATCH_SIZE concurrent uploads"
echo "Number of batches: $NUM_BATCHES"
echo "Total uploads: $TOTAL_UPLOADS"
echo "Notebook size: ${NOTEBOOK_SIZE_KB}KB"

# Verify container if memory monitoring requested
if [ -n "$CONTAINER_ID" ]; then
    CONTAINER_NAME=""
    CONTAINER_CHECK=$(docker ps -q -f id="$CONTAINER_ID" 2>/dev/null)
    if [ -n "$CONTAINER_CHECK" ]; then
        # Found by ID
        CONTAINER_NAME=$(docker ps --format '{{.Names}}' -f id="$CONTAINER_ID")
    else
        CONTAINER_CHECK=$(docker ps -q -f name="$CONTAINER_ID" 2>/dev/null)
        if [ -n "$CONTAINER_CHECK" ]; then
            # Found by name
            CONTAINER_NAME="$CONTAINER_ID"
            CONTAINER_ID=$(docker ps -q -f name="$CONTAINER_ID")
        else
            echo "[ERROR] Container '$CONTAINER_ID' not found"
            exit 1
        fi
    fi

    # Check if container is running
    RUNNING_CHECK=$(docker ps -q -f id="$CONTAINER_ID" -f status=running 2>/dev/null)
    if [ -z "$RUNNING_CHECK" ]; then
        echo "[ERROR] Container '$CONTAINER_NAME' ($CONTAINER_ID) is not running"
        exit 1
    fi

    MEMORY_MONITORING=true
    echo "Memory monitoring: enabled (container: $CONTAINER_NAME)"
    if [ "$IDLE_DURATION" -gt 0 ]; then
        echo "Idle monitoring: ${IDLE_DURATION}s"
    fi
else
    echo "Memory monitoring: disabled"
fi
echo ""

# Capture initial memory if monitoring enabled
if [ "$MEMORY_MONITORING" = true ]; then
    INITIAL_MEM=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" | awk '{print $1}')
    echo "Initial memory: $INITIAL_MEM"
    echo ""
fi

# Get auth token
echo "Getting auth token..."
TOKEN=$(curl -sf -X POST "$BASE_URL/auth/issue" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" | jq -r '.token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "[ERROR] Failed to get auth token"
    exit 1
fi
echo "[OK] Got auth token"
echo ""

# Generate notebook once
echo "Generating ${NOTEBOOK_SIZE_KB}KB notebook..."
TARGET_BYTES=$((NOTEBOOK_SIZE_KB * 1024))
BASE_SIZE=600  # Approximate size of base notebook structure
PADDING_BYTES=$((TARGET_BYTES - BASE_SIZE))

if [ $PADDING_BYTES -gt 0 ]; then
    # Generate padding efficiently using dd + tr
    PADDING=$(dd if=/dev/zero bs=1 count="$PADDING_BYTES" 2>/dev/null | tr '\000' 'X')
else
    PADDING=""
fi

# Create notebook JSON (stored in memory)
if [ -n "$PADDING" ]; then
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
      "source": ["print('Batch upload test')"],
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
else
    NOTEBOOK_JSON=$(cat <<'EOF'
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
      "source": ["print('Batch upload test')"],
      "metadata": {},
      "outputs": [],
      "execution_count": null
    }
  ]
}
EOF
)
fi

echo "[OK] Generated notebook ($(echo "$NOTEBOOK_JSON" | wc -c | xargs) bytes)"
echo ""

# Function to upload a notebook
upload_notebook() {
    local upload_id=$1
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

    echo "$upload_id|$status|$body"
}

# Start overall timing
overall_start_time=$(date +%s.%N 2>/dev/null || date +%s)

# Global counters
total_success=0
total_failed=0
total_duplicate_key_errors=0

# Memory tracking arrays
declare -a memory_samples_mb
declare -a memory_samples_labels

# Clean up results file
rm -f /tmp/batch_upload_results.txt

echo "Starting upload batches..."
echo ""

# Process batches
for batch_num in $(seq 1 "$NUM_BATCHES"); do
    batch_start_time=$(date +%s.%N 2>/dev/null || date +%s)

    if [ "$BATCH_SIZE" -eq 1 ]; then
        echo -n "Batch $batch_num/$NUM_BATCHES... "
    else
        echo "Batch $batch_num/$NUM_BATCHES: Starting $BATCH_SIZE concurrent uploads..."
    fi

    # Array to store background PIDs
    declare -a pids

    # Start concurrent uploads in this batch
    for i in $(seq 1 "$BATCH_SIZE"); do
        upload_id=$(( (batch_num - 1) * BATCH_SIZE + i ))
        {
            result=$(upload_notebook "$upload_id")
            echo "$result" >> /tmp/batch_upload_results.txt
        } &
        pids+=($!)
    done

    # Wait for all uploads in this batch to complete
    for pid in "${pids[@]}"; do
        wait "$pid" || {
            # Check if container restarted (OOM killed)
            if [ "$MEMORY_MONITORING" = true ]; then
                # First, check if original container still exists and get its exit code
                exit_code=$(docker inspect "$CONTAINER_ID" --format='{{.State.ExitCode}}' 2>/dev/null || echo "unknown")

                # Get current container ID for this name
                current_id=$(docker ps -q -f name="$CONTAINER_NAME" 2>/dev/null || echo "")

                if [ -z "$current_id" ]; then
                    echo ""
                    echo "[ERROR] Container '$CONTAINER_NAME' no longer exists"
                    # Try to get logs from stopped container
                    docker logs "$CONTAINER_ID" 2>&1 | tail -n 50 || echo "Unable to retrieve logs"
                    if [ "$exit_code" = "137" ]; then
                        echo "   Exit code 137 indicates OOM kill"
                    fi
                    exit 1
                elif [ "$current_id" != "$CONTAINER_ID" ]; then
                    echo ""
                    echo "[ERROR] Container restarted (exit code: $exit_code)"
                    if [ "$exit_code" = "137" ]; then
                        echo "   Exit code 137 indicates OOM kill"
                    fi
                    # Get logs from old container before it's removed
                    docker logs "$CONTAINER_ID" 2>&1 | tail -n 50 || echo "Unable to retrieve logs"
                    exit 1
                fi
            fi
        }
    done

    batch_end_time=$(date +%s.%N 2>/dev/null || date +%s)
    batch_time=$(echo "$batch_end_time - $batch_start_time" | bc 2>/dev/null || echo "0")

    # Sample memory periodically
    if [ "$MEMORY_MONITORING" = true ] && [ $((batch_num % MEMORY_SAMPLE_INTERVAL)) -eq 0 ]; then
        mem_usage=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" 2>/dev/null | awk '{print $1}')
        mem_mb=$(parse_memory_to_mib "$mem_usage")
        memory_samples_mb+=("$mem_mb")
        memory_samples_labels+=("batch_$batch_num")

        if [ "$BATCH_SIZE" -eq 1 ]; then
            echo "completed in ${batch_time}s | Memory: $mem_usage"
        else
            echo "  [OK] Batch $batch_num completed in ${batch_time}s | Memory: $mem_usage"
        fi
    else
        if [ "$BATCH_SIZE" -eq 1 ]; then
            echo "completed in ${batch_time}s"
        else
            echo "  [OK] Batch $batch_num completed in ${batch_time}s"
        fi
    fi
done

overall_end_time=$(date +%s.%N 2>/dev/null || date +%s)
total_time=$(echo "$overall_end_time - $overall_start_time" | bc 2>/dev/null || echo "1")

# Validate total_time to prevent division by zero
if [ -z "$total_time" ] || [ "$total_time" = "0" ]; then
    total_time=1
fi

echo ""
echo "=== Results ==="
echo ""

# Parse results
while IFS='|' read -r upload_id status body; do
    if [ "$status" = "201" ]; then
        ((total_success++))
    else
        ((total_failed++))

        # Check if it's a duplicate key error
        if echo "$body" | grep -qi "duplicate.*readable.*id"; then
            ((total_duplicate_key_errors++))
        fi
    fi
done < /tmp/batch_upload_results.txt

# Calculate throughput
if command -v bc >/dev/null 2>&1 && [ -n "$total_time" ]; then
    throughput=$(echo "scale=2; $total_success / $total_time" | bc 2>/dev/null || echo "N/A")
    avg_time_per_upload=$(echo "scale=3; $total_time / $TOTAL_UPLOADS" | bc 2>/dev/null || echo "N/A")
else
    throughput="N/A"
    avg_time_per_upload="N/A"
fi

echo "=== Summary ==="
echo "Total uploads: $TOTAL_UPLOADS"
echo "Successful: $total_success"
echo "Failed: $total_failed"
if [ $total_duplicate_key_errors -gt 0 ]; then
    echo "Duplicate key errors: $total_duplicate_key_errors"
fi
echo ""
echo "=== Performance ==="
echo "Total time: ${total_time}s"
echo "Throughput: ${throughput} uploads/second"
echo "Average time per upload: ${avg_time_per_upload}s"
echo ""

# Memory metrics
if [ "$MEMORY_MONITORING" = true ]; then
    FINAL_MEM=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" 2>/dev/null | awk '{print $1}')
    echo "=== Memory Metrics ==="
    echo "Initial memory: $INITIAL_MEM"
    echo "Final memory: $FINAL_MEM"

    # Calculate memory growth
    if [ ${#memory_samples_mb[@]} -gt 0 ]; then
        first_sample=${memory_samples_mb[0]}
        last_index=$((${#memory_samples_mb[@]} - 1))
        last_sample=${memory_samples_mb[$last_index]}

        if command -v bc >/dev/null 2>&1; then
            growth=$(echo "scale=2; $last_sample - $first_sample" | bc 2>/dev/null)
            echo "Memory growth: ${growth}MiB (first sample to last sample)"
        fi

        echo "Samples collected: ${#memory_samples_mb[@]}"
        echo "First sample: ${first_sample}MiB"
        echo "Last sample: ${last_sample}MiB"
    fi
    echo ""

    # Idle monitoring
    if [ "$IDLE_DURATION" -gt 0 ]; then
        echo "=== Idle Monitoring (${IDLE_DURATION}s) ==="
        idle_start_mem=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" 2>/dev/null | awk '{print $1}')
        idle_start_mb=$(parse_memory_to_mib "$idle_start_mem")

        declare -a idle_samples_mb
        idle_samples_mb+=("$idle_start_mb")

        # Sample every 30 seconds during idle period
        sample_count=$((IDLE_DURATION / 30))
        if [ $sample_count -lt 1 ]; then
            sample_count=1
        fi

        for i in $(seq 1 "$sample_count"); do
            sleep 30
            idle_mem=$(docker stats --no-stream --format "{{.MemUsage}}" "$CONTAINER_ID" 2>/dev/null | awk '{print $1}')
            idle_mem_mb=$(parse_memory_to_mib "$idle_mem")
            idle_samples_mb+=("$idle_mem_mb")

            elapsed=$((i * 30))
            echo "  ${elapsed}s: ${idle_mem}"
        done

        idle_end_index=$((${#idle_samples_mb[@]} - 1))
        idle_end_mb=${idle_samples_mb[$idle_end_index]}

        if command -v bc >/dev/null 2>&1; then
            idle_change=$(echo "scale=2; $idle_end_mb - $idle_start_mb" | bc 2>/dev/null)
            echo ""
            echo "Memory at start: ${idle_start_mb}MiB"
            echo "Memory at end: ${idle_end_mb}MiB"
            echo "Change: ${idle_change}MiB"
        fi
        echo ""
    fi
fi

# Determine exit status
if [ $total_duplicate_key_errors -gt 0 ]; then
    echo "[FAIL] TEST FAILED: Duplicate key errors detected"
    exit 1
elif [ $total_success -eq $TOTAL_UPLOADS ]; then
    echo "[PASS] TEST PASSED: All $TOTAL_UPLOADS uploads succeeded"
    exit 0
else
    echo "[FAIL] TEST FAILED: $total_failed uploads failed"
    exit 1
fi
