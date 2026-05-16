#!/bin/bash

# https://docs.vllm.ai/en/latest/serving/engine_args.html

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
MODEL_PATH=${PROJECT_ROOT}/models/Qwen2.5-0.5B-Instruct
LORA_PATH=${PROJECT_ROOT}/models/qwen-sft-lora

export CUDA_VISIBLE_DEVICES=0

cmd=$(cat <<- EOF
vllm serve ${MODEL_PATH} \
--served-model-name $(basename ${MODEL_PATH}) \
--enable-lora \
--lora-modules $(basename ${LORA_PATH})=${LORA_PATH} \
--tensor-parallel-size 1 \
--max-model-len 128 \
--gpu-memory-utilization 0.9 \
--max-num-seqs 8 \
--enforce-eager \
--host 0.0.0.0 \
--port 6011
EOF
)

if [ "-d" = "$1" ]; then
  echo "run in daemon model..."
  cmd="nohup ${cmd} > ${PROJECT_ROOT}/logs/vllm.log 2>&1 &"
fi

eval $cmd
