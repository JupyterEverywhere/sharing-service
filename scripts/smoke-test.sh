#!/usr/bin/env bash

# This script is used to smoke test a running sharing service.

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

# Helper function to print to stderr
eecho() {
  echo "$@" >&2
}

# Logging functions
log_info() {
  eecho -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
  eecho -e "${GREEN}[SUCCESS]${NC} $*"
}

log_warning() {
  eecho -e "${YELLOW}[WARNING]${NC} $*"
}

log_error() {
  eecho -e "${RED}[ERROR]${NC} $*"
}

log_step() {
  eecho -e "${PURPLE}[STEP]${NC} $*"
}

# Function to print colored headers
print_header() {
  eecho -e "\n${CYAN}========================================${NC}"
  eecho -e "${WHITE}$1${NC}"
  eecho -e "${CYAN}========================================${NC}"
}

# Default configuration
: "${API_HOST:=http://localhost:8080}"
: "${API_PATH:=api}"
: "${API_VERSION:=v1}"
: "${API_URL:=${API_HOST}/${API_PATH}/${API_VERSION}}"

# Global variables
PYTHON_NOTEBOOK_ID=""
R_NOTEBOOK_ID=""
API_TOKEN=""

# Check required dependencies
check_dependencies() {
  local missing_deps=()

  if ! command -v curl &>/dev/null; then
    missing_deps+=("curl")
  fi

  if ! command -v jq &>/dev/null; then
    missing_deps+=("jq")
  fi

  if [[ ${#missing_deps[@]} -gt 0 ]]; then
    log_error "Missing required dependencies: ${missing_deps[*]}"
    log_error "Please install them and try again."
    exit 1
  fi
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
  --api-url=*) API_URL="${1#*=}" ;;
  --api-url)
    shift
    API_URL="$1"
    ;;
  --help)
    eecho -e "${WHITE}Usage:${NC} $0 [--api-url=API_URL]"
    eecho -e "${WHITE}Options:${NC}"
    eecho -e "  --api-url=URL    Set the API URL (default: ${API_URL})"
    eecho -e "  --help           Show this help message"
    exit 0
    ;;
  *)
    log_error "Unknown option: $1"
    eecho -e "${WHITE}Use --help for usage information.${NC}"
    exit 1
    ;;
  esac
  shift
done

readonly API_URL

# Print the parameters being used
show_parameters() {
  print_header "SMOKE TEST CONFIGURATION"
  log_info "API URL: ${CYAN}${API_URL}${NC}"
  log_info "Timestamp: $(date)"
}

# Function to perform a health check on the service
health_check() {
  log_step "Performing health check..."

  local response
  if response=$(curl -sf "${API_URL}/health" 2>/dev/null); then
    log_success "Health check passed"
  else
    log_error "Health check failed"
    log_error "Please ensure the service is running at ${API_URL}"
    exit 1
  fi
}

# Issue authentication token
issue_token() {
  log_step "Issuing authentication token..."

  local response
  if response=$(curl -sf -X POST "${API_URL}/auth/issue" 2>/dev/null); then
    API_TOKEN=$(echo "${response}" | jq -r '.token' 2>/dev/null || echo "")
    if [[ -n "${API_TOKEN}" && "${API_TOKEN}" != "null" ]]; then
      log_success "Token issued successfully"
      log_info "Token: ${YELLOW}${API_TOKEN}${NC}"
    else
      log_error "Failed to parse token from response"
      log_error "Response: ${response}"
      exit 1
    fi
  else
    log_error "Failed to issue token"
    log_error "Please check that the auth endpoint is available"
    exit 1
  fi
}

