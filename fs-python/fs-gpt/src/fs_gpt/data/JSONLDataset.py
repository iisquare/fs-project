import re
from typing import Dict, List, Union, Optional, Callable

import datasets
from torch.utils.data.dataset import Dataset

from fs_gpt.data.DatasetConfig import DatasetConfig


class JSONLDataset(Dataset):
    def __init__(self, dataset_names: Union[str, List[str]], args: Dict, sample: Optional[Callable] = None):
        self.data = []
        self.args = args
        self.sample = sample if sample else (lambda d, l: [d.json(l)])
        self.dataset_names = dataset_names
        if isinstance(dataset_names, str):
            self.dataset_names = [name.strip() for name in re.split(r'[,;\s]+', dataset_names) if name.strip()]
        self._load_data()

    def _load_data(self):
        for name in self.dataset_names:
            dataset = DatasetConfig(name, self.args)
            with open(dataset.path(), 'r', encoding='utf-8') as f:
                for line in f:
                    samples = self.sample(dataset, line.strip())
                    self.data.extend(samples)

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        sample = self.data[idx]
        return sample

    def datasets(self) -> datasets.Dataset:
        return datasets.Dataset.from_list(self.data)
