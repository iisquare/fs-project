import tensorflow as tf
from tensorflow.python.framework import graph_util
import model_cv as model


tf.app.flags.DEFINE_string('gpu_list', '0', '')
tf.app.flags.DEFINE_string('checkpoint_path', './east_icdar2015_resnet_v1_50_rbox/', '')
tf.app.flags.DEFINE_string('output_path', './east_icdar2015_resnet_v1_50_rbox/east.pb', '')
tf.app.flags.DEFINE_bool('print_graph', False, '')
FLAGS = tf.app.flags.FLAGS


def main(argv=None):
    import os
    os.environ['CUDA_VISIBLE_DEVICES'] = FLAGS.gpu_list

    with tf.get_default_graph().as_default():
        input_images = tf.placeholder(tf.float32, shape=[None, None, None, 3], name='input_images')
        global_step = tf.get_variable('global_step', [], initializer=tf.constant_initializer(0), trainable=False)

        f_score, f_geometry = model.model(input_images, is_training=False)

        variable_averages = tf.train.ExponentialMovingAverage(0.997, global_step)
        saver = tf.train.Saver(variable_averages.variables_to_restore())

        with tf.Session(config=tf.ConfigProto(allow_soft_placement=True)) as sess:
            ckpt_state = tf.train.get_checkpoint_state(FLAGS.checkpoint_path)
            model_path = os.path.join(FLAGS.checkpoint_path, os.path.basename(ckpt_state.model_checkpoint_path))
            print('Restore from {}'.format(model_path))
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

            print('score name {}, geometry name {}'.format(f_score.op.name, f_geometry.op.name))
            output_graph_def = graph_util.convert_variables_to_constants(
                sess=sess,
                input_graph_def=sess.graph_def,
                output_node_names=[f_score.op.name, f_geometry.op.name])

            with tf.gfile.GFile(FLAGS.output_path, "wb") as f:  # 保存模型
                f.write(output_graph_def.SerializeToString())  # 序列化输出
            print("%d ops in the final graph." % len(output_graph_def.node))  # 得到当前图有几个操作节点


if __name__ == '__main__':
    tf.app.run()
