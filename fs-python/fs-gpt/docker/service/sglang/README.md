## 问题

### [解决sglang镜像推理不了量化模型](https://blog.csdn.net/jenken1209/article/details/147723634)

当sglang镜像版本大于0.4.4之后 vllm（0.7.2）不再安装到镜像中，
推理awq/gptq等会出现不支持该量化格式的报错（如：name ‘WNA16_SUPPORTED_BITS’ is not defined）。

```shell
pip install vllm==0.7.2 --no-deps -i https://pypi.tuna.tsinghua.edu.cn/simple/
```
