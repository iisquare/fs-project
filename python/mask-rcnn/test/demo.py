import unittest
import cv2
import numpy as np


class TestDemo(unittest.TestCase):

    def test_dict(self):
        d = {'a': 1, 'b': 3, 'c': 7}
        print(list(d.values()))

    def test_mask(self):
        mask_path = 'C:\\Users\\Ouyang\\Desktop\\text\\mask\\train\\58_6\\58.1.png'
        image = cv2.cvtColor(cv2.imread(mask_path), cv2.COLOR_BGRA2GRAY)
        print(image)
        print(np.where(image > 200))


if __name__ == '__main__':
    unittest.main()
