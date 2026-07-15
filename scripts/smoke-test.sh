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
REAL_R_NOTEBOOK_ID=""
NO_KERNEL_NOTEBOOK_ID=""
API_TOKEN=""
ADMIN_DELETE_NOTEBOOK_ID=""
ADMIN_DELETE_NOTEBOOK_ID_2=""
ADMIN_TOKEN=""
EXTRA_AUTH_HEADER_NAME="${EXTRA_AUTH_HEADER_NAME:-X-Extra-Auth}"
EXTRA_AUTH_HEADER_SECRET="${EXTRA_AUTH_HEADER_SECRET:-secret}"
ADMIN_SECRET="${ADMIN_SECRET:-admin-secret-for-dev}"

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
  if response=$(curl -sf "${API_URL}/health" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null); then
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
  if response=$(curl -sf -X POST "${API_URL}/auth/issue" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null); then
    API_TOKEN=$(echo "${response}" | jq -r '.token' 2>/dev/null || echo "")
    if [[ -n "${API_TOKEN}" && "${API_TOKEN}" != "null" ]]; then
      log_success "Token issued successfully"
    else
      log_error "Failed to parse token from response"
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
  local http_status

  # Use a here-string to capture both response and status in one request
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "${API_URL}/notebooks" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" \
    -d "${data}" 2>/dev/null)

  # Extract HTTP status and response body
  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
  response="${response//HTTPSTATUS:[0-9]*}"

  if [[ "${http_status}" -ge 200 && "${http_status}" -lt 300 ]]; then
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
    log_error "HTTP Status: ${http_status}"
    if [[ -n "${response}" ]]; then
      log_error "Server response: ${response}"
    fi
    log_error "Please check the API endpoint and authentication"
    exit 1
  fi
}

# Share a Python notebook
share_python_notebook() {
  PYTHON_NOTEBOOK_ID=$(share_notebook "Python" "scripts/example-notebooks/py.ipynb")
}

# Share an R notebook
share_r_notebook() {
  R_NOTEBOOK_ID=$(share_notebook "R" "scripts/example-notebooks/r.ipynb")
}

# Share a real R notebook (larger, real-world example with nbformat 4.1)
share_real_r_notebook() {
  REAL_R_NOTEBOOK_ID=$(share_notebook "Real-R" "scripts/example-notebooks/real-r.ipynb")
}

# Share a notebook with empty language_info.name (tests fix for issue #0.8.1)
share_no_kernel_notebook() {
  NO_KERNEL_NOTEBOOK_ID=$(share_notebook "No-Kernel" "scripts/example-notebooks/no-kernel.ipynb")
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
  local http_status

  # Use a custom marker to capture both response and status in one request
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "${API_URL}/notebooks/${notebook_id}" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  # Extract HTTP status and response body
  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
  response="${response//HTTPSTATUS:[0-9]*/}"

  if [[ "${http_status}" -ge 200 && "${http_status}" -lt 300 ]]; then
    log_success "${notebook_type} notebook retrieved successfully"

    # Uncomment to show the full response content
    # eecho -e "\n${CYAN}${notebook_type} notebook response content:${NC}"
    # echo "${response}" | jq '.'
  else
    log_error "Failed to retrieve ${notebook_type} notebook"
    log_error "HTTP Status: ${http_status}"
    if [[ -n "${response}" ]]; then
      log_error "Server response: ${response}"
    fi
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

# Retrieve the shared real R notebook
retrieve_real_r_notebook() {
  retrieve_notebook "Real-R" "${REAL_R_NOTEBOOK_ID}"
}

# Retrieve the shared no-kernel notebook
retrieve_no_kernel_notebook() {
  retrieve_notebook "No-Kernel" "${NO_KERNEL_NOTEBOOK_ID}"
}

# Issue admin token
issue_admin_token() {
  log_step "Issuing admin token..."

  local response
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "${API_URL}/auth/admin/token" \
    -H "Content-Type: application/json" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" \
    -d "{\"secret\":\"${ADMIN_SECRET}\",\"tokenName\":\"smoke-test\"}" 2>/dev/null)

  local http_status
  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
  response="${response//HTTPSTATUS:[0-9]*}"

  if [[ "${http_status}" == "200" ]]; then
    ADMIN_TOKEN=$(echo "${response}" | jq -r '.token' 2>/dev/null || echo "")
    if [[ -n "${ADMIN_TOKEN}" && "${ADMIN_TOKEN}" != "null" ]]; then
      log_success "Admin token issued successfully"
    else
      log_error "Failed to parse admin token from response"
      exit 1
    fi
  else
    log_error "Failed to issue admin token (HTTP ${http_status})"
    log_error "Response: ${response}"
    exit 1
  fi
}

# Admin delete notebook by UUID
admin_delete_by_uuid() {
  log_step "Testing admin delete by UUID..."

  # Create a notebook to delete
  ADMIN_DELETE_NOTEBOOK_ID=$(share_notebook "AdminDelete" "scripts/example-notebooks/py.ipynb")

  # Delete it
  local response
  local http_status
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X DELETE \
    "${API_URL}/notebooks/${ADMIN_DELETE_NOTEBOOK_ID}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)

  if [[ "${http_status}" == "204" ]]; then
    log_success "Notebook deleted by UUID (HTTP 204)"
  else
    log_error "Expected HTTP 204 but got ${http_status}"
    exit 1
  fi

  # Verify it's gone (expect 404)
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET \
    "${API_URL}/notebooks/${ADMIN_DELETE_NOTEBOOK_ID}" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)

  if [[ "${http_status}" == "404" ]]; then
    log_success "Deleted notebook returns 404 as expected"
  else
    log_error "Expected HTTP 404 for deleted notebook but got ${http_status}"
    exit 1
  fi
}

