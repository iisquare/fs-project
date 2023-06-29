import cv2
import argparse
import config
import math
import numpy as np
from models import net


def test(args):
    image = cv2.imread(args.im)
    if args.cut == 'tl':
        image = image[0:300, 0:300, :]
    elif args.cut == 'br':
        image = image[-300:, -300:, :]
    elif args.cut == 'ct':
        x, y = math.ceil(image.shape[1] / 2), math.ceil(image.shape[0] / 2)
        image = image[y - 150:y + 150, x - 150:x + 150, :]
    print('shape:', image.shape)
    image = cv2.resize(image, (config.IMAGE_WIDTH, config.IMAGE_HEIGHT))
    # img = img.transpose((2, 0, 1))
    image = image.astype('float32') / 255
    image = np.expand_dims(image, axis=0)

    model = net.dense(config.IMAGE_WIDTH, config.IMAGE_HEIGHT, len(config.CLASS_NAMES))
    model.load_weights(args.w)
    pred = model.predict(image)
    print('pred:', pred)

    threshold = config.CLASS_THRESHOLD_BEST
    y_pred = np.array([1 if pred[0, i] >= threshold[i] else 0 for i in range(pred.shape[1])])
    classes = config.CLASS_NAMES
    result = [classes[i] for i in range(len(classes)) if y_pred[i] == 1]
    print('result:', result)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-im', type=str, required=True, help='image file path')
    parser.add_argument('-w', type=str, required=True, help='initial weights file')
    parser.add_argument('-cut', type=str, required=True, help='cut image with [tl,ct,br]')
    args = parser.parse_args()

    test(args)
