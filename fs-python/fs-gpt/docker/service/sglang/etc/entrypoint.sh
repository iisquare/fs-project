#!/bin/bash

MODEL_PATH=/data/models/Qwen2.5-0.5B-Instruct
API_KEY=fs-gpt

python3 -m sglang.launch_server --model-path ${MODEL_PATH} \
--api-key ${API_KEY} \
--trust-remote-code \
--tp 1 \
--dp 1 \
--host 0.0.0.0 \
--port 30000
