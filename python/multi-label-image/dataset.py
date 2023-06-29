import os
import pandas as pd
import numpy as np
import cv2
import matplotlib.pyplot as plt


class CsvDataset:
    def __init__(self, csv_file, root_dir, dataset, names):
        self.labels = names
        self.frames = pd.read_csv(csv_file)
        # self.labels = np.array(self.frames.columns[2:])
        self.frames = self.frames[self.frames['dataset'] == dataset].loc[:, ['path'] + names]
        self.root_dir = root_dir

    def __len__(self):
        return len(self.frames)

    def __getitem__(self, idx):
        img_name = self.frames.iloc[idx, 0]
        img_path = os.path.join(self.root_dir, img_name)
        image = cv2.imread(img_path)
        if image is None:
            raise RuntimeWarning('read image {} failed from {}'.format(img_name, img_path))
        label = self.frames.iloc[idx, 1:]
        label = np.array(label)
        label = label.astype(np.uint8)
        sample = {'name': img_name, 'image': image, 'label': label}
        return sample

    def show(self, idx):
        sample = self.__getitem__(idx)
        plt.figure()
        plt.imshow(sample['image'])
        print(','.join(self.labels[sample['label'] == 1]))
        plt.show()

    def format(self, width, height):
        x = []
        y = []
        for i in range(self.__len__()):
            sample = self.__getitem__(i)
            image = cv2.resize(sample['image'], (width, height))
            # img = img.transpose((2, 0, 1))
            x.append(image / 255)
            y.append(sample['label'])
        return np.array(x).astype(np.float32), np.array(y).astype(np.float32)
