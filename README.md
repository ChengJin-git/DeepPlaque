# DeepPlaque

<div align="center">

**A Scalable Platform for Multimodal Investigation of Aβ Pathology and Brain Cell Analysis in Alzheimer's Disease**

[![License: CC BY-NC-ND 4.0](https://img.shields.io/badge/License-CC%20BY--NC--ND%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc-nd/4.0/)
[![Python 3.8+](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org/downloads/)
[![PyTorch](https://img.shields.io/badge/PyTorch-%23EE4C2C.svg?style=flat&logo=PyTorch&logoColor=white)](https://pytorch.org/)
[![MMDetection](https://img.shields.io/badge/MMDetection-3.0-blueviolet)](https://github.com/open-mmlab/mmdetection)

</div>

---

## 📝 Abstract

**DeepPlaque** is a comprehensive, end-to-end computational framework designed for the automated detection, classification, and spatial analysis of Beta-amyloid (Aβ) plaques in postmortem human brain tissues. Leveraging state-of-the-art deep learning architectures—specifically a **Swin Transformer-based Mask R-CNN**—DeepPlaque achieves expert-level performance (AUC > 0.90) in distinguishing between distinct plaque morphologies: **diffuse**, **fibrillar**, and **cored**.

Beyond simple classification, the platform integrates seamless workflows for Whole Slide Image (WSI) preprocessing, cellular phenotyping via QuPath, and targeted Laser Microdissection (LMD) for spatial proteomics. This tool enables researchers to perform high-throughput, quantitative analysis of Aβ pathology and its microenvironmental interactions at scale.

## ✨ Key Features

*   **State-of-the-Art Architecture**: Utilizes a Swin Transformer backbone for superior feature extraction and robust instance segmentation of complex plaque structures.
*   **Morphological Classification**: Automatically categorizes plaques into three biologically distinct types:
    *   **Diffuse**: Early-stage, loosely aggregated deposits.
    *   **Fibrillar**: Intermediate, fibril-structured plaques.
    *   **Cored**: Mature, dense-core neuritic plaques.
*   **Spatial Cellular Phenotyping**: Integrated QuPath scripts for analyzing the spatial relationship between plaques and surrounding glial cells (microglia, astrocytes).
*   **End-to-End Pipeline**: A unified workflow covering raw image import, patch generation, deep learning inference, and quantitative reporting.
*   **Spatial Proteomics Ready**: Includes specialized modules for generating Laser Microdissection (LMD) maps for proteomic profiling of specific plaque niches.

## 📂 Repository Structure

The repository is organized into three main pipeline stages and figure-specific scripts for reproducibility.

```text
DeepPlaque/
├── pipeline/                          # Core Analysis Pipeline
│   ├── 01_preprocessing/              # Stage 1: ImageJ/Fiji macros for WSI processing
│   ├── 02_inference/                  # Stage 2: Deep Learning Inference (Python/PyTorch)
│   │   ├── configs/                   # Model configurations (MMDetection)
│   │   ├── weights/                   # Pre-trained model weights
│   │   └── inference.py               # Main inference engine
│   └── 03_postprocessing/             # Stage 3: QuPath scripts for spatial analysis
│
├── scripts/                           # Reproducibility Scripts
│   ├── Fig2/                          # Scripts for Figure 2 generation
│   ├── Fig3/                          # Scripts for Figure 3 generation
│   └── Fig4/                          # Scripts for Figure 4 (Spatial Proteomics)
│
├── environment.yml                    # Conda environment specification
└── README.md                          # Documentation
```

## 🚀 Installation

### Prerequisites

*   **OS**: Linux (Recommended), Windows, or macOS.
*   **GPU**: NVIDIA GPU with CUDA 12.1+ support is highly recommended for inference.
*   **Software**:
    *   [Miniconda](https://docs.conda.io/en/latest/miniconda.html) or Anaconda
    *   [Fiji (ImageJ)](https://imagej.net/software/fiji/)
    *   [QuPath](https://qupath.github.io/) (v0.4.0+)

### Environment Setup

We provide a `environment.yml` file for easy setup using Conda.

```bash
# 1. Clone the repository
git clone https://github.com/your-username/DeepPlaque.git
cd DeepPlaque

# 2. Create the Conda environment
conda env create -f environment.yml

# 3. Activate the environment
conda activate deepplaque

# 4. Verify CUDA availability (Optional but recommended)
python -c "import torch; print('CUDA available:', torch.cuda.is_available())"
```

## ⚡ Usage Pipeline

The DeepPlaque workflow is divided into three sequential stages.

### Stage 1: Preprocessing (Fiji/ImageJ)
**Goal**: Convert raw Whole Slide Images (WSI) into standardized composite TIFF patches.

1.  Open **Fiji**.
2.  Navigate to `Plugins` > `Macros` > `Run...`.
3.  Select the appropriate script from `pipeline/01_preprocessing/`:
    *   Use `01_BioFormats_import_Leica.ijm` for Leica `.lif` files.
    *   Use `02_BioFormats_import_Zeiss.ijm` for Zeiss `.czi` files.
4.  **Note**: Ensure you edit the input/output paths within the script before running.

### Stage 2: Inference (Python/PlaqueNet)
**Goal**: Detect and classify plaques using the pre-trained PlaqueNet model.

```bash
conda activate deepplaque
cd pipeline/02_inference

# Run inference
python inference.py \
    /path/to/input/composite_patches \
    configs/inference_config.py \
    --out-dir /path/to/output/results \
    --device cuda \
    --pred-score-thr 0.45
```

| Argument | Description |
| :--- | :--- |
| `input_dir` | Path to the folder containing composite TIFF patches from Stage 1. |
| `config` | Path to the model configuration file. |
| `--out-dir` | Directory where results (visualizations and GeoJSONs) will be saved. |
| `--device` | Compute device (`cuda` or `cpu`). |
| `--pred-score-thr` | Confidence threshold for detection (default: 0.45). |

### Stage 3: Postprocessing (QuPath)
**Goal**: Import predictions, perform spatial analysis, and export quantitative metrics.

1.  Open **QuPath** and create a project.
2.  Open the Script Editor (`Automate` > `Script Editor`).
3.  Run the scripts in `pipeline/03_postprocessing/` sequentially:
    *   `06_Re-import_JSON_predicted_class.groovy`: Imports the GeoJSON predictions from Stage 2.
    *   `08e_NearestNeighbourDistance.groovy`: Calculates spatial statistics.
    *   `09a_MeasurementExporter...`: Exports final data to CSV.

## 🤖 Model Download

Pre-trained weights for PlaqueNet are available at [Hugging Face](https://huggingface.co/peterjin0703/PlaqueNet) and will be automatically downloaded by the inference script.


## 📄 License

This project is licensed under the **Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International (CC BY-NC-ND 4.0)**. See the [LICENSE](LICENSE) file for details.