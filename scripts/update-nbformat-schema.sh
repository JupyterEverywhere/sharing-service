#!/usr/bin/env bash

# Update nbformat JSON Schema
#
# This script downloads the latest Jupyter Notebook v4 JSON schema from the
# official nbformat repository and adds proper license attribution.
#
# Usage: ./scripts/update-nbformat-schema.sh
#
# The schema is used for validating Jupyter Notebook files in the sharing service.
# This is a manual operation - run when you want to update to the latest schema.

set -eou pipefail

# Color definitions
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Configuration
readonly NBFORMAT_REPO="jupyter/nbformat"
readonly SCHEMA_FILE="nbformat/v4/nbformat.v4.schema.json"
readonly OUTPUT_DIR="src/main/resources/schemas"
readonly OUTPUT_FILE="${OUTPUT_DIR}/nbformat.v4.schema.json"
readonly TEMP_FILE="/tmp/nbformat.v4.schema.json.tmp"

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

log_warning() {
  echo -e "${YELLOW}[WARNING]${NC} $*" >&2
}

# Main function
main() {
  log_info "Fetching latest nbformat schema..."

  # Get latest release tag from GitHub API
  local latest_tag
  latest_tag=$(curl -s "https://api.github.com/repos/${NBFORMAT_REPO}/releases/latest" | \
    grep '"tag_name"' | \
    sed -E 's/.*"tag_name": "([^"]+)".*/\1/')

  if [[ -z "$latest_tag" ]]; then
    log_warning "Could not fetch latest release tag, using 'main' branch"
    latest_tag="main"
  else
    log_info "Latest release: ${latest_tag}"
  fi

  # Construct raw GitHub URL
  local schema_url="https://raw.githubusercontent.com/${NBFORMAT_REPO}/${latest_tag}/${SCHEMA_FILE}"
  log_info "Downloading from: ${schema_url}"

  # Download schema
  if ! curl -f -s -o "$TEMP_FILE" "$schema_url"; then
    log_error "Failed to download schema from ${schema_url}"
    exit 1
  fi

  log_success "Downloaded schema successfully"

  # Verify it's valid JSON
  if ! python3 -m json.tool "$TEMP_FILE" >/dev/null 2>&1; then
    log_error "Downloaded file is not valid JSON"
    rm -f "$TEMP_FILE"
    exit 1
  fi

  # Create output directory if it doesn't exist
  mkdir -p "$OUTPUT_DIR"

  # Move schema to final location
  mv "$TEMP_FILE" "$OUTPUT_FILE"

  # Create attribution file
  local download_date=$(date +"%Y-%m-%d")
  local attribution_file="${OUTPUT_DIR}/nbformat-schema-LICENSE.txt"

  cat > "$attribution_file" <<EOF
Jupyter Notebook Format v4 JSON Schema
=======================================

Source: https://github.com/${NBFORMAT_REPO}
License: BSD-3-Clause
Copyright (c) Jupyter Development Team

This schema file (nbformat.v4.schema.json) is part of the nbformat package
and is used to validate Jupyter Notebook v4 format. The schema is maintained
by the Jupyter project.

Downloaded from: ${schema_url}
Date: ${download_date}
Version/Tag: ${latest_tag}

For the latest version, see: https://github.com/${NBFORMAT_REPO}
For the full BSD-3-Clause license text, see:
https://github.com/${NBFORMAT_REPO}/blob/main/LICENSE

---

BSD 3-Clause License

Copyright (c) Jupyter Development Team.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
EOF

  log_success "Created attribution file: ${attribution_file}"

  # Clean up
  rm -f "$TEMP_FILE"

  log_success "Schema updated successfully: ${OUTPUT_FILE}"
  log_info "Version: ${latest_tag}"
  log_info "Date: ${download_date}"

  # Show file size
  local file_size=$(du -h "$OUTPUT_FILE" | awk '{print $1}')
  log_info "File size: ${file_size}"

  # Verify with Java that it's still a valid JSON Schema
  log_info "Note: You should rebuild and run tests to verify the schema is compatible"
}

main "$@"
