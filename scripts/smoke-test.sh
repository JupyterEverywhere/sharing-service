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

# Logging functions
log_info() {
  echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_warning() {
  echo -e "${YELLOW}[WARNING]${NC} $*"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $*" >&2
}

log_step() {
  echo -e "${PURPLE}[STEP]${NC} $*"
}

# Function to print colored headers
print_header() {
  echo -e "\n${CYAN}========================================${NC}"
  echo -e "${WHITE}$1${NC}"
  echo -e "${CYAN}========================================${NC}"
}

# Default configuration
: "${API_HOST:=http://localhost:8080}"
: "${API_PATH:=api}"
: "${API_VERSION:=v1}"
: "${API_URL:=${API_HOST}/${API_PATH}/${API_VERSION}}"

# Global variables
NOTEBOOK_ID=""
NOTEBOOK_DOMAIN_ID=""
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
    echo -e "${WHITE}Usage:${NC} $0 [--api-url=API_URL]"
    echo -e "${WHITE}Options:${NC}"
    echo -e "  --api-url=URL    Set the API URL (default: ${API_URL})"
    echo -e "  --help           Show this help message"
    exit 0
    ;;
  *)
    log_error "Unknown option: $1"
    echo -e "${WHITE}Use --help for usage information.${NC}"
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

# Share a test notebook
share_notebook() {
  if [[ -z "${API_TOKEN}" ]]; then
    log_error "API token is not set. Please run issue_token first."
    exit 1
  fi

  log_step "Sharing test notebook..."

  local notebook
  notebook=$(
    cat <<'EOF'
{
    "cells": [
        {
            "cell_type": "markdown",
            "execution_count": null,
            "id": "f99e9668",
            "metadata": {},
            "source": [
                "# Notebook Example\n",
                "This is an example of Markdown from the smoke test."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": 1,
            "id": "b1c2d3e4",
            "metadata": {},
            "outputs": [],
            "source": [
                "print('Hello, world!')\n",
                "print('This is a smoke test notebook.')"
            ]
        }
    ],
    "metadata": {
        "kernelspec": {
            "display_name": "Python 3",
            "language": "python",
            "name": "python3"
        },
        "language_info": {
            "codemirror_mode": {
                "name": "ipython",
                "version": 3
            },
            "file_extension": ".py",
            "mimetype": "text/x-python",
            "name": "python",
            "nbconvert_exporter": "python",
            "version": "3.8.5"
        }
    },
    "visibility": "public",
    "nbformat": 4,
    "nbformat_minor": 5
}
EOF
  )

  local data
  data=$(jq -n --argjson notebook "${notebook}" '{password: "", notebook: $notebook}')

  local response
  if response=$(curl -sf -X POST "${API_URL}/notebooks" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -d "${data}" 2>/dev/null); then

    NOTEBOOK_ID=$(echo "${response}" | jq -r '.notebook.id' 2>/dev/null || echo "")
    NOTEBOOK_DOMAIN_ID=$(echo "${response}" | jq -r '.notebook.domain_id' 2>/dev/null || echo "")
    local readable_id
    readable_id=$(echo "${response}" | jq -r '.notebook.readable_id' 2>/dev/null || echo "")

    if [[ -n "${NOTEBOOK_ID}" && "${NOTEBOOK_ID}" != "null" ]]; then
      log_success "Notebook shared successfully"
      log_info "Notebook ID: ${YELLOW}${NOTEBOOK_ID}${NC}"
      log_info "Domain ID: ${YELLOW}${NOTEBOOK_DOMAIN_ID}${NC}"
      log_info "Readable ID: ${YELLOW}${readable_id}${NC}"
    else
      log_error "Failed to parse notebook ID from response"
      log_error "Response: ${response}"
      exit 1
    fi
  else
    log_error "Failed to share notebook"
    log_error "Please check the API endpoint and authentication"
    exit 1
  fi
}

# Retrieve the shared notebook
retrieve_notebook() {
  if [[ -z "${NOTEBOOK_ID}" ]]; then
    log_error "Notebook ID not set. Please run share_notebook first."
    exit 1
  fi

  log_step "Retrieving notebook..."
  local response
  if response=$(curl -sf -X GET "${API_URL}/notebooks/${NOTEBOOK_ID}" \
    -H "Authorization: Bearer ${API_TOKEN}" 2>/dev/null); then

    log_success "Notebook retrieved successfully"

    # Show the full response content
    echo -e "\n${CYAN}Response content:${NC}"
    echo "${response}" | jq '.'
  else
    log_error "Failed to retrieve notebook"
    log_error "Please check the notebook ID and authentication"
    exit 1
  fi
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
    "share_notebook"
    "retrieve_notebook"
  )

  local failed_tests=()

  for test in "${tests[@]}"; do
    if ! "$test"; then
      failed_tests+=("$test")
    fi
    echo "" # Add spacing between tests
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
    if [[ -n "${NOTEBOOK_ID}" ]]; then
      log_info "Created notebook: ${NOTEBOOK_ID}"
    fi
  else
    log_error "Some tests failed! ❌"
    log_error "Failed tests: ${failed_tests[*]}"
    exit 1
  fi
}

# Run the main function
main "$@"
