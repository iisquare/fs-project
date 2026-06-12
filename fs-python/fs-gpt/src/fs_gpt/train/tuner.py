import os
import math
import json
from abc import abstractmethod
from typing import Dict, Optional, Union, List, Any

from peft import LoraConfig, get_peft_model
from transformers import AutoModelForCausalLM, AutoTokenizer, Trainer, BitsAndBytesConfig, AutoConfig

from fs_gpt.data.DatasetConfig import DatasetConfig
from fs_gpt.data.JSONLDataset import JSONLDataset
from fs_gpt.data.JSONLStreamingDataset import JSONLStreamingDataset
from fs_gpt.utils import ModelUtil, PlotUtil


class Tuner:
    def __init__(self, args: Dict) -> None:
        self.args = args
        self.model_name_or_path = args.get("model_name_or_path")
        self.output_dir = args.get("output_dir", f"logs/{(os.path.basename(self.model_name_or_path))}")
        self.max_steps = args.get("max_steps", -1)
        self.fine_tuning = args.get("fine_tuning")
        self.quantization_method = args.get("quantization_method")
        self.quantization_bit = args.get("quantization_bit", 4)
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_name_or_path)

        # 自动检测模型配置中的量化方法
        self._detect_quantization_from_config()

    def _detect_quantization_from_config(self) -> None:
        """
        从模型配置文件中自动检测量化方法
        如果配置文件中包含 quantization_config，则自动设置 quantization_method
        """
        try:
            config_path = os.path.join(self.model_name_or_path, "config.json")
            if os.path.exists(config_path):
                with open(config_path, 'r', encoding='utf-8') as f:
                    config = json.load(f)

                # 检查是否存在 quantization_config
                if "quantization_config" in config:
                    quant_config = config["quantization_config"]
                    quant_method = quant_config.get("quant_method") or quant_config.get("method")

                    if quant_method:
                        # 如果用户没有指定 quantization_method，则使用配置文件中的
                        if not self.quantization_method:
                            self.quantization_method = quant_method
                            print(f"Auto-detected quantization method from config: {quant_method}")

                        # 自动设置量化位数
                        if "bits" in quant_config and not self.args.get("quantization_bit"):
                            self.quantization_bit = quant_config["bits"]
                            print(f"Auto-detected quantization bits from config: {self.quantization_bit}")
        except Exception as e:
            print(f"Warning: Failed to detect quantization from config: {e}")


    def train(self) -> None:
        print(f"Load model from {self.model_name_or_path}")
        model = self.model()
        model.config.use_cache = self.args.get("use_cache", True) # 可通过禁用KVCache节省显存
        print(f"Load train dataset with {self.args['train_dataset_names']}")
        if self.max_steps == -1:
            train_dataset = JSONLDataset(self.args["train_dataset_names"], args=self.args, sample=self.sample)
        else: # 流式加载数据，必须指定max_steps最大步长
            train_dataset = JSONLStreamingDataset(self.args["train_dataset_names"], args=self.args, sample=self.sample)
        print(f"Load evaluate dataset with {self.args['eval_dataset_names']}")
        eval_dataset = JSONLDataset(self.args["eval_dataset_names"], args=self.args, sample=self.sample)

        match self.fine_tuning:
            case "freeze": # 暂未支持，仅打印模型结构
                from torchinfo import summary
                print(f"Freeze model parameters")
                print(model)
                summary(model=model)
                for name, param in model.named_parameters():
                    print(f"{name}: requires_grad={param.requires_grad}")
            case "lora":
                target_modules = ModelUtil.find_all_linear_modules(model, self.args.get("freeze_vision_tower", True))
                lora_config = LoraConfig(
                    r=self.args.get("lora_rank", 8),  # 低秩矩阵的秩
                    lora_alpha=self.args.get("lora_alpha", 8),  # 缩放因子
                    target_modules=target_modules,  # 目标模块
                    lora_dropout=self.args.get("lora_dropout", 0.0),
                )
                print(f"Patch lora config: {lora_config}")
                # 应用LoRA到模型
                model = get_peft_model(model, lora_config)
        print(f"Train...")
        trainer = self.trainer(model, train_dataset=train_dataset, eval_dataset=eval_dataset,)
        if trainer.args.do_train:
            result = trainer.train(resume_from_checkpoint=trainer.args.resume_from_checkpoint)
            trainer.log_metrics("train", result.metrics)
            trainer.save_metrics("train", result.metrics)
            trainer.save_state()
            if trainer.is_world_process_zero():
                PlotUtil.plot_loss(trainer.args.output_dir, keys=["loss", "eval_loss"])
        print(f"Save model to {self.output_dir}")
        trainer.save_model()
        print(f"Evaluate...")
        if trainer.args.do_eval:
            metrics = trainer.evaluate(metric_key_prefix="eval")
            if "eval_loss" in metrics:
                try:
                    perplexity = math.exp(metrics["eval_loss"])
                except OverflowError:
                    perplexity = float("inf")
                metrics["perplexity"] = perplexity
                trainer.log_metrics("eval", metrics)
                trainer.save_metrics("eval", metrics)
            else:
                print("Warning: eval_loss not found in metrics. Evaluation may have failed or eval dataset is empty.")
        print(f'Done.')

    def model(self):
        match self.quantization_method:
            case "bitsandbytes":
                match self.quantization_bit:
                    case 4:
                        quantization_config = BitsAndBytesConfig(
                            load_in_4bit=True,
                            bnb_4bit_compute_dtype=self.args.get("bnb_4bit_compute_dtype"),
                            bnb_4bit_use_double_quant=self.args.get("bnb_4bit_use_double_quant", False),
                            bnb_4bit_quant_type=self.args.get("bnb_4bit_quant_type", "fp4"),
                            bnb_4bit_quant_storage=self.args.get("bnb_4bit_quant_storage"),
                        )
                    case 8:
                        quantization_config = BitsAndBytesConfig(
                            load_in_8bit=True,
                        )
                    case _:
                        raise Exception(
                            f"Bitsandbytes only accepts 4-bit or 8-bit quantization, but got {self.quantization_bit}")
                # 加载量化模型
                print(f"Quantization config: {quantization_config}")
                model = AutoModelForCausalLM.from_pretrained(
                    self.model_name_or_path,
                    quantization_config=quantization_config,
                    device_map=self.args.get("device_map", "auto"),
                )
            case "gptq":
                # 使用 GPTQModel 加载 GPTQ 量化模型
                try:
                    from gptqmodel import GPTQModel
                except ImportError:
                    raise ImportError(
                        "gptqmodel is required for loading GPTQ models. "
                        "Please install it with: pip install gptqmodel"
                    )
                print(f"Loading GPTQ model with gptqmodel library (bits={self.quantization_bit})")
                model = GPTQModel.from_quantized(
                    self.model_name_or_path,
                    device_map=self.args.get("device_map", "auto"),
                )
            case "awq":
                # 使用 AutoAWQForCausalLM 加载 AWQ 量化模型
                try:
                    from awq import AutoAWQForCausalLM
                except ImportError:
                    raise ImportError(
                        "autoawq is required for loading AWQ models. "
                        "Please install it with: pip install autoawq"
                    )
                print(f"Loading AWQ model with autoawq library (bits={self.quantization_bit})")
                model = AutoAWQForCausalLM.from_quantized(
                    self.model_name_or_path,
                    device_map=self.args.get("device_map", "auto"),
                )
            case _:
                # AutoModelForCausalLM 通过集成 optimum 库来支持 GPTQ 量化方法
                # 内置的转换逻辑无法完美兼容 Marlin 算子的对齐限制，试图用 Marlin 规则去解析时，断言异常：
                # NotImplementedError: <class 'gptqmodel.nn_modules.qlinear.marlin.MarlinLinear'>: `out_features`: 16 must be divisible by [64].
                # GPTQConfig(
                #     use_marlin=False, # 关键：关闭 Marlin
                #     disable_exllama=False # 确保启用 exllama 作为替代
                # )
                model = AutoModelForCausalLM.from_pretrained(
                    self.model_name_or_path,
                    device_map=self.args.get("device_map", "auto"),
                )
        return model

    @abstractmethod
    def trainer(
            self,
            model,
            train_dataset: Optional[Union[JSONLDataset, JSONLStreamingDataset]] = None,
            eval_dataset: Optional[Union[JSONLDataset, JSONLStreamingDataset]] = None,
    ) -> Trainer:
        pass

    @abstractmethod
    def sample(self, dataset: DatasetConfig, line: str) -> List[Any]:
        pass

def main(args: Dict):
    match args.get("stage"):
        case "pt":
            from fs_gpt.train.pt.PtTuner import PtTuner
            tuner = PtTuner(args)
            pass
        case "sft":
            from fs_gpt.train.sft.SftTuner import SftTuner
            tuner = SftTuner(args)
            pass
        case "rlhf":
            from fs_gpt.train.rlhf.RlhfTuner import RlhfTuner
            tuner = RlhfTuner(args)
        case _:
            raise Exception(f"Unknown stage: {args.get('stage')}")
    tuner.train()
