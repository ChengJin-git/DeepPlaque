---
license: cc-by-nc-nd-4.0
language:
  - en
tags:
  - image-segmentation
  - instance-segmentation
  - medical-imaging
  - pathology
  - alzheimers-disease
  - amyloid-plaque
library_name: mmdet
pipeline_tag: image-segmentation
---

# PlaqueNet

A deep learning model for detecting and classifying amyloid plaques in brain tissue images.

## Model Description

PlaqueNet is a Mask R-CNN based instance segmentation model trained to detect three types of amyloid plaques:
- **Cored**: Dense-core plaques with compact amyloid deposits
- **Diffuse**: Loosely distributed amyloid deposits
- **Fibrillar**: Fibrous amyloid structures

## Usage

### Installation

```bash
# Clone the DeepPlaque repository
git clone https://github.com/ChengJin-git/DeepPlaque.git
cd DeepPlaque

# Create conda environment
conda env create -f environment.yml
conda activate deepplaque
```

### Inference

Weights are automatically downloaded from Hugging Face when you run inference:

```bash
cd pipeline/02_inference

python inference.py \
    /path/to/image/folder \
    configs/inference_config.py \
    --out-dir /path/to/output \
    --device cuda:0 \
    --pred-score-thr 0.45
```

> Weights are automatically downloaded from `peterjin0703/PlaqueNet` by default.
> For offline use, download manually and specify: `--weights /path/to/PlaqueNet.pth`

### Python API

```python
from huggingface_hub import hf_hub_download
from mmdet.apis import DetInferencer

# Download weights
weights_path = hf_hub_download(
    repo_id="peterjin0703/PlaqueNet",
    filename="PlaqueNet.pth"
)

# Initialize inferencer
inferencer = DetInferencer(
    model='path/to/inference_config.py',
    weights=weights_path,
    device='cuda:0'
)

# Run inference
results = inferencer(
    'path/to/image.png',
    pred_score_thr=0.45,
    out_dir='output/'
)
```

### Output Format

The model outputs:
- **Visualization images**: With bounding boxes, masks, and class labels
- **JSON results**: COCO-format detection results per image
- **GeoJSON files**: QuPath-compatible annotations for pathology analysis

Please refer to the original GitHub repo for the complete pipeline: https://github.com/ChengJin-git/DeepPlaque

## Model Architecture

- **Backbone**: Swin Transformer with FPN
- **Head**: Mask R-CNN
- **Framework**: MMDetection 3.3.0
- **Input**: RGB images (any resolution, internally resized)

## Training Data

Trained on annotated brain tissue images with expert-labeled amyloid plaques.

## License

CC-BY-NC-ND 4.0

