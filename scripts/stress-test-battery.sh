#!/bin/bash

# Stress test battery - runs multiple stress test scenarios
# Runs various stress-test.sh configurations as a comprehensive test suite
#
# Environment variables:
#   API_URL: Base API URL (default: http://localhost:8080/api/v1)
#   CONTAINER_ID: Docker container ID for memory monitoring (optional)

set -e

# Use the same environment variables as stress-test
BASE_URL="${API_URL:-http://localhost:8080/api/v1}"
CONTAINER_ID="${CONTAINER_ID:-}"
EXTRA_AUTH_HEADER_NAME="${EXTRA_AUTH_HEADER_NAME:-X-Extra-Auth}"
EXTRA_AUTH_HEADER_SECRET="${EXTRA_AUTH_HEADER_SECRET:-secret}"

# Track test results
declare -a test_names
declare -a test_results
declare -a test_times
overall_start=$(date +%s)

echo "========================================"
echo "    STRESS TEST BATTERY"
echo "========================================"
echo ""
echo "API URL: $BASE_URL"
if [ -n "$CONTAINER_ID" ]; then
    echo "Memory monitoring: enabled"
else
    echo "Memory monitoring: disabled"
fi
echo ""

# Function to run a test and track results
run_test() {
    local test_name="$1"
    shift
    local test_start
    test_start=$(date +%s)

    echo "----------------------------------------"
    echo "TEST: $test_name"
    echo "----------------------------------------"

    if "$@"; then
        test_results+=("PASS")
        echo "[PASS] $test_name"
    else
        test_results+=("FAIL")
        echo "[FAIL] $test_name"
    fi

    local test_end
    test_end=$(date +%s)
    local test_duration=$((test_end - test_start))
    test_names+=("$test_name")
    test_times+=("${test_duration}s")
    echo ""
}

# Test 1: Sequential performance (100 uploads, 1KB each)
run_test "Sequential Performance" \
    env BATCH_SIZE=1 NUM_BATCHES=100 NOTEBOOK_SIZE_KB=1 CONTAINER_ID="$CONTAINER_ID" \
    ./scripts/stress-test.sh

# Test 2: Concurrent uploads (10 concurrent, 10KB each)
run_test "Concurrent Uploads" \
    env BATCH_SIZE=10 NUM_BATCHES=1 NOTEBOOK_SIZE_KB=10 CONTAINER_ID="$CONTAINER_ID" \
    ./scripts/stress-test.sh

# Test 3: Race condition test (20 concurrent, 5MB each)
run_test "Race Condition Test" \
    env BATCH_SIZE=20 NUM_BATCHES=1 NOTEBOOK_SIZE_KB=5000 CONTAINER_ID="$CONTAINER_ID" \
    ./scripts/stress-test.sh

# Test 4: Memory stress (10 concurrent, 10MB each)
run_test "Memory Stress Test" \
    env BATCH_SIZE=10 NUM_BATCHES=1 NOTEBOOK_SIZE_KB=10000 CONTAINER_ID="$CONTAINER_ID" \
    ./scripts/stress-test.sh

# Test 5: Mixed load (5 concurrent × 20 batches = 100 total)
run_test "Mixed Load Test" \
    env BATCH_SIZE=5 NUM_BATCHES=20 NOTEBOOK_SIZE_KB=100 CONTAINER_ID="$CONTAINER_ID" \
    ./scripts/stress-test.sh

# Summary
overall_end=$(date +%s)
overall_duration=$((overall_end - overall_start))

echo "========================================"
echo "    TEST BATTERY SUMMARY"
echo "========================================"
echo ""

# Count passes and failures
pass_count=0
fail_count=0
for result in "${test_results[@]}"; do
    if [ "$result" = "PASS" ]; then
        ((pass_count++))
    else
        ((fail_count++))
    fi
done

# Print results table
for i in "${!test_names[@]}"; do
    result="${test_results[$i]}"
    time="${test_times[$i]}"
    name="${test_names[$i]}"

    if [ "$result" = "PASS" ]; then
        printf "[PASS] %4s | %s\n" "${time}" "${name}"
    else
        printf "[FAIL] %4s | %s\n" "${time}" "${name}"
    fi
done

echo ""
echo "Total tests: ${#test_names[@]}"
echo "Passed: $pass_count"
echo "Failed: $fail_count"
echo "Total time: ${overall_duration}s"
echo ""

# Exit with failure if any test failed
if [ $fail_count -gt 0 ]; then
    echo "[FAIL] Battery failed: $fail_count test(s) failed"
    exit 1
else
    echo "[PASS] Battery passed: All tests succeeded"
    exit 0
fi
