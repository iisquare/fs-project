import os
import config
import argparse
import numpy as np
from dataset import CsvDataset
from models import net
from keras import optimizers
from keras.callbacks import ModelCheckpoint
from sklearn.metrics import matthews_corrcoef
from keras.callbacks import TensorBoard


def test(model):
    test_dataset = CsvDataset(config.CSV_FILE, config.ROOT_DIR, 'val', config.CLASS_NAMES)
    x_test, y_test = test_dataset.format(config.IMAGE_WIDTH, config.IMAGE_HEIGHT)

    out = model.predict_proba(x_test)
    out = np.array(out)
    threshold = np.array(config.CLASS_THRESHOLD)

    acc = []
    accuracies = []
    best_threshold = np.zeros(out.shape[1])
    for i in range(out.shape[1]):
        y_prob = np.array(out[:, i])
        for j in threshold:
            y_pred = [1 if prob >= j else 0 for prob in y_prob]
            acc.append(matthews_corrcoef(y_test[:, i], y_pred))
        acc = np.array(acc)
        index = np.where(acc == acc.max())
        accuracies.append(acc.max())
        best_threshold[i] = threshold[index[0][0]]
        acc = []

    print
    "best thresholds", best_threshold
    y_pred = np.array(
        [[1 if out[i, j] >= best_threshold[j] else 0 for j in range(y_test.shape[1])] for i in range(len(y_test))])

    print("-" * 40)
    print("Matthews Correlation Coefficient")
    print("Class wise accuracies")
    print(accuracies)

    print("other statistics\n")
    total_correctly_predicted = len([i for i in range(len(y_test)) if (y_test[i] == y_pred[i]).sum() == 5])
    print("Fully correct output")
    print(total_correctly_predicted)
    print(total_correctly_predicted / 400.)


def train(args):
    train_dataset = CsvDataset(config.CSV_FILE, config.ROOT_DIR, 'train', config.CLASS_NAMES)
    x_train, y_train = train_dataset.format(config.IMAGE_WIDTH, config.IMAGE_HEIGHT)
    test_dataset = CsvDataset(config.CSV_FILE, config.ROOT_DIR, 'val', config.CLASS_NAMES)
    x_test, y_test = test_dataset.format(config.IMAGE_WIDTH, config.IMAGE_HEIGHT)

    model = net.dense(config.IMAGE_WIDTH, config.IMAGE_HEIGHT, len(config.CLASS_NAMES))
    # let's train the model using SGD + momentum (how original).
    # sgd = optimizers.SGD(lr=args.lr, decay=1e-6, momentum=0.9, nesterov=True)
    adam = optimizers.Adam(lr=args.lr, beta_1=0.9, beta_2=0.999, epsilon=None, decay=0.0, amsgrad=False)
    model.compile(loss='binary_crossentropy', optimizer=adam, metrics=['accuracy'])

    if args.w:
        model.load_weights(args.w)

    model.summary()
    if not os.path.exists(args.d):
        os.mkdir(args.d)
    check = ModelCheckpoint(os.path.join(args.d, "{epoch:02d}-{val_acc:.6f}.hdf5"), monitor='val_acc', verbose=1,
                            save_best_only=False, save_weights_only=True, mode='auto')
    board = TensorBoard(log_dir=args.ld,  # 日志目录
                        histogram_freq=args.e,  # 按照何等频率（epoch）来计算直方图，0为不计算
                        batch_size=args.b,  # 用多大量的数据计算直方图
                        write_graph=True,  # 是否存储网络结构图
                        write_grads=True,  # 是否可视化梯度直方图
                        write_images=True,  # 是否可视化参数
                        embeddings_freq=0,
                        embeddings_layer_names=None,
                        embeddings_metadata=None)
    if args.e > 0:
        model.fit(x_train, y_train, batch_size=args.b,
                  nb_epoch=args.e, callbacks=[check, board], validation_data=(x_test, y_test))
    return model


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-b', type=int, default=2, help='batch size for train')
    parser.add_argument('-e', type=int, default=100, help='number epoch for train')
    parser.add_argument('-w', type=str, default=None, help='initial weights file')
    parser.add_argument('-lr', type=float, default=0.001, help='initial learning rate')
    parser.add_argument('-d', type=str, default='weights', help='path to save weights')
    parser.add_argument('-ld', type=str, default='logs', help='path to save logs')
    args = parser.parse_args()

    model = train(args)
    test(model)
