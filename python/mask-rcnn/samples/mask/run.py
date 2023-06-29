import argparse
import os
import skimage.io
import numpy as np
import sys

# Root directory of the project
ROOT_DIR = os.path.abspath("../../")
sys.path.append(ROOT_DIR)  # To find local version of the library

from mrcnn.config import Config
from mrcnn import visualize, tfserve
from mrcnn import model as modellib, utils


DEFAULT_LOGS_DIR = os.path.join(ROOT_DIR, "logs")
FULL_CLASS_NAMES = ['BG', 'watermark']


class RunConfig(Config):
    NAME = "watermark"
    IMAGES_PER_GPU = 1
    IMAGE_MIN_DIM = 256
    NUM_CLASSES = len(FULL_CLASS_NAMES)
    STEPS_PER_EPOCH = 1000
    DETECTION_MIN_CONFIDENCE = 0.9


def run(args, model):
    print("Running on {}".format(args.image))

    # Read image
    image = skimage.io.imread(args.image)
    images = [image]
    molded_images, image_metas, windows = model.mold_inputs(images)
    image_shape = molded_images[0].shape
    # Anchors
    anchors = model.get_anchors(image_shape)
    anchors = np.broadcast_to(anchors, (model.config.BATCH_SIZE,) + anchors.shape)

    detections, _, _, mrcnn_mask, _, _, _ = tfserve.predict(args.export, {
        'input_image': molded_images,
        'input_image_meta': image_metas,
        'input_anchors': anchors,
    })

    results = []
    for i, image in enumerate(images):
        final_rois, final_class_ids, final_scores, final_masks = \
            model.unmold_detections(detections[i], mrcnn_mask[i],
                                   image.shape, molded_images[i].shape,
                                   windows[i])
        results.append({
            "rois": final_rois,
            "class_ids": final_class_ids,
            "scores": final_scores,
            "masks": final_masks,
        })
    # Detect objects
    r = results[0]
    visualize.display_instances(image, r['rois'], r['masks'], r['class_ids'],
                                FULL_CLASS_NAMES, r['scores'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--image', required=False,
                        metavar="path or URL to image",
                        help='Image to apply the detect')
    parser.add_argument('--export', required=False,
                        metavar="path to export the saved model",
                        help='Path to apply the export')
    parser.add_argument('--logs', required=False,
                        default=DEFAULT_LOGS_DIR,
                        metavar="/path/to/logs/",
                        help='Logs and checkpoints directory (default=logs/)')
    args = parser.parse_args()

    config = RunConfig()
    model = modellib.MaskRCNN(mode="inference", config=config, model_dir=args.logs)

    run(args, model)

