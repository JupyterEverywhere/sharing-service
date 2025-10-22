#!/usr/bin/env bash

# Performance test script for notebook upload validation
# Tests upload performance by timing multiple notebook uploads

set -eou pipefail

# Color definitions
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly WHITE='\033[1;37m'
readonly NC='\033[0m' # No Color

# Configuration
readonly ITERATIONS=${1:-10}
readonly NOTEBOOK_SIZE_KB=${2:-5120}  # Default 5MB
readonly NOTEBOOK_PATH="scripts/test-notebook.ipynb"
readonly SERVICE_URL="http://localhost:8080/api/v1"
readonly TIMEOUT=120

# Logging functions
log_info() {
  echo -e "${BLUE}[INFO]${NC} $*" >&2
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $*" >&2
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $*" >&2
}

print_header() {
  echo -e "\n${CYAN}========================================${NC}" >&2
  echo -e "${WHITE}$1${NC}" >&2
  echo -e "${CYAN}========================================${NC}" >&2
}

# Cleanup function
cleanup() {
  log_info "Stopping Docker services..."
  docker compose down -v >/dev/null 2>&1 || true
}

trap cleanup EXIT

# Main test function
main() {
  print_header "Performance Test: $ITERATIONS iterations"

  # Generate test notebook
  log_info "Generating ${NOTEBOOK_SIZE_KB}KB test notebook..."
  ./scripts/generate-test-notebook.sh "$NOTEBOOK_SIZE_KB" "$NOTEBOOK_PATH"

  local notebook_size=$(du -h "$NOTEBOOK_PATH" | awk '{print $1}')
  log_info "Using notebook: $NOTEBOOK_PATH ($notebook_size)"

  # Start services
  log_info "Starting Docker services..."
  docker compose up -d --build >/dev/null 2>&1

  # Wait for service to be ready
  log_info "Waiting for service to be ready..."
  local retries=0
  while ! curl -s "$SERVICE_URL/health" >/dev/null 2>&1; do
    sleep 2
    retries=$((retries + 1))
    if [[ $retries -gt 30 ]]; then
      log_error "Service failed to start after 60 seconds"
      exit 1
    fi
  done
  log_success "Service is ready"

  # Issue auth token
  log_info "Issuing auth token..."
  local token=$(curl -s -X POST "$SERVICE_URL/auth/issue" | jq -r .token)
  if [[ -z "$token" || "$token" == "null" ]]; then
    log_error "Failed to issue auth token"
    exit 1
  fi
  log_success "Auth token obtained"

  # Read notebook content
  local notebook_json=$(cat "$NOTEBOOK_PATH")

  # Array to store upload times
  declare -a upload_times

  # Run upload iterations
  print_header "Running $ITERATIONS upload iterations"

  for ((i=1; i<=$ITERATIONS; i++)); do
    log_info "Upload $i/$ITERATIONS..."

    local start_time=$(date +%s.%N)

    local response=$(curl -s -w "\n%{http_code}" \
      -X POST "$SERVICE_URL/notebooks" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: application/json" \
      -d "{\"notebook\": $notebook_json}" \
      --max-time $TIMEOUT)

    local end_time=$(date +%s.%N)
    local http_code=$(echo "$response" | tail -n 1)

    if [[ "$http_code" != "201" ]]; then
      log_error "Upload $i failed with HTTP $http_code"
      continue
    fi

    local upload_time=$(echo "$end_time - $start_time" | bc)
    upload_times+=($upload_time)

    log_success "Upload $i completed in ${upload_time}s"
  done

  # Calculate statistics
  if [[ ${#upload_times[@]} -eq 0 ]]; then
    log_error "No successful uploads"
    exit 1
  fi

  print_header "Performance Results"

  local total=0
  local min=${upload_times[0]}
  local max=${upload_times[0]}

  for time in "${upload_times[@]}"; do
    total=$(echo "$total + $time" | bc)
    if (( $(echo "$time < $min" | bc -l) )); then
      min=$time
    fi
    if (( $(echo "$time > $max" | bc -l) )); then
      max=$time
    fi
  done

  local avg=$(echo "scale=2; $total / ${#upload_times[@]}" | bc)

  echo -e "${WHITE}Iterations:${NC} ${#upload_times[@]}/$ITERATIONS successful"
  echo -e "${WHITE}Notebook Size:${NC} $notebook_size"
  echo -e "${WHITE}Average Time:${NC} ${GREEN}${avg}s${NC}"
  echo -e "${WHITE}Min Time:${NC} ${min}s"
  echo -e "${WHITE}Max Time:${NC} ${max}s"

  # Output JSON for programmatic parsing
  echo ""
  echo "{\"iterations\": ${#upload_times[@]}, \"avg\": $avg, \"min\": $min, \"max\": $max, \"notebook_size\": \"$notebook_size\"}"
}

main "$@"
