## fs-gpt

## 如何使用

### 环境安装

- 创建环境
```
conda create -n fs-gpt python=3.10
conda activate fs-gpt
```

- 安装项目
```
pip install -e ".[all]"
# 或手动指定依赖
pip install -e ".[torch, embedding, inference]"
# 指定cuda版本
pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu129
```

## 运行服务

### 训练

- 全参数微调（Full Parameter Fine-Tuning）
```
fs-gpt run examples/train_pt_full.yaml
```
- 部分参数微调（Partial Parameter Fine-Tuning）
- LoRA（Low-Rank Adaptation）
- QLoRA（Quantized Low-Rank Adaptation）

### 词向量

```
pip install -e ".[embedding]"
fs-gpt run examples/embedding.yaml
```
### 量化

- GPTQ
```
pip install gptqmodel
fs-gpt run examples/derive_gptq.yaml
```
- AWQ
```
pip install autoawq
export CUDA_VISIBLE_DEVICES=-1 # 禁用GPU，采用纯CPU方式量化
export HF_ENDPOINT=https://hf-mirror.com # 用于下载校准数据集（calib_data, calibration dataset）
fs-gpt run examples/derive_awq.yaml
```

## 开发计划

### 功能说明

- 预训练
- 微调训练：LoRA、QLoRA
- 模型合并：LoRA合并、量化、GPTQ、AWQ
- 推理接口：Bitsandbytes、vLLM、SGLang、LoRA切换
- 模型评估

## 相关参考

### 参考项目

- [xusenlinzy/api-for-open-llm](https://github.com/xusenlinzy/api-for-open-llm)
- [hiyouga/LLaMA-Factory](https://github.com/hiyouga/LLaMA-Factory)

### 参考文档
