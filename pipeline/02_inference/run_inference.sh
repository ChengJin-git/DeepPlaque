#!/bin/bash
# DeepPlaque Inference Script
#
# Usage: ./run_inference.sh <input_images/folder> <output_dir> [score_threshold]
#
# Examples:
#   ./run_inference.sh /path/to/images /path/to/output
#   ./run_inference.sh /path/to/images /path/to/output 0.45

set -e

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Default parameters
INPUT_PATH="${1:?Error: Please provide input image path or folder}"
OUTPUT_DIR="${2:?Error: Please provide output directory}"
SCORE_THR="${3:-0.45}"

# Config and weights paths
CONFIG="${SCRIPT_DIR}/configs/inference_config.py"
WEIGHTS="${SCRIPT_DIR}/weights/PlaqueNet.pth"

# Check if files exist
if [ ! -f "$CONFIG" ]; then
    echo "Error: Config file not found: $CONFIG"
    exit 1
fi

if [ ! -f "$WEIGHTS" ]; then
    echo "Error: Weights file not found: $WEIGHTS"
    exit 1
fi

# Run inference
echo "=========================================="
echo "DeepPlaque Inference"
echo "=========================================="
echo "Input path: $INPUT_PATH"
echo "Output dir: $OUTPUT_DIR"
echo "Score threshold: $SCORE_THR"
echo "=========================================="

python "${SCRIPT_DIR}/inference.py" \
    "$INPUT_PATH" \
    "$CONFIG" \
    --weights "$WEIGHTS" \
    --out-dir "$OUTPUT_DIR" \
    --palette coco \
    --device cuda \
    --pred-score-thr "$SCORE_THR"

echo "=========================================="
echo "Inference complete! Results saved to: $OUTPUT_DIR"
echo "=========================================="

