#!/bin/bash

# https://docs.vllm.ai/en/latest/serving/engine_args.html

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
MODEL_PATH=${PROJECT_ROOT}/models/Qwen2.5-0.5B-Instruct
API_KEY=fs-gpt

export CUDA_VISIBLE_DEVICES=0
export VLLM_LOGGING_CONFIG_PATH=config/log_vllm_generate.json


cmd=$(cat <<- EOF
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
--port 6011
EOF
)

if [ "-d" = "$1" ]; then
  echo "run in daemon model..."
  cmd="nohup ${cmd} &"
fi

eval $cmd
