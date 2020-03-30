## Nvidia CUDA
- [GPU算力](https://developer.nvidia.com/cuda-gpus)
- [显卡驱动](https://www.geforce.com/drivers)
- [CUDA Toolkit and Compatible Driver Versions](https://docs.nvidia.com/deploy/cuda-compatibility/index.html)

## Anaconda
- 从文件创建环境
```
conda env create -f environment.yml
```
- 单独创建环境
```
conda create --name xxx python=3.7
```
- 查看及使用环境
```
conda info --envs
conda activate xxx
```
- 安装及卸载组件
```
conda install jupyter
conda uninstall jupyter
```
- 退出及删除环境
```
conda deactivate
conda env remove -n xxx
```

## 参考链接
