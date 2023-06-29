from keras import backend as ks
import tensorflow as tf


def predict(export_path, inputs):
    signature_key = tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
    with tf.Session(graph=tf.Graph()) as sess:
        meta_graph_def = tf.saved_model.loader.load(sess, [tf.saved_model.tag_constants.SERVING], export_path)
        signature = meta_graph_def.signature_def
        signature_inputs = {
            'input_image': sess.graph.get_tensor_by_name(signature[signature_key].inputs['input_image'].name),
            'input_image_meta': sess.graph.get_tensor_by_name(signature[signature_key].inputs['input_image_meta'].name),
            'input_anchors': sess.graph.get_tensor_by_name(signature[signature_key].inputs['input_anchors'].name),
        }

        signature_outputs = {
            'mrcnn_detection': sess.graph.get_tensor_by_name(signature[signature_key].outputs['mrcnn_detection'].name),
            'mrcnn_class': sess.graph.get_tensor_by_name(signature[signature_key].outputs['mrcnn_class'].name),
            'mrcnn_bbox': sess.graph.get_tensor_by_name(signature[signature_key].outputs['mrcnn_bbox'].name),
            'mrcnn_mask': sess.graph.get_tensor_by_name(signature[signature_key].outputs['mrcnn_mask'].name),
            'ROI': sess.graph.get_tensor_by_name(signature[signature_key].outputs['ROI'].name),
            'rpn_class': sess.graph.get_tensor_by_name(signature[signature_key].outputs['rpn_class'].name),
            'rpn_bbox': sess.graph.get_tensor_by_name(signature[signature_key].outputs['rpn_bbox'].name),
        }

        return sess.run(list(signature_outputs.values()), feed_dict={
            signature_inputs['input_image']: inputs['input_image'],
            signature_inputs['input_image_meta']: inputs['input_image_meta'],
            signature_inputs['input_anchors']: inputs['input_anchors'],
        })


def export(model, export_path):
    with ks.get_session() as sess:
        builder = tf.saved_model.builder.SavedModelBuilder(export_path)

        signature_inputs = {
            'input_image': tf.saved_model.utils.build_tensor_info(model.keras_model.input[0]),
            'input_image_meta': tf.saved_model.utils.build_tensor_info(model.keras_model.input[1]),
            'input_anchors': tf.saved_model.utils.build_tensor_info(model.keras_model.input[2]),
        }

        signature_outputs = {
            'mrcnn_detection': tf.saved_model.utils.build_tensor_info(model.keras_model.output[0]),
            'mrcnn_class': tf.saved_model.utils.build_tensor_info(model.keras_model.output[1]),
            'mrcnn_bbox': tf.saved_model.utils.build_tensor_info(model.keras_model.output[2]),
            'mrcnn_mask': tf.saved_model.utils.build_tensor_info(model.keras_model.output[3]),
            'ROI': tf.saved_model.utils.build_tensor_info(model.keras_model.output[4]),
            'rpn_class': tf.saved_model.utils.build_tensor_info(model.keras_model.output[5]),
            'rpn_bbox': tf.saved_model.utils.build_tensor_info(model.keras_model.output[6]),
        }

        classification_signature_def = tf.saved_model.signature_def_utils.build_signature_def(
            inputs=signature_inputs,
            outputs=signature_outputs,
            method_name=tf.saved_model.signature_constants.PREDICT_METHOD_NAME)

        builder.add_meta_graph_and_variables(
            sess,
            [tf.saved_model.tag_constants.SERVING],
            signature_def_map={
                tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY: classification_signature_def
            },
        )

        builder.save()
