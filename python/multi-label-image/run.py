import tensorflow as tf
import argparse
import cv2
import numpy as np
import config


def run(args):
    signature_key = tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
    with tf.Session(graph=tf.Graph()) as sess:
        meta_graph_def = tf.saved_model.loader.load(sess, [tf.saved_model.tag_constants.SERVING], args.e)
        signature = meta_graph_def.signature_def

        # 从signature中找出具体输入输出的tensor name
        x_tensor_name = signature[signature_key].inputs['input'].name
        y_tensor_name = signature[signature_key].outputs['output'].name

        # 获取tensor 并inference
        x = sess.graph.get_tensor_by_name(x_tensor_name)
        y = sess.graph.get_tensor_by_name(y_tensor_name)

        # 构建数据
        img = cv2.imread(args.im)
        print('shape:', img.shape)
        img = cv2.resize(img, (config.IMAGE_WIDTH, config.IMAGE_HEIGHT))
        # img = img.transpose((2, 0, 1))
        img = img.astype('float32') / 255
        img = np.expand_dims(img, axis=0)

        # _x 实际输入待inference的data
        pred = sess.run(y, feed_dict={x: img})
        print('pred:', pred)

        threshold = config.CLASS_THRESHOLD_BEST
        y_pred = np.array([1 if pred[0, i] >= threshold[i] else 0 for i in range(pred.shape[1])])
        classes = config.CLASS_NAMES
        result = [classes[i] for i in range(len(classes)) if y_pred[i] == 1]
        print('result:', result)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-e', type=str, required=True, help='export path')
    parser.add_argument('-im', type=str, required=True, help='image file path')
    args = parser.parse_args()

    run(args)

