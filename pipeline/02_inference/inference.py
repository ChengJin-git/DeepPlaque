# Patch mmdet to remove mmcv version check before importing
# mmdet 3.3.0 requires mmcv<2.2.0, but mmcv 2.2.0 works fine in our practice.
def _patch_mmdet_version_check():
    import importlib.util
    spec = importlib.util.find_spec('mmdet')
    if spec is None or spec.origin is None:
        return
    init_file = spec.origin
    try:
        with open(init_file, 'r') as f:
            content = f.read()
        # Check if patch is needed
        old_assert = """assert (mmcv_version >= digit_version(mmcv_minimum_version)
        and mmcv_version < digit_version(mmcv_maximum_version)), \\
    f'MMCV=={mmcv.__version__} is used but incompatible. ' \\
    f'Please install mmcv>={mmcv_minimum_version}, <{mmcv_maximum_version}.'"""
        if old_assert in content:
            patched = content.replace(old_assert, '# mmcv version check disabled by DeepPlaque')
            with open(init_file, 'w') as f:
                f.write(patched)
    except (PermissionError, IOError):
        pass  # Skip if no write permission

_patch_mmdet_version_check()

import os
import json
import uuid
from argparse import ArgumentParser

import cv2
from pycocotools import mask as mask_util
from mmengine.logging import print_log
from mmdet.apis import DetInferencer

# Class names for QuPath GeoJSON export
CLASS_NAMES = ['cored', 'diffuse', 'fibrillar']

# QuPath GeoJSON colors (RGB)
CLASS_COLORS = {
    'cored': [255, 0, 0],      # Red
    'diffuse': [0, 255, 0],    # Green
    'fibrillar': [0, 0, 255],  # Blue
}


