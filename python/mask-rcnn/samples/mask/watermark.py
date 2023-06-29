"""
Mask R-CNN
Train on the image mask dataset and detection.

Copyright (c) 2018 Matterport, Inc.
Licensed under the MIT License (see LICENSE for details)
Written by Waleed Abdulla

------------------------------------------------------------

Usage: run from the command line as such:

    # Train a new model starting from pre-trained COCO weights
    python watermark.py train --dataset=/path/to/watermark/dataset --weights=coco

    # Resume training a model that you had trained earlier
    python watermark.py train --dataset=/path/to/watermark/dataset --weights=last

    # Train a new model starting from ImageNet weights
    python watermark.py train --dataset=/path/to/watermark/dataset --weights=imagenet

    # Detect watermark from image
    python watermark.py detect --weights=/path/to/weights/file.h5 --image=<URL or path to file>

"""

import cv2
import os
import sys

import numpy as np
import skimage.draw

# Root directory of the project
ROOT_DIR = os.path.abspath("../../")

# Import Mask RCNN
sys.path.append(ROOT_DIR)  # To find local version of the library
from mrcnn.config import Config
from mrcnn import model as modellib, utils
from mrcnn import visualize, tfserve

# Path to trained weights file
COCO_WEIGHTS_PATH = os.path.join(ROOT_DIR, "model/mask_rcnn_coco.h5")

# Directory to save logs and model checkpoints, if not provided
# through the command line argument --logs
DEFAULT_LOGS_DIR = os.path.join(ROOT_DIR, "logs")
FULL_CLASS_NAMES = ['BG', 'watermark']


############################################################
#  Configurations
############################################################


class WatermarkConfig(Config):
    """Configuration for training on the toy  dataset.
    Derives from the base Config class and overrides some values.
    """
    # Give the configuration a recognizable name
    NAME = "watermark"

    # We use a GPU with 12GB memory, which can fit two images.
    # Adjust down if you use a smaller GPU.
    IMAGES_PER_GPU = 1

    IMAGE_MIN_DIM = 256
    # Number of classes (including background)
    NUM_CLASSES = len(FULL_CLASS_NAMES)

    # Number of training steps per epoch
    STEPS_PER_EPOCH = 1000

    # Skip detections with < 90% confidence
    DETECTION_MIN_CONFIDENCE = 0.9


############################################################
#  Dataset
############################################################


class MaskDataset(utils.Dataset):

    def load_dataset(self, dataset_dir, subset):
        """Load a subset of the Mask dataset.
        dataset_dir: Root directory of the dataset.
        subset: Subset to load: train or val
        """
        # Add classes. We have only one class to add.
        for index in range(1, len(FULL_CLASS_NAMES)):
            self.add_class("watermark", index, FULL_CLASS_NAMES[index])
        # Train or validation dataset?
        assert subset in ["train", "val"]
        dataset_dir = os.path.join(dataset_dir, subset)

        for file_name in os.listdir(dataset_dir):
            file_path = os.path.join(dataset_dir, file_name)
            if os.path.isdir(file_path):
                continue
            names = file_name.split('.')
            if names[-1] != 'jpg':
                continue
            regions = []
            region_path = os.path.join(dataset_dir, '.'.join(names[:-1]))
            if os.path.isdir(region_path):
                for mask_name in os.listdir(region_path):
                    mask_path = os.path.join(region_path, mask_name)
                    if os.path.isdir(mask_path):
                        continue
                    classes = mask_name.split('.')
                    if classes[-1] != 'png':
                        continue
                    regions.append({
                        'name': 'watermark',
                        'mask': cv2.cvtColor(cv2.imread(mask_path), cv2.COLOR_BGRA2GRAY)
                    })
            image = skimage.io.imread(file_path)
            height, width = image.shape[:2]
            self.add_image(
                "watermark",
                image_id=file_name,  # use file name as a unique image id
                path=file_path,
                width=width, height=height,
                regions=regions)


    def load_mask(self, image_id):
        """Generate instance masks for an image.
       Returns:
        masks: A bool array of shape [height, width, instance count] with
            one mask per instance.
        class_ids: a 1D array of class IDs of the instance masks.
        """
        # If not a mask dataset image, delegate to parent class.
        image_info = self.image_info[image_id]
        if image_info["source"] != "watermark":
            return super(self.__class__, self).load_mask(image_id)

        # Convert polygons to a bitmap mask of shape
        # [height, width, instance_count]
        info = self.image_info[image_id]
        mask = np.zeros([info["height"], info["width"], len(info["regions"])], dtype=np.uint8)
        class_ids = np.zeros(len(info["regions"]), dtype=np.int32)
        for i, p in enumerate(info["regions"]):
            rr, cc = np.where(p['mask'] > 200)
            mask[rr, cc, i] = 1
            class_ids[i] = self.class_names.index(p['name'])
        return mask.astype(np.bool), class_ids

    def image_reference(self, image_id):
        """Return the path of the image."""
        info = self.image_info[image_id]
        if info["source"] == "watermark":
            return info["path"]
        else:
            super(self.__class__, self).image_reference(image_id)