# Admin delete notebook by readable ID
admin_delete_by_readable_id() {
  log_step "Testing admin delete by readable ID..."

  # Create a notebook to delete and capture its readable ID
  local notebook_id
  notebook_id=$(share_notebook "AdminDeleteReadable" "scripts/example-notebooks/r.ipynb")

  # Get readable ID by fetching the notebook
  local response
  local http_status
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET \
    "${API_URL}/notebooks/${notebook_id}" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
  response="${response//HTTPSTATUS:[0-9]*}"

  local readable_id
  readable_id=$(echo "${response}" | jq -r '.readable_id' 2>/dev/null || echo "")

  if [[ -z "${readable_id}" || "${readable_id}" == "null" ]]; then
    log_error "Could not get readable ID for notebook"
    exit 1
  fi

  log_info "Deleting by readable ID: ${YELLOW}${readable_id}${NC}"

  # Delete by readable ID
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X DELETE \
    "${API_URL}/notebooks/readable/${readable_id}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)

  if [[ "${http_status}" == "204" ]]; then
    log_success "Notebook deleted by readable ID (HTTP 204)"
  else
    log_error "Expected HTTP 204 but got ${http_status}"
    exit 1
  fi

  # Verify it's gone
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET \
    "${API_URL}/notebooks/${notebook_id}" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)

  if [[ "${http_status}" == "404" ]]; then
    log_success "Deleted notebook returns 404 as expected"
  else
    log_error "Expected HTTP 404 for deleted notebook but got ${http_status}"
    exit 1
  fi
}