def rle_to_polygons(rle_mask):
    """Convert RLE mask to polygon contours."""
    binary_mask = mask_util.decode(rle_mask)
    contours, _ = cv2.findContours(binary_mask, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    polygons = []
    for contour in contours:
        if contour.size >= 6:  # At least 3 points
            polygons.append(contour)
    return polygons


def convert_to_geojson(json_path, output_path, score_threshold=0.45):
    """Convert mmdet JSON output to QuPath-compatible GeoJSON."""
    with open(json_path, 'r') as f:
        data = json.load(f)

    # Filter by score threshold
    valid_indices = [i for i, score in enumerate(data['scores']) if score >= score_threshold]

    features = []
    for idx in valid_indices:
        label_id = data['labels'][idx]
        class_name = CLASS_NAMES[label_id] if label_id < len(CLASS_NAMES) else 'unknown'
        rle_mask = data['masks'][idx]

        # Convert RLE to polygons
        polygons = rle_to_polygons(rle_mask)
        if not polygons:
            continue

        # Convert numpy contours to coordinate lists
        coords = []
        for poly in polygons:
            ring = [[int(pt[0][0]), int(pt[0][1])] for pt in poly]
            ring.append(ring[0])  # Close the ring
            coords.append(ring)

        # Determine geometry type
        if len(coords) > 1:
            geom_type = 'MultiPolygon'
            coordinates = [[ring] for ring in coords]
        else:
            geom_type = 'Polygon'
            coordinates = coords

        feature = {
            "type": "Feature",
            "id": str(uuid.uuid4()),
            "geometry": {
                "type": geom_type,
                "coordinates": coordinates
            },
            "properties": {
                "objectType": "annotation",
                "name": f"Plaque_{idx + 1}",
                "classification": {
                    "name": class_name,
                    "color": CLASS_COLORS.get(class_name, [128, 128, 128])
                },
                "isLocked": True
            }
        }
        features.append(feature)

    # Write GeoJSON
    with open(output_path, 'w') as f:
        json.dump(features, f, indent=2)

    return len(features)


def convert_all_to_geojson(preds_dir, geojson_dir, score_threshold=0.45):
    """Convert all JSON files in preds directory to GeoJSON."""
    os.makedirs(geojson_dir, exist_ok=True)

    json_files = [f for f in os.listdir(preds_dir) if f.endswith('.json')]
    total_converted = 0

    for json_file in json_files:
        json_path = os.path.join(preds_dir, json_file)
        geojson_path = os.path.join(geojson_dir, json_file.replace('.json', '.geojson'))

        try:
            count = convert_to_geojson(json_path, geojson_path, score_threshold)
            print_log(f'Converted {json_file}: {count} annotations')
            total_converted += 1
        except Exception as e:
            print_log(f'Failed to convert {json_file}: {e}', level='WARNING')

    return total_converted


def parse_args():
    parser = ArgumentParser()
    parser.add_argument(
        'inputs', type=str, help='Input image file or folder path.',)
    parser.add_argument(
        'model',
        type=str,
        help='Config or checkpoint .pth file or the model name '
        'and alias defined in metafile. The model configuration '
        'file will try to read from .pth if the parameter is '
        'a .pth weights file.')
    parser.add_argument('--weights', default='peterjin0703/PlaqueNet',
                        help='Checkpoint file or HuggingFace model ID')
    parser.add_argument(
        '--out-dir',
        type=str,
        default='outputs',
        help='Output directory of images or prediction results.')
    parser.add_argument('--texts', help='text prompt')
    parser.add_argument(
        '--device', default='cuda:0', help='Device used for inference')
    parser.add_argument(
        '--pred-score-thr',
        type=float,
        default=0.45,
        help='bbox score threshold')
    parser.add_argument(
        '--batch-size', type=int, default=1, help='Inference batch size.')
    parser.add_argument(
        '--show',
        action='store_true',
        help='Display the image in a popup window.')
    parser.add_argument(
        '--no-save-vis',
        action='store_true',
        help='Do not save detection vis results')
    parser.add_argument(
        '--no-save-pred',
        action='store_true',
        help='Do not save detection json results')
    parser.add_argument(
        '--print-result',
        action='store_true',
        help='Whether to print the results.')
    parser.add_argument(
        '--palette',
        default='none',
        choices=['coco', 'voc', 'citys', 'random', 'none'],
        help='Color palette used for visualization')
    # only for GLIP
    parser.add_argument(
        '--custom-entities',
        '-c',
        action='store_true',
        help='Whether to customize entity names? '
        'If so, the input text should be '
        '"cls_name1 . cls_name2 . cls_name3 ." format')

    call_args = vars(parser.parse_args())

    if call_args['no_save_vis'] and call_args['no_save_pred']:
        call_args['out_dir'] = ''

    if call_args['model'].endswith('.pth'):
        print_log('The model is a weight file, automatically '
                  'assign the model to --weights')
        call_args['weights'] = call_args['model']
        call_args['model'] = None

    init_kws = ['model', 'weights', 'device', 'palette']
    init_args = {}
    for init_kw in init_kws:
        init_args[init_kw] = call_args.pop(init_kw)

    # Support HuggingFace model download (format: username/repo or hf://username/repo)
    weights = init_args.get('weights')
    if weights and not os.path.exists(weights):
        hf_id = weights.replace('hf://', '') if weights.startswith('hf://') else weights
        if '/' in hf_id and not hf_id.startswith('/'):
            try:
                from huggingface_hub import hf_hub_download
            except ImportError:
                raise ImportError(
                    'huggingface_hub is required to download weights. '
                    'Install it with: pip install huggingface_hub\n'
                    'Or download weights manually and use: --weights /path/to/PlaqueNet.pth'
                )
            print_log(f'Downloading weights from HuggingFace: {hf_id}')
            init_args['weights'] = hf_hub_download(
                repo_id=hf_id,
                filename='PlaqueNet.pth'
            )
            print_log(f'Weights downloaded to: {init_args["weights"]}')

    return init_args, call_args


def main():
    init_args, call_args = parse_args()
    score_thr = call_args.get('pred_score_thr', 0.45)

    inferencer = DetInferencer(**init_args)
    inferencer(**call_args)

    out_dir = call_args['out_dir']
    if out_dir != '' and not (call_args['no_save_vis'] and call_args['no_save_pred']):
        print_log(f'Results saved at {out_dir}')

        # Convert predictions to QuPath GeoJSON
        preds_dir = os.path.join(out_dir, 'preds')
        geojson_dir = os.path.join(out_dir, 'geojsons')

        if os.path.isdir(preds_dir):
            print_log('Converting predictions to QuPath GeoJSON format...')
            count = convert_all_to_geojson(preds_dir, geojson_dir, score_thr)
            print_log(f'GeoJSON conversion complete: {count} files saved to {geojson_dir}')


if __name__ == '__main__':
    main()