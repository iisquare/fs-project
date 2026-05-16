from typing import Dict, List
import random

from peft import PeftModel
from transformers import AutoTokenizer

from fs_gpt.train.tuner import Tuner


class Derive:
    def __init__(self, args: Dict) -> None:
        self.args = args
        self.device = args.get("device")
        self.derive_method = args.get("derive_method")
        self.derive_dir = args.get("derive_dir")
        self.derive_size = args.get("derive_size", 4)
        self.model_name_or_path = args.get("model_name_or_path")
        self.quantization_bit = args.get("quantization_bit", 4)
        self.calibration_samples = args.get("calibration_samples", 512)
        self.calibration_max_length = args.get("calibration_max_length", 2048)
        self.calibration_dataset = args.get("calibration_dataset", "mixed")  # mixed, en, zh, local

    def generate(self):
        match self.derive_method:
            case 'lora':
                self.lora()
            case 'gptq':
                self.gptq()
            case 'awq':
                self.awq()
            case _:
                print(f"Unknown derive_method: {self.derive_method}")
        print(f"Done.")

    def _load_calibration_data(self, tokenizer) -> List[str]:
        """加载校准数据集（中英文友好），返回文本列表

        calibration_dataset 可以是:
        - 'mixed': 中英文混合在线数据集 (C4 + mc4)
        - 'en': 英文在线数据集 (wikitext)
        - 'zh': 中文在线数据集 (mc4)
        - 文件路径: 本地 JSONL 文件路径 (如 'data/calibration.jsonl')
        """
        import os
        import json

        # 尝试从在线数据集加载
        if self.calibration_dataset == "mixed":
            # 混合中英文数据集（推荐）
            from datasets import load_dataset

            print(f"Loading mixed EN/ZH calibration dataset...")

            # 加载英文样本 (C4)
            en_samples = []
            try:
                en_dataset = load_dataset("allenai/c4", "en", split="validation", streaming=True)
                for i, sample in enumerate(en_dataset):
                    if i >= self.calibration_samples // 2:
                        break
                    text = sample["text"].strip()
                    if len(text) > 100:  # 过滤太短的文本
                        en_samples.append(text)
                print(f"Loaded {len(en_samples)} English samples")
            except Exception as e:
                print(f"Warning: Failed to load English dataset: {e}")

            # 加载中文样本 (mc4)
            zh_samples = []
            try:
                zh_dataset = load_dataset("mc4", "zh", split="validation", streaming=True)
                for i, sample in enumerate(zh_dataset):
                    if i >= self.calibration_samples // 2:
                        break
                    text = sample["text"].strip()
                    if len(text) > 100:
                        zh_samples.append(text)
                print(f"Loaded {len(zh_samples)} Chinese samples")
            except Exception as e:
                print(f"Warning: Failed to load Chinese dataset: {e}")

            # 混合并打乱
            all_samples = en_samples + zh_samples
            if len(all_samples) < 128:
                raise ValueError(
                    f"Not enough samples loaded from online datasets (got {len(all_samples)}, need at least 128). "
                    f"Please check your network connection or use a local calibration file."
                )

            random.shuffle(all_samples)
            print(f"Successfully loaded {len(all_samples)} mixed samples")
            return all_samples[:self.calibration_samples]

        elif self.calibration_dataset == "en":
            # 仅英文数据集
            from datasets import load_dataset

            print(f"Loading English calibration dataset...")
            dataset = load_dataset("wikitext", "wikitext-2-raw-v1", split="test")
            texts = [text for text in dataset["text"] if text.strip() and len(text.strip()) > 100]

            if len(texts) < 128:
                raise ValueError(
                    f"Not enough samples in English dataset (got {len(texts)}, need at least 128)."
                )

            # 采样
            if len(texts) > self.calibration_samples:
                texts = random.sample(texts, self.calibration_samples)

            print(f"Successfully loaded {len(texts)} English samples")
            return texts[:self.calibration_samples]

        elif self.calibration_dataset == "zh":
            # 仅中文数据集
            from datasets import load_dataset

            print(f"Loading Chinese calibration dataset...")
            zh_samples = []
            zh_dataset = load_dataset("mc4", "zh", split="validation", streaming=True)
            for i, sample in enumerate(zh_dataset):
                if i >= self.calibration_samples:
                    break
                text = sample["text"].strip()
                if len(text) > 100:
                    zh_samples.append(text)

            if len(zh_samples) < 128:
                raise ValueError(
                    f"Not enough samples in Chinese dataset (got {len(zh_samples)}, need at least 128)."
                )

            print(f"Successfully loaded {len(zh_samples)} Chinese samples")
            return zh_samples

        else:
            # 作为文件路径处理
            return self._load_jsonl_calibration_data(self.calibration_dataset)

    def _load_jsonl_calibration_data(self, file_path: str) -> List[str]:
        """从 JSONL 文件加载校准数据，返回文本列表"""
        import json

        print(f"Loading calibration data from: {file_path}")
        texts = []

        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                for line in f:
                    if not line.strip():
                        continue
                    try:
                        item = json.loads(line)
                        text = item.get('text', '')
                        if len(text) > 100:
                            texts.append(text)
                            if len(texts) >= self.calibration_samples:
                                break
                    except json.JSONDecodeError as e:
                        print(f"Warning: Skipping invalid JSON line: {e}")
                        continue

            if len(texts) == 0:
                raise ValueError(
                    f"No valid samples found in {file_path}. "
                    f"Please check the file format (should be JSONL with 'text' field)."
                )

            if len(texts) < 128:
                print(f"Warning: Only {len(texts)} samples found (recommended: 256+)")

            print(f"Successfully loaded {len(texts)} samples from {file_path}")
            return texts

        except FileNotFoundError:
            raise FileNotFoundError(
                f"Calibration file not found: {file_path}\n"
                f"Please ensure the file exists or use an online dataset (mixed/en/zh)."
            )
        except Exception as e:
            raise RuntimeError(f"Failed to load calibration data from {file_path}: {e}")

    def lora(self):
        print(f"Load model from {self.model_name_or_path}")
        tuner = Tuner(self.args)
        model = tuner.model()
        lora_path = self.args.get("lora_path")
        print(f"Loading the LoRA adapter from {lora_path}")
        lora_model = PeftModel.from_pretrained(
            model,
            lora_path,
            torch_device=self.args.get("device"),
        )
        print(f"Applying the LoRA")
        model = lora_model.merge_and_unload()
        print(f"Saving the target model to {self.derive_dir}")
        model.save_pretrained(self.derive_dir, max_shard_size=f"{self.derive_size}GB",)
        tuner.tokenizer.save_pretrained(self.derive_dir)

    def gptq(self):
        from gptqmodel import GPTQModel, QuantizeConfig

        tokenizer = AutoTokenizer.from_pretrained(self.model_name_or_path, use_fast=True)

        # 加载校准数据（文本列表）
        calibration_texts = self._load_calibration_data(tokenizer)

        # GPTQ 需要 token ids
        examples = [
            tokenizer(text, truncation=True, max_length=self.calibration_max_length).input_ids
            for text in calibration_texts
        ]

        quantize_config = QuantizeConfig(
            bits=self.quantization_bit,  # quantize model to 4-bit
            group_size=128,  # it is recommended to set the value to 128
            device=self.device,
        )

        print(f"Load model from {self.model_name_or_path}")
        model = GPTQModel.load(self.model_name_or_path, quantize_config=quantize_config,)
        print(f"Quantize with {len(examples)} examples")
        model.quantize(examples)
        print(f"Save model to {self.derive_dir}")
        model.save(self.derive_dir, max_shard_size=f"{self.derive_size}GB",)
        tokenizer.save_pretrained(self.derive_dir)

    def awq(self):
        from awq import AutoAWQForCausalLM

        tokenizer = AutoTokenizer.from_pretrained(self.model_name_or_path, trust_remote_code=True)

        # 加载校准数据（文本列表）
        calibration_texts = self._load_calibration_data(tokenizer)

        # AWQ 直接使用文本格式的数据
        quant_config = {
            "zero_point": True,
            "q_group_size": 128,
            "w_bit": self.quantization_bit,
            "version": "GEMM"  # 使用 GEMM 内核以获得更好的性能
        }

        print(f"Load model from {self.model_name_or_path}")
        model = AutoAWQForCausalLM.from_pretrained(
            self.model_name_or_path,
            device_map=self.device,
            trust_remote_code=True
        )

        print(f"Quantize with {len(calibration_texts)} examples")
        model.quantize(tokenizer, quant_config=quant_config, calib_data=calibration_texts)

        print(f"Save model to {self.derive_dir}")
        model.save_quantized(self.derive_dir, shard_size=f"{self.derive_size}GB",)
        tokenizer.save_pretrained(self.derive_dir)

def main(args: Dict):
    Derive(args).generate()