# Admin delete all notebooks by session
admin_delete_by_session() {
  log_step "Testing admin delete by session..."

  # Issue a fresh token so this test gets its own session and doesn't delete
  # notebooks created by earlier tests
  local session_token
  local session_response
  session_response=$(curl -sf -X POST "${API_URL}/auth/issue" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)
  session_token=$(echo "${session_response}" | jq -r '.token' 2>/dev/null || echo "")

  if [[ -z "${session_token}" || "${session_token}" == "null" ]]; then
    log_error "Failed to issue session token for session delete test"
    exit 1
  fi

  # Save and swap token for notebook creation
  local saved_token="${API_TOKEN}"
  API_TOKEN="${session_token}"

  local nb1_id
  local nb2_id
  nb1_id=$(share_notebook "SessionDelete1" "scripts/example-notebooks/py.ipynb")
  nb2_id=$(share_notebook "SessionDelete2" "scripts/example-notebooks/r.ipynb")

  # Restore original token
  API_TOKEN="${saved_token}"

  # Extract session ID from the dedicated token (portable base64 decode)
  local session_id
  local jwt_payload
  jwt_payload=$(echo "${session_token}" | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null || echo "${session_token}" | cut -d. -f2 | tr '_-' '/+' | base64 -D 2>/dev/null)
  session_id=$(echo "${jwt_payload}" | jq -r '.session_id' 2>/dev/null || echo "")

  if [[ -z "${session_id}" || "${session_id}" == "null" ]]; then
    log_error "Could not extract session ID from token"
    exit 1
  fi

  log_info "Session ID: ${YELLOW}${session_id}${NC}"
  log_info "Created notebooks: ${YELLOW}${nb1_id}${NC}, ${YELLOW}${nb2_id}${NC}"

  # Delete all notebooks for this session
  local response
  local http_status
  local start_time
  start_time=$(date +%s)

  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X DELETE \
    "${API_URL}/sessions/${session_id}/notebooks" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  local end_time
  end_time=$(date +%s)
  local duration=$((end_time - start_time))

  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
  response="${response//HTTPSTATUS:[0-9]*}"

  if [[ "${http_status}" == "200" ]]; then
    local deleted_count
    deleted_count=$(echo "${response}" | jq -r '.deletedCount' 2>/dev/null || echo "")

    if [[ "${deleted_count}" -eq 2 ]]; then
      log_success "Session notebooks deleted (HTTP 200, deletedCount=${deleted_count})"
    else
      log_error "Expected deletedCount == 2 but got ${deleted_count}"
      exit 1
    fi

    # Verify performance gate (SC-005: under 30 seconds)
    if [[ "${duration}" -lt 30 ]]; then
      log_success "Response completed in ${duration}s (< 30s performance gate)"
    else
      log_error "Response took ${duration}s, exceeds 30s performance gate"
      exit 1
    fi
  else
    log_error "Expected HTTP 200 but got ${http_status}"
    log_error "Response: ${response}"
    exit 1
  fi

  # Verify notebooks return 404
  for nb_id in "${nb1_id}" "${nb2_id}"; do
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET \
      "${API_URL}/notebooks/${nb_id}" \
      -H "Authorization: Bearer ${API_TOKEN}" \
      -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

    http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)

    if [[ "${http_status}" == "404" ]]; then
      log_success "Notebook ${nb_id} returns 404 as expected"
    else
      log_error "Expected HTTP 404 for deleted notebook ${nb_id} but got ${http_status}"
      exit 1
    fi
  done
}

# Verify non-admin cannot delete
admin_delete_unauthorized() {
  log_step "Testing unauthorized delete rejection..."

  # Create a notebook
  ADMIN_DELETE_NOTEBOOK_ID_2=$(share_notebook "AdminDeleteUnauth" "scripts/example-notebooks/py.ipynb")

  # Try to delete with regular (non-admin) token — should be rejected
  local response
  local http_status
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X DELETE \
    "${API_URL}/notebooks/${ADMIN_DELETE_NOTEBOOK_ID_2}" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)

  if [[ "${http_status}" == "403" ]]; then
    log_success "Non-admin delete correctly rejected (HTTP 403)"
  else
    log_error "Expected HTTP 403 for non-admin delete but got ${http_status}"
    exit 1
  fi

  # Verify notebook still exists
  response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET \
    "${API_URL}/notebooks/${ADMIN_DELETE_NOTEBOOK_ID_2}" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" 2>/dev/null)

  http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)

  if [[ "${http_status}" == "200" ]]; then
    log_success "Notebook still exists after unauthorized delete attempt"
  else
    log_error "Expected notebook to still exist (HTTP 200) but got ${http_status}"
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
    "share_python_notebook"
    "retrieve_python_notebook"
    "share_r_notebook"
    "retrieve_r_notebook"
    "share_real_r_notebook"
    "retrieve_real_r_notebook"
    "share_no_kernel_notebook"
    "retrieve_no_kernel_notebook"
    "issue_admin_token"
    "admin_delete_by_uuid"
    "admin_delete_by_readable_id"
    "admin_delete_unauthorized"
    "admin_delete_by_session"
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
    log_success "All tests passed"
    log_info "Duration: ${duration}s"
    log_info "API URL: ${API_URL}"
    if [[ -n "${PYTHON_NOTEBOOK_ID}" ]]; then
      log_info "Created Python notebook: ${PYTHON_NOTEBOOK_ID}"
    fi
    if [[ -n "${R_NOTEBOOK_ID}" ]]; then
      log_info "Created R notebook: ${R_NOTEBOOK_ID}"
    fi
    if [[ -n "${REAL_R_NOTEBOOK_ID}" ]]; then
      log_info "Created Real-R notebook: ${REAL_R_NOTEBOOK_ID}"
    fi
    if [[ -n "${NO_KERNEL_NOTEBOOK_ID}" ]]; then
      log_info "Created No-Kernel notebook: ${NO_KERNEL_NOTEBOOK_ID}"
    fi
  else
    log_error "Some tests failed"
    log_error "Failed tests: ${failed_tests[*]}"
    exit 1
  fi
}

# Run the main function
main "$@"