# Share a notebook (generic function)
share_notebook() {
  local notebook_type="$1"
  local notebook_file="$2"

  if [[ -z "${API_TOKEN}" ]]; then
    log_error "API token is not set. Please run issue_token first."
    exit 1
  fi

  log_step "Sharing ${notebook_type} notebook..."

  if [[ ! -f "${notebook_file}" ]]; then
    log_error "${notebook_type} notebook file not found: ${notebook_file}"
    exit 1
  fi

  local notebook
  notebook=$(cat "${notebook_file}")

  local data
  data=$(jq -n --argjson notebook "${notebook}" '{password: "", notebook: $notebook}')

  local response
  if response=$(curl -sf -X POST "${API_URL}/notebooks" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -d "${data}" 2>/dev/null); then

    local notebook_id
    local domain_id
    local readable_id
    notebook_id=$(echo "${response}" | jq -r '.notebook.id' 2>/dev/null || echo "")
    domain_id=$(echo "${response}" | jq -r '.notebook.domain_id' 2>/dev/null || echo "")
    readable_id=$(echo "${response}" | jq -r '.notebook.readable_id' 2>/dev/null || echo "")

    if [[ -n "${notebook_id}" && "${notebook_id}" != "null" ]]; then
      log_success "${notebook_type} notebook shared successfully"
      log_info "${notebook_type} Notebook ID: ${YELLOW}${notebook_id}${NC}"
      log_info "${notebook_type} Domain ID: ${YELLOW}${domain_id}${NC}"
      log_info "${notebook_type} Readable ID: ${YELLOW}${readable_id}${NC}"

      # Return the notebook ID
      echo "${notebook_id}"
    else
      log_error "Failed to parse ${notebook_type} notebook ID from response"
      log_error "Response: ${response}"
      exit 1
    fi
  else
    log_error "Failed to share ${notebook_type} notebook"
    log_error "Please check the API endpoint and authentication"
    exit 1
  fi
}

# Share a Python notebook
share_python_notebook() {
  PYTHON_NOTEBOOK_ID=$(share_notebook "Python" "scripts/example-py.ipynb")
}

# Share an R notebook
share_r_notebook() {
  R_NOTEBOOK_ID=$(share_notebook "R" "scripts/example-r.ipynb")
}

# Retrieve a notebook (generic function)
retrieve_notebook() {
  local notebook_type="$1"
  local notebook_id="$2"

  if [[ -z "${notebook_id}" ]]; then
    log_error "${notebook_type} notebook ID not set. Please run share_${notebook_type,,}_notebook first."
    exit 1
  fi

  log_step "Retrieving ${notebook_type} notebook..."
  local response
  if response=$(curl -sf -X GET "${API_URL}/notebooks/${notebook_id}" \
    -H "Authorization: Bearer ${API_TOKEN}" 2>/dev/null); then

    log_success "${notebook_type} notebook retrieved successfully"

    # Show the full response content
    # eecho -e "\n${CYAN}${notebook_type} notebook response content:${NC}"
    # echo "${response}" | jq '.'
  else
    log_error "Failed to retrieve ${notebook_type} notebook"
    log_error "Please check the notebook ID and authentication"
    exit 1
  fi
}

# Retrieve the shared Python notebook
retrieve_python_notebook() {
  retrieve_notebook "Python" "${PYTHON_NOTEBOOK_ID}"
}

# Retrieve the shared R notebook
retrieve_r_notebook() {
  retrieve_notebook "R" "${R_NOTEBOOK_ID}"
}

# Main execution function
main() {
  local start_time
  start_time=$(date +%s)

  # Check dependencies first
  check_dependencies

  # Test functions to run
  local tests=(
    "show_parameters"
    "health_check"
    "issue_token"
    "share_python_notebook"
    "retrieve_python_notebook"
    "share_r_notebook"
    "retrieve_r_notebook"
  )

  local failed_tests=()

  for test in "${tests[@]}"; do
    if ! "$test"; then
      failed_tests+=("$test")
    fi
    eecho "" # Add spacing between tests
  done

  # Summary
  local end_time
  end_time=$(date +%s)
  local duration=$((end_time - start_time))

  print_header "SMOKE TEST SUMMARY"

  if [[ ${#failed_tests[@]} -eq 0 ]]; then
    log_success "All tests passed! ✅"
    log_info "Duration: ${duration}s"
    log_info "API URL: ${API_URL}"
    if [[ -n "${PYTHON_NOTEBOOK_ID}" ]]; then
      log_info "Created Python notebook: ${PYTHON_NOTEBOOK_ID}"
    fi
    if [[ -n "${R_NOTEBOOK_ID}" ]]; then
      log_info "Created R notebook: ${R_NOTEBOOK_ID}"
    fi
  else
    log_error "Some tests failed! ❌"
    log_error "Failed tests: ${failed_tests[*]}"
    exit 1
  fi
}

# Run the main function
main "$@"
