#!/bin/bash

MODEL_PATH=/data/models/Qwen2.5-0.5B-Instruct
API_KEY=fs-gpt

vllm serve ${MODEL_PATH} \
--api-key ${API_KEY} \
--served-model-name $(basename ${MODEL_PATH}) \
--task generate \
--tensor-parallel-size 1 \
--pipeline-parallel-size 1 \
--max-model-len 32768 \
--gpu-memory-utilization 0.9 \
--max-num-seqs 8 \
--enforce-eager \
--host 0.0.0.0 \
--port 8000
