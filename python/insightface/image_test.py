import argparse

import cv2
import numpy as np
import tensorflow as tf
import tensorlayer as tl
from tensorflow import saved_model as sm

from losses.face_losses import arcface_loss
from nets.L_Resnet_E_IR import get_resnet


def get_args():
    parser = argparse.ArgumentParser(description='input information')
    parser.add_argument('--ckpt_file', default=r'E:\workspace\dataset\model\ckpt_model_c\InsightFace_iter_best_',
                        type=str, help='the ckpt file path')
    parser.add_argument('--image', default=None, help='the image path')
    parser.add_argument('--image_size', default=[112, 112], help='the image size')
    parser.add_argument('--net_depth', default=50, help='resnet depth, default is 50')
    parser.add_argument('--num_output', default=85164, help='the image size')
    parser.add_argument('--ckpt_index', default='1950000.ckpt', help='ckpt file index')
    parser.add_argument('--export', default=None, help='the export path')
    args = parser.parse_args()
    return args


if __name__ == '__main__':
    args = get_args()

    images = tf.placeholder(name='img_inputs', shape=[None, *args.image_size, 3], dtype=tf.float32)
    labels = tf.placeholder(name='img_labels', shape=[None, ], dtype=tf.int64)
    dropout_rate = tf.constant(1.0, name='dropout_rate', dtype=tf.float32)

    w_init_method = tf.contrib.layers.xavier_initializer(uniform=False)
    net = get_resnet(images, args.net_depth, type='ir', w_init=w_init_method, trainable=False, keep_rate=dropout_rate)
    embedding_tensor = net.outputs
    logit = arcface_loss(embedding=net.outputs, labels=labels, w_init=w_init_method, out_num=args.num_output)

    sess = tf.Session(config=tf.ConfigProto(allow_soft_placement=True))
    saver = tf.train.Saver()

    path = args.ckpt_file + args.ckpt_index
    saver.restore(sess, path)
    print('ckpt file %s restored!' % path)

    if args.export is not None:
        saved_builder = sm.builder.SavedModelBuilder(args.export)
        signatur_def = sm.signature_def_utils.build_signature_def(
            inputs={
                'images': sm.utils.build_tensor_info(images)
            },
            outputs={'embedding': sm.utils.build_tensor_info(embedding_tensor)},
            method_name=sm.signature_constants.PREDICT_METHOD_NAME,
        )
        saved_builder.add_meta_graph_and_variables(
            sess,
            tags=[sm.tag_constants.SERVING],
            signature_def_map={sm.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY: signatur_def},
        )
        print('export model to {}'.format(args.export))
        saved_builder.save()

    if args.image is not None:
        image = cv2.imread(args.image)
        image = (image - 127.5) * 0.0078125
        feed_dict = {}
        feed_dict.update(tl.utils.dict_to_one(net.all_drop))
        feed_dict[images] = np.expand_dims(image, axis=0)

        result = sess.run(embedding_tensor, feed_dict)
        print(result)
