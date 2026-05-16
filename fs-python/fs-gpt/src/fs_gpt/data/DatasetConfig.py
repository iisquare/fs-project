import json
from pathlib import Path
from typing import Dict, Any, List

import yaml

"""
配置格式：
[name]:
    filepath: /path/to/dataset
    text_field: text
    input_field: input
    output_field: output
    prompt_field: prompt
    chosen_field: chosen
    rejected_field: rejected
"""
class DatasetConfig:
    def __init__(self, name: str, args: Dict):
        self.name = name
        self.args = args
        self.dataset = yaml.safe_load(Path(args.get("dataset_config", "config/dataset.yaml")).read_text())
        self.block_size = args.get("dataset_block_size", 1024)
        self.overlap = args.get("dataset_overlap", 0)
        assert self.overlap < self.block_size, "Overlap must be less than block size"
        self.text_field = self.dataset[name].get("text_field", "text")
        self.input_field = self.dataset[name].get("input_field", "input")
        self.output_field = self.dataset[name].get("output_field", "output")
        self.prompt_field = self.dataset[name].get("prompt_field", "prompt")
        self.chosen_field = self.dataset[name].get("chosen_field", "chosen")
        self.rejected_field = self.dataset[name].get("rejected_field", "rejected")

    def path(self) -> str:
        return str(Path(self.dataset[self.name]["filepath"]).absolute())

    def json(self, line: str) -> Any | None:
        return json.loads(line)

    def pt(self, line: str) -> Any | None:
        data = self.json(line)
        if not data:
            return None
        return data.get(self.text_field)

    def sft(self, line: str) -> Any | None:
        data = self.json(line)
        if not data:
            return None
        return {
            "input": data.get(self.input_field),
            "output": data.get(self.output_field),
        }

    def rlhf(self, line: str) -> Any | None:
        data = self.json(line)
        if not data:
            return None
        return {
            "prompt": data.get(self.prompt_field),
            "chosen": data.get(self.chosen_field),
            "rejected": data.get(self.rejected_field),
        }

    def split(self, sample: str) -> List[str]:
        result = []
        start = 0
        end = self.block_size
        length = len(sample)
        while start < length:
            chunk = sample[start:end]
            result.append(chunk)
            start += self.block_size - self.overlap
            end = start + self.block_size
        return result
