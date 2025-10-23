#!/bin/bash

# Performance test for sequential notebook uploads
# Usage: ./perf-test.sh <count> [size_kb]
#   count: Number of notebooks to upload sequentially
#   size_kb: Size of each notebook in KB (default: minimal ~1KB)
#
# Examples:
#   ./perf-test.sh 100              # 100 minimal notebooks
#   ./perf-test.sh 100 5120         # 100 notebooks of ~5MB each

set -e

BASE_URL="${API_URL:-http://localhost:8080/api/v1}"
EXTRA_AUTH_HEADER_NAME="${EXTRA_AUTH_HEADER_NAME:-X-Extra-Auth}"
EXTRA_AUTH_HEADER_SECRET="${EXTRA_AUTH_HEADER_SECRET:-secret}"

# Parse arguments
COUNT=${1:-10}
NOTEBOOK_SIZE_KB=${2:-1}

# Get auth token
TOKEN=$(curl -sf -X POST "$BASE_URL/auth/issue" \
    -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" | jq -r '.token')

# Generate notebook with specified size
TARGET_BYTES=$((NOTEBOOK_SIZE_KB * 1024))
BASE_SIZE=600
PADDING_BYTES=$((TARGET_BYTES - BASE_SIZE))

if [ $PADDING_BYTES -gt 0 ]; then
    temp_nb=$(mktemp)
    dd if=/dev/zero bs=1 count="$PADDING_BYTES" 2>/dev/null | tr '\000' 'X' | jq -Rs '{
        nbformat: 4,
        nbformat_minor: 5,
        metadata: {
            kernelspec: {
                display_name: "Python 3",
                language: "python",
                name: "python3"
            },
            language_info: {
                name: "python",
                version: "3.9.0",
                mimetype: "text/x-python",
                file_extension: ".py",
                codemirror_mode: {name: "ipython", version: 3}
            }
        },
        cells: [
            {
                id: "cell1",
                cell_type: "code",
                source: ["print(\"Performance test\")"],
                metadata: {},
                outputs: [],
                execution_count: null
            },
            {
                id: "cell2",
                cell_type: "markdown",
                source: [.],
                metadata: {}
            }
        ]
    }' > "$temp_nb"
    NOTEBOOK_JSON=$(cat "$temp_nb")
    rm -f "$temp_nb"
else
    NOTEBOOK_JSON=$(cat <<'EOF'
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
      "mimetype": "text/x-python",
      "file_extension": ".py",
      "codemirror_mode": {"name": "ipython", "version": 3}
    }
  },
  "cells": [
    {
      "id": "cell1",
      "cell_type": "code",
      "source": ["print('Performance test')"],
      "metadata": {},
      "outputs": [],
      "execution_count": null
    }
  ]
}
EOF
)
fi

# Sequential uploads
start_time=$(date +%s)

for i in $(seq 1 "$COUNT"); do
    temp_request=$(mktemp)
    echo "$NOTEBOOK_JSON" | jq -s '{password: "", notebook: .[0]}' > "$temp_request"

    curl -s -X POST "$BASE_URL/notebooks" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -H "${EXTRA_AUTH_HEADER_NAME}: ${EXTRA_AUTH_HEADER_SECRET}" \
        --data-binary @"$temp_request" > /dev/null

    rm -f "$temp_request"
done

end_time=$(date +%s)
total_time=$((end_time - start_time))

echo "Uploaded $COUNT notebooks (${NOTEBOOK_SIZE_KB}KB each) in ${total_time}s"
