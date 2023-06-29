from keras import backend as ks
import tensorflow as tf
import argparse
import config
from models import net


def export(args):
    model = net.dense(config.IMAGE_WIDTH, config.IMAGE_HEIGHT, len(config.CLASS_NAMES))
    model.load_weights(args.w)
    with ks.get_session() as sess:
        builder = tf.saved_model.builder.SavedModelBuilder(args.e)

        signature_inputs = {
            'input': tf.saved_model.utils.build_tensor_info(model.inputs[0])
        }

        signature_outputs = {
            'output': tf.saved_model.utils.build_tensor_info(model.outputs[0])
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


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-e', type=str, required=True, help='export path')
    parser.add_argument('-w', type=str, required=True, help='initial weights file')
    args = parser.parse_args()

    export(args)

