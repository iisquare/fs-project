# fs-python

模型侧工程，按功能拆分为独立子项目：

| 子项目 | 说明 |
| --- | --- |
| `fs-gpt` | 大模型训练与推理：预训练、LoRA / QLoRA 微调、模型合并与量化（GPTQ / AWQ）、推理接口（Bitsandbytes / vLLM / SGLang / LoRA 切换）、模型评估；附 vLLM、SGLang 的容器化服务配置。 |
| `fs-core` | 通用能力模块，目前仅预留。 |

使用 conda 创建 Python 3.10 环境，具体安装、训练与推理命令见 [fs-gpt 说明](fs-gpt/README.md)。

早期基于 Pytorch / TensorFlow 的模型实验代码（CRNN、EAST、InsightFace、LaMa、Mask R-CNN、MTCNN 等）保留在仓库根目录的 `python/` 中，不再随本工程演进。
