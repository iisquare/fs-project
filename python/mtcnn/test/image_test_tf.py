#coding:utf-8
import sys
sys.path.append('..')
import cv2
from Detection.mtcnn import MTCNN


model_paths = [
    '../data/MTCNN_model/PNet_landmark',
    '../data/MTCNN_model/RNet_landmark',
    '../data/MTCNN_model/ONet_landmark']

image = cv2.imread('../data/image/lala/img_414.jpg')
mtcnn = MTCNN(model_paths[0], model_paths[1], model_paths[2])
mtcnn.export('../data/model/mtcnn')
# print(mtcnn.detect(image))
