from typing import Optional, Union, List, Any

from transformers import Trainer
from trl import DPOConfig, DPOTrainer

from fs_gpt.data.DatasetConfig import DatasetConfig
from fs_gpt.data.JSONLDataset import JSONLDataset
from fs_gpt.data.JSONLStreamingDataset import JSONLStreamingDataset
from fs_gpt.train.tuner import Tuner


class RlhfTuner(Tuner):
    def sample(self, dataset: DatasetConfig, line: str) -> List[Any]:
        sample = dataset.rlhf(line)
        return [sample] if sample else []

    def trainer(
            self,
            model,
            train_dataset: Optional[Union[JSONLDataset, JSONLStreamingDataset]] = None,
            eval_dataset: Optional[Union[JSONLDataset, JSONLStreamingDataset]] = None,
    ) -> Trainer:
        training_args = DPOConfig(
            output_dir=self.output_dir,
            logging_dir=self.output_dir,
            do_train=self.args.get("do_train", True),
            do_eval=self.args.get("do_eval", True),
            resume_from_checkpoint=self.args.get("resume_from_checkpoint"),
            overwrite_output_dir=self.args.get("overwrite_output_dir", False),
            num_train_epochs=self.args.get("num_train_epochs", 3.0),
            max_steps=self.max_steps,
            per_device_train_batch_size=self.args.get("per_device_train_batch_size", 1),
            learning_rate=self.args.get("learning_rate", 1e-5),
            lr_scheduler_type=self.args.get("lr_scheduler_type", "cosine"),
            warmup_ratio=self.args.get("warmup_ratio", 0.1),
            bf16=self.args.get("bf16", True),
            fp16=self.args.get("fp16", False),
            ddp_timeout=self.args.get("ddp_timeout", 1800),
            save_steps=self.args.get("save_steps", 500),
            logging_steps=self.args.get("logging_steps", 10),
            per_device_eval_batch_size=self.args.get("per_device_eval_batch_size", 1),
            eval_strategy=self.args.get("eval_strategy", "no"),
            eval_steps=self.args.get("eval_steps", None),
            deepspeed=self.args.get("deepspeed"),
        )
        return DPOTrainer(
            model=model,
            args=training_args,
            train_dataset=train_dataset.datasets(),
            eval_dataset=eval_dataset.datasets(),
            processing_class=self.tokenizer
        )
