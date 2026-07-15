## 最佳实践

### CUDA图

若非显存不足、兼容性等问题，生产环境尽量避免启用`--enforce-eager`参数，性能会严重下降。

## 参考文档

- [OpenAI-Compatible Server](https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html#openai-compatible-server)
