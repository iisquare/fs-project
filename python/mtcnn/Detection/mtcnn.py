import os
import sys

import tensorflow as tf
from tensorflow import saved_model as sm
from tensorflow.python.framework import graph_util

sys.path.append("../")
from train_models.mtcnn_model import P_Net, R_Net, O_Net


class Net(object):

    def restore(self, graph, model_path, output_node_names):
        ckpt_state = tf.train.get_checkpoint_state(model_path)
        model_path = os.path.join(model_path, os.path.basename(ckpt_state.model_checkpoint_path))
        print('restore from {}'.format(model_path))
        output_node_names = [item.name.split(':')[0] for item in output_node_names]
        with tf.Session(config=tf.ConfigProto(allow_soft_placement=True), graph=graph) as sess:
            saver = tf.train.Saver()
            saver.restore(sess, model_path)
            return graph_util.convert_variables_to_constants(
                sess=sess,
                input_graph_def=sess.graph_def,
                output_node_names=output_node_names)


class PNet(Net):

    def __init__(self, model_path):
        self.batch_size = 1
        with tf.Graph().as_default() as graph:
            self.image_op = tf.placeholder(tf.float32, name='input_image')
            self.width_op = tf.placeholder(tf.int32, name='image_width')
            self.height_op = tf.placeholder(tf.int32, name='image_height')
            image_reshape = tf.reshape(self.image_op, [self.batch_size, self.height_op, self.width_op, 3])
            self.cls_prob, self.bbox_pred, self.landmark_pred = P_Net(image_reshape, training=False)
            output_node_names = [self.image_op, self.width_op, self.height_op,
                                 self.cls_prob, self.bbox_pred, self.landmark_pred]
            self.graph = self.restore(graph, model_path, output_node_names)


class RNet(Net):

    def __init__(self, model_path):
        self.batch_size = None
        self.data_size = 24
        with tf.Graph().as_default() as graph:
            self.image_op = tf.placeholder(tf.float32, shape=[self.batch_size, self.data_size, self.data_size, 3],
                                           name='input_image')
            self.cls_prob, self.bbox_pred, self.landmark_pred = R_Net(self.image_op, training=False)
            output_node_names = [self.image_op, self.cls_prob, self.bbox_pred, self.landmark_pred]
            self.graph = self.restore(graph, model_path, output_node_names)


class ONet(Net):

    def __init__(self, model_path):
        self.batch_size = None
        self.data_size = 48
        with tf.Graph().as_default() as graph:
            self.image_op = tf.placeholder(tf.float32, shape=[self.batch_size, self.data_size, self.data_size, 3],
                                           name='input_image')
            self.cls_prob, self.bbox_pred, self.landmark_pred = O_Net(self.image_op, training=False)
            output_node_names = [self.image_op, self.cls_prob, self.bbox_pred, self.landmark_pred]
            self.graph = self.restore(graph, model_path, output_node_names)


class MTCNN(object):

    def __init__(self, p_net_path, r_net_path, o_net_path):
        self.p_net = PNet(p_net_path)
        self.r_net = RNet(r_net_path)
        self.o_net = ONet(o_net_path)

    def export(self, export_dir):
        p_net_input_image = tf.placeholder(tf.float32, name='p_input_image')
        p_net_input_shape = tf.shape(p_net_input_image)
        p_net_cls_prob, p_net_bbox_pred, p_net_landmark_pred = tf.import_graph_def(self.p_net.graph, input_map={
            self.p_net.image_op.name: p_net_input_image,
            self.p_net.height_op.name: p_net_input_shape[0],
            self.p_net.width_op.name: p_net_input_shape[1]
        }, return_elements=[self.p_net.cls_prob.name, self.p_net.bbox_pred.name, self.p_net.landmark_pred.name])
        r_net_input_image = tf.placeholder(tf.float32,
                                           shape=[self.r_net.batch_size, self.r_net.data_size, self.r_net.data_size, 3],
                                           name='r_input_image')
        r_net_cls_prob, r_net_bbox_pred, r_net_landmark_pred = tf.import_graph_def(self.r_net.graph, input_map={
            self.r_net.image_op.name: r_net_input_image
        }, return_elements=[self.r_net.cls_prob.name, self.r_net.bbox_pred.name, self.r_net.landmark_pred.name])
        o_net_input_image = tf.placeholder(tf.float32,
                                           shape=[self.o_net.batch_size, self.o_net.data_size, self.o_net.data_size, 3],
                                           name='o_input_image')
        o_net_cls_prob, o_net_bbox_pred, o_net_landmark_pred = tf.import_graph_def(self.o_net.graph, input_map={
            self.o_net.image_op.name: o_net_input_image
        }, return_elements=[self.o_net.cls_prob.name, self.o_net.bbox_pred.name, self.o_net.landmark_pred.name])
        with tf.Session(config=tf.ConfigProto(allow_soft_placement=True)) as sess:
            saved_builder = sm.builder.SavedModelBuilder(export_dir)
            signatur_def = sm.signature_def_utils.build_signature_def(
                inputs={
                    'p_net_input_image':  sm.utils.build_tensor_info(p_net_input_image),
                    'r_net_input_image': sm.utils.build_tensor_info(r_net_input_image),
                    'o_net_input_image': sm.utils.build_tensor_info(o_net_input_image),
                },
                outputs={
                    'p_net_cls_prob': sm.utils.build_tensor_info(p_net_cls_prob),
                    'p_net_bbox_pred': sm.utils.build_tensor_info(p_net_bbox_pred),
                    'p_net_landmark_pred': sm.utils.build_tensor_info(p_net_landmark_pred),
                    'r_net_cls_prob': sm.utils.build_tensor_info(r_net_cls_prob),
                    'r_net_bbox_pred': sm.utils.build_tensor_info(r_net_bbox_pred),
                    'r_net_landmark_pred': sm.utils.build_tensor_info(r_net_landmark_pred),
                    'o_net_cls_prob': sm.utils.build_tensor_info(o_net_cls_prob),
                    'o_net_bbox_pred': sm.utils.build_tensor_info(o_net_bbox_pred),
                    'o_net_landmark_pred': sm.utils.build_tensor_info(o_net_landmark_pred),
                },
                method_name=sm.signature_constants.PREDICT_METHOD_NAME,
            )
            saved_builder.add_meta_graph_and_variables(
                sess,
                tags=[sm.tag_constants.SERVING],
                signature_def_map={sm.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY: signatur_def},
            )
            print('export model to {}'.format(export_dir))
            saved_builder.save()

    def detect(self, image):
        input_image = tf.placeholder(tf.float32, name='input_image')
        input_shape = tf.shape(input_image)
        input_shape = tf.Print(input_shape, ['input_shape', input_shape])
        cls_prob, bbox_pred = tf.import_graph_def(self.p_net.graph, input_map={
            self.p_net.image_op.name: input_image,
            self.p_net.height_op.name: input_shape[0],
            self.p_net.width_op.name: input_shape[1]
        }, return_elements=[self.p_net.cls_prob.name, self.p_net.bbox_pred.name])
        cls_prob = tf.Print(cls_prob, ['cls_prob_shape', tf.shape(cls_prob)])
        bbox_pred = tf.Print(bbox_pred, ['bbox_pred_shape', tf.shape(bbox_pred)])
        with tf.Session(config=tf.ConfigProto(allow_soft_placement=True)) as session:
            return session.run([cls_prob, bbox_pred], feed_dict={input_image: image})