def train(model):
    """Train the model."""
    # Training dataset.
    dataset_train = MaskDataset()
    dataset_train.load_dataset(args.dataset, "train")
    dataset_train.prepare()

    # Validation dataset
    dataset_val = MaskDataset()
    dataset_val.load_dataset(args.dataset, "val")
    dataset_val.prepare()

    # *** This training schedule is an example. Update to your needs ***
    # Since we're using a very small dataset, and starting from
    # COCO trained weights, we don't need to train too long. Also,
    # no need to train all layers, just the heads should do it.
    print("Training network heads")
    model.train(dataset_train, dataset_val,
                learning_rate=config.LEARNING_RATE,
                epochs=500,
                layers='heads')


def detect(model, image_path=None):
    assert image_path

    print("Running on {}".format(args.image))
    # Read image
    image = skimage.io.imread(args.image)
    # Detect objects
    r = model.detect([image], verbose=1)[0]
    visualize.display_instances(image, r['rois'], r['masks'], r['class_ids'],
                                FULL_CLASS_NAMES, r['scores'])


############################################################
#  Training
############################################################

if __name__ == '__main__':
    import argparse

    # Parse command line arguments
    parser = argparse.ArgumentParser(
        description='Train Mask R-CNN to detect masks.')
    parser.add_argument("command",
                        metavar="<command>",
                        help="'train' or 'detect'")
    parser.add_argument('--dataset', required=False,
                        metavar="/path/to/mask/dataset/",
                        help='Directory of the Mask dataset')
    parser.add_argument('--weights', required=True,
                        metavar="/path/to/weights.h5",
                        help="Path to weights .h5 file or 'coco'")
    parser.add_argument('--logs', required=False,
                        default=DEFAULT_LOGS_DIR,
                        metavar="/path/to/logs/",
                        help='Logs and checkpoints directory (default=logs/)')
    parser.add_argument('--image', required=False,
                        metavar="path or URL to image",
                        help='Image to apply the detect')
    parser.add_argument('--export', required=False,
                        metavar="path to export the saved model",
                        help='Path to apply the export')
    args = parser.parse_args()

    # Validate arguments
    if args.command == "train":
        assert args.dataset, "Argument --dataset is required for training"
    elif args.command == "detect":
        assert args.image, "Provide --image to detect"
    elif args.command == "export":
        assert args.export, "Provide --export to save"

    print("Weights: ", args.weights)
    print("Dataset: ", args.dataset)
    print("Logs: ", args.logs)

    # Configurations
    if args.command == "train":
        config = WatermarkConfig()
    else:
        class InferenceConfig(WatermarkConfig):
            # Set batch size to 1 since we'll be running inference on
            # one image at a time. Batch size = GPU_COUNT * IMAGES_PER_GPU
            GPU_COUNT = 1
            IMAGES_PER_GPU = 1
        config = InferenceConfig()
    config.display()

    # Create model
    if args.command == "train":
        model = modellib.MaskRCNN(mode="training", config=config,
                                  model_dir=args.logs)
    else:
        model = modellib.MaskRCNN(mode="inference", config=config,
                                  model_dir=args.logs)

    # Select weights file to load
    if args.weights.lower() == "coco":
        weights_path = COCO_WEIGHTS_PATH
        # Download weights file
        if not os.path.exists(weights_path):
            utils.download_trained_weights(weights_path)
    elif args.weights.lower() == "last":
        # Find last trained weights
        weights_path = model.find_last()
    elif args.weights.lower() == "imagenet":
        # Start from ImageNet trained weights
        weights_path = model.get_imagenet_weights()
    else:
        weights_path = args.weights

    # Load weights
    print("Loading weights ", weights_path)
    if args.weights.lower() == "coco":
        # Exclude the last layers because they require a matching
        # number of classes
        model.load_weights(weights_path, by_name=True, exclude=[
            "mrcnn_class_logits", "mrcnn_bbox_fc",
            "mrcnn_bbox", "mrcnn_mask"])
    else:
        model.load_weights(weights_path, by_name=True)

    # Train or evaluate
    if args.command == "train":
        train(model)
    elif args.command == "detect":
        detect(model, image_path=args.image)
    elif args.command == "export":
        tfserve.export(model, export_path=args.export)
    else:
        print("'{}' is not recognized. Use 'train' or 'detect'".format(args.command))
