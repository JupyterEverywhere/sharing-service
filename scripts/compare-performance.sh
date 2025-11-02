#!/usr/bin/env bash

# Performance comparison script: Current version vs 0.6.0
# Runs performance tests on both versions and generates comparison report

set -eou pipefail

# Color definitions
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly WHITE='\033[1;37m'
readonly NC='\033[0m' # No Color

# Configuration
readonly ITERATIONS=${1:-100}
readonly NOTEBOOK_SIZE_KB=${2:-5120}  # Default 5MB
readonly BASELINE_VERSION="0.6.0"
readonly RESULTS_DIR="perf-results"
TIMESTAMP=""
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
readonly TIMESTAMP

# Logging functions
log_info() {
  echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $*"
}

log_warning() {
  echo -e "${YELLOW}[WARNING]${NC} $*"
}

print_header() {
  echo -e "\n${CYAN}========================================${NC}"
  echo -e "${WHITE}$1${NC}"
  echo -e "${CYAN}========================================${NC}\n"
}

print_separator() {
  echo -e "${CYAN}----------------------------------------${NC}"
}

# Cleanup function
cleanup() {
  log_info "Cleaning up..."
  docker compose down -v >/dev/null 2>&1 || true

  # Return to original branch if we switched
  if [[ -n "${ORIGINAL_BRANCH:-}" ]]; then
    log_info "Returning to branch: $ORIGINAL_BRANCH"
    git checkout "$ORIGINAL_BRANCH" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

# Save current git state
save_git_state() {
  ORIGINAL_BRANCH=$(git rev-parse --abbrev-ref HEAD)
  log_info "Current branch: $ORIGINAL_BRANCH"

  if ! git diff-index --quiet HEAD --; then
    log_error "Working directory has uncommitted changes. Please commit or stash them first."
    exit 1
  fi
}

# Run performance test for a specific version
run_performance_test() {
  local version=$1
  local output_file=$2

  print_header "Testing Version: $version"

  log_info "Checking out $version..."
  git checkout "$version" >/dev/null 2>&1

  log_info "Building Docker image for $version..."
  docker compose build >/dev/null 2>&1

  log_info "Running performance test ($ITERATIONS iterations, ${NOTEBOOK_SIZE_KB}KB notebooks)..."

  # Run the performance test and capture JSON output
  local test_output
  test_output=$(./scripts/performance-test.sh "$ITERATIONS" "$NOTEBOOK_SIZE_KB" 2>&1)
  local json_result
  json_result=$(echo "$test_output" | tail -n 1)

  # Save full output
  echo "$test_output" > "$output_file"

  # Parse JSON results
  local avg
  avg=$(echo "$json_result" | jq -r '.avg')
  local min
  min=$(echo "$json_result" | jq -r '.min')
  local max
  max=$(echo "$json_result" | jq -r '.max')
  local iterations
  iterations=$(echo "$json_result" | jq -r '.iterations')

  log_success "Test completed: $iterations successful uploads"
  echo -e "  ${WHITE}Average:${NC} ${avg}s"
  echo -e "  ${WHITE}Min:${NC} ${min}s"
  echo -e "  ${WHITE}Max:${NC} ${max}s"

  # Return results as JSON
  echo "$json_result"
}

# Calculate improvement metrics
calculate_improvement() {
  local baseline_avg=$1
  local current_avg=$2

  local improvement
  improvement=$(echo "scale=2; (($baseline_avg - $current_avg) / $baseline_avg) * 100" | bc)
  local speedup
  speedup=$(echo "scale=2; $baseline_avg / $current_avg" | bc)

  echo "$improvement|$speedup"
}

# Generate comparison report
generate_report() {
  local baseline_results=$1
  local current_results=$2

  local baseline_avg
  baseline_avg=$(echo "$baseline_results" | jq -r '.avg')
  local baseline_min
  baseline_min=$(echo "$baseline_results" | jq -r '.min')
  local baseline_max
  baseline_max=$(echo "$baseline_results" | jq -r '.max')

  local current_avg
  current_avg=$(echo "$current_results" | jq -r '.avg')
  local current_min
  current_min=$(echo "$current_results" | jq -r '.min')
  local current_max
  current_max=$(echo "$current_results" | jq -r '.max')

  local notebook_size
  notebook_size=$(echo "$current_results" | jq -r '.notebook_size')

  local metrics
  metrics=$(calculate_improvement "$baseline_avg" "$current_avg")
  local improvement
  improvement=$(echo "$metrics" | cut -d'|' -f1)
  local speedup
  speedup=$(echo "$metrics" | cut -d'|' -f2)

  print_header "Performance Comparison Report"

  echo -e "${WHITE}Test Configuration:${NC}"
  echo -e "  Iterations: $ITERATIONS"
  echo -e "  Notebook Size: $notebook_size"
  echo ""

  print_separator
  echo -e "${WHITE}Version $BASELINE_VERSION (Baseline):${NC}"
  echo -e "  Average: ${baseline_avg}s"
  echo -e "  Min: ${baseline_min}s"
  echo -e "  Max: ${baseline_max}s"

  print_separator
  echo -e "${WHITE}Current Version (HEAD):${NC}"
  echo -e "  Average: ${GREEN}${current_avg}s${NC}"
  echo -e "  Min: ${GREEN}${current_min}s${NC}"
  echo -e "  Max: ${GREEN}${current_max}s${NC}"

  print_separator
  echo -e "${WHITE}Performance Improvement:${NC}"

  if (( $(echo "$improvement > 0" | bc -l) )); then
    echo -e "  ${GREEN}✓ ${improvement}% faster${NC}"
    echo -e "  ${GREEN}✓ ${speedup}x speedup${NC}"
  elif (( $(echo "$improvement < 0" | bc -l) )); then
    local regression
    regression=$(echo "scale=2; -1 * $improvement" | bc)
    echo -e "  ${RED}✗ ${regression}% slower${NC}"
    echo -e "  ${RED}✗ Performance regression${NC}"
  else
    echo -e "  ${YELLOW}= No significant change${NC}"
  fi

  echo ""

  # Save comparison as JSON
  local comparison_file="$RESULTS_DIR/comparison-$TIMESTAMP.json"
  cat > "$comparison_file" <<EOF
{
  "timestamp": "$TIMESTAMP",
  "iterations": $ITERATIONS,
  "notebook_size": "$notebook_size",
  "baseline_version": "$BASELINE_VERSION",
  "baseline": {
    "avg": $baseline_avg,
    "min": $baseline_min,
    "max": $baseline_max
  },
  "current": {
    "avg": $current_avg,
    "min": $current_min,
    "max": $current_max
  },
  "improvement": {
    "percentage": $improvement,
    "speedup": $speedup
  }
}
EOF

  log_success "Results saved to: $comparison_file"
}

# Main execution
main() {
  print_header "Performance Comparison: Current vs $BASELINE_VERSION"

  # Create results directory
  mkdir -p "$RESULTS_DIR"

  # Save git state
  save_git_state

  # Test baseline version
  local baseline_output="$RESULTS_DIR/baseline-$BASELINE_VERSION-$TIMESTAMP.log"
  local baseline_results
  baseline_results=$(run_performance_test "$BASELINE_VERSION" "$baseline_output")

  # Test current version
  local current_output="$RESULTS_DIR/current-$TIMESTAMP.log"
  local current_results
  current_results=$(run_performance_test "$ORIGINAL_BRANCH" "$current_output")

  # Generate comparison report
  generate_report "$baseline_results" "$current_results"

  log_success "Comparison complete!"
}

main "$@"
