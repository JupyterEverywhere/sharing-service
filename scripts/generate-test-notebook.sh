#!/usr/bin/env bash

# Generate test notebooks of various sizes for performance testing

set -eou pipefail

# Configuration
readonly SIZE_KB=${1:-5120}  # Default 5MB
readonly OUTPUT_FILE=${2:-scripts/test-notebook.ipynb}

# Calculate number of cells needed to reach target size
# Each cell with output is roughly 10-15KB
readonly CELLS_NEEDED=$((SIZE_KB / 12))

echo "Generating ${SIZE_KB}KB notebook with ~${CELLS_NEEDED} cells..." >&2

python3 -c "
import json
import sys

target_kb = $SIZE_KB
cells_needed = $CELLS_NEEDED

# Create cells with outputs to bulk up size
cells = []
for i in range(cells_needed):
    cells.append({
        'cell_type': 'code',
        'execution_count': i + 1,
        'id': f'cell-{i}',
        'metadata': {},
        'outputs': [
            {
                'output_type': 'stream',
                'name': 'stdout',
                'text': ['Line ' + str(j) + '\n' for j in range(500)]
            }
        ],
        'source': [f'print(\"Cell {i}\")\n' for _ in range(10)]
    })

notebook = {
    'cells': cells,
    'metadata': {
        'kernelspec': {
            'display_name': 'Python 3',
            'language': 'python',
            'name': 'python3'
        },
        'language_info': {
            'name': 'python',
            'version': '3.9.0'
        }
    },
    'nbformat': 4,
    'nbformat_minor': 5
}

with open('$OUTPUT_FILE', 'w') as f:
    json.dump(notebook, f)

# Report actual size
import os
actual_kb = os.path.getsize('$OUTPUT_FILE') / 1024
print(f'Created notebook: $OUTPUT_FILE ({actual_kb:.1f}KB)', file=sys.stderr)
"
