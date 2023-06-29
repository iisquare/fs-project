import unittest
import numpy as np


class TestDemo(unittest.TestCase):

    def test_mask(self):
        a = np.ones((6,)) * 0.5
        print(a)


if __name__ == '__main__':
    unittest.main()
