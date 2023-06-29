import os
import numpy as np

CLASS_NAMES = ['ke_wm', 'lianjia_wm', '5i5j_wm', '58_br', '58_wm', 'anjuke_br', 'anjuke_wm', 'centanet_ct', 'fang_br']
IMAGE_WIDTH = 100
IMAGE_HEIGHT = 100
ROOT_DIR = 'C:\\Users\\Ouyang\\Desktop\\dataset\\ml-classify'
CSV_FILE = os.path.join(ROOT_DIR, 'full.csv')

CLASS_THRESHOLD = [0.1, 0.9, 0.1]
CLASS_THRESHOLD_BEST = np.ones((len(CLASS_NAMES),)) * 0.6
