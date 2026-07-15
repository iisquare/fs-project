#!/bin/bash

MODEL_PATH=/data/models/Qwen3.5-0.8B-GPTQ-Int4
LORA_PATH=/data/models/qwen-sft-qlora-bnb
API_KEY=fs-gpt

vllm serve ${MODEL_PATH} \
--api-key ${API_KEY} \
--served-model-name $(basename ${MODEL_PATH}) \
--enable-lora \
--lora-modules $(basename ${LORA_PATH})=${LORA_PATH} \
--tensor-parallel-size 1 \
--pipeline-parallel-size 1 \
--max-model-len 4096 \
--gpu-memory-utilization 0.6 \
--max-num-seqs 8 \
--host 0.0.0.0 \
--port 8000
