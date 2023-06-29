import tensorflow as tf
from tensorflow.python.framework import graph_util

tf.app.flags.DEFINE_string('gpu_list', '0', '')
tf.app.flags.DEFINE_string('checkpoint_path', './east_icdar2015_resnet_v1_50_rbox/', '')
tf.app.flags.DEFINE_string('output_path', './east_icdar2015_resnet_v1_50_rbox/east.pb', '')
tf.app.flags.DEFINE_bool('print_graph', False, '')
FLAGS = tf.app.flags.FLAGS


def main(argv=None):
    import os
    os.environ['CUDA_VISIBLE_DEVICES'] = FLAGS.gpu_list
    output_node_names = 'feature_fusion/Conv_7/Sigmoid,feature_fusion/concat_3'

    ckpt_state = tf.train.get_checkpoint_state(FLAGS.checkpoint_path)
    model_path = os.path.join(FLAGS.checkpoint_path, os.path.basename(ckpt_state.model_checkpoint_path))
    print('Restore from {}'.format(model_path))

    saver = tf.train.import_meta_graph(model_path + '.meta', clear_devices=True)

    with tf.Session(config=tf.ConfigProto(allow_soft_placement=True)) as sess:

        saver.restore(sess, model_path)

        if FLAGS.print_graph:
            print('***variable_name')
            for variable_name in tf.global_variables():
                print(variable_name)
            print('***tensor_name')
            for tensor_name in tf.contrib.graph_editor.get_tensors(tf.get_default_graph()):
                print(tensor_name)
            print('***node')
            for node in sess.graph_def.node:
                print(node)

        output_graph_def = graph_util.convert_variables_to_constants(
            sess=sess,
            input_graph_def=sess.graph_def,
            output_node_names=output_node_names.split(','))

        with tf.gfile.GFile(FLAGS.output_path, "wb") as f:  # 保存模型
            f.write(output_graph_def.SerializeToString())  # 序列化输出
        print("%d ops in the final graph." % len(output_graph_def.node))  # 得到当前图有几个操作节点


if __name__ == '__main__':
    tf.app.run()
