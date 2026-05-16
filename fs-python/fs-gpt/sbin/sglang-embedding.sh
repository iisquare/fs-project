#!/bin/bash

# https://docs.sglang.ai/backend/server_arguments.html

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
MODEL_PATH=${PROJECT_ROOT}/models/bge-large-zh-v1.5
API_KEY=fs-gpt

export CUDA_VISIBLE_DEVICES=0

cmd=$(cat <<- EOF
python -m sglang.launch_server --model-path ${MODEL_PATH} \
--api-key ${API_KEY} \
--is-embedding \
--trust-remote-code \
--host 0.0.0.0 \
--port 6011
EOF
)

if [ "-d" = "$1" ]; then
  echo "run in daemon model..."
  cmd="nohup ${cmd} > ${PROJECT_ROOT}/logs/sglang.log 2>&1 &"
fi

eval $cmd
