# ML Model Setup Guide

## Required Models

Place these TFLite model files in `androidApp/src/main/assets/models/`:

### 1. Object Detection — `detect.tflite` + `labelmap.txt`
**Model:** SSD MobileNet v2 (COCO)
**Size:** ~4 MB (quantized)
**Download:**
```bash
# From TensorFlow Hub
wget https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip
unzip coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip
cp detect.tflite androidApp/src/main/assets/models/
cp labelmap.txt androidApp/src/main/assets/models/
```

Or from TFLite Model Zoo:
- https://www.tensorflow.org/lite/examples/object_detection/overview

### 2. Image Classification — `label.tflite` + `labels.txt`
**Model:** MobileNet v2 (ImageNet)
**Size:** ~3 MB (quantized)
**Download:**
```bash
wget https://storage.googleapis.com/download.tensorflow.org/models/tflite_11_05_08/mobilenet_v2_1.0_224_quant.tgz
tar xzf mobilenet_v2_1.0_224_quant.tgz
cp mobilenet_v2_1.0_224_quant.tflite androidApp/src/main/assets/models/label.tflite
```

For labels file:
```bash
wget https://storage.googleapis.com/download.tensorflow.org/models/mobilenet_v1_1.0_224_frozen.tgz
# Extract labels_mobilenet_quant_v1_224.txt and rename to labels.txt
```

### 3. Sound Classification — `yamnet.tflite` + `yamnet_labels.txt`
**Model:** YAMNet (AudioSet)
**Size:** ~3 MB
**Download:**
```bash
# From TFLite Model Hub
wget https://tfhub.dev/google/lite-model/yamnet/classification/tflite/1?lite-format=tflite -O yamnet.tflite
cp yamnet.tflite androidApp/src/main/assets/models/
```

For labels:
```bash
# YAMNet class map (521 classes)
wget https://raw.githubusercontent.com/tensorflow/models/master/research/audioset/yamnet/yamnet_class_map.csv
# Convert CSV to txt (one label per line, using display_name column)
awk -F',' 'NR>1 {print $3}' yamnet_class_map.csv | tr -d '"' > yamnet_labels.txt
cp yamnet_labels.txt androidApp/src/main/assets/models/
```

## Directory Structure After Setup
```
androidApp/src/main/assets/models/
├── detect.tflite          # Object detection model
├── labelmap.txt           # COCO object labels (90 classes)
├── label.tflite           # Image classification model
├── labels.txt             # ImageNet labels (1001 classes)
├── yamnet.tflite          # Sound classification model
└── yamnet_labels.txt      # AudioSet labels (521 classes)
```

## Running Without Models

The app will work without model files — ML features will return
informative error messages instead of crashing. This lets you
develop and test the UI, camera, and audio pipeline independently.

## Custom Model Training

For better accessibility-specific results, you can fine-tune:

1. **Object Detection** — Fine-tune on accessibility-relevant objects
   (wheelchair ramps, crosswalk buttons, escalators, elevators)
2. **Sound Classification** — Fine-tune YAMNet on specific alert sounds
   with your own recordings for higher accuracy
3. **Image Captioning** — Train a custom BLIP/GIT model and convert
   to TFLite for more descriptive captions

See `ml-models/` directory (to be added) for training scripts.
