## Nvidia CUDA
- [GPU算力](https://developer.nvidia.com/cuda-gpus)
- [显卡驱动](https://www.geforce.com/drivers)
- [CUDA Toolkit and Compatible Driver Versions](https://docs.nvidia.com/deploy/cuda-compatibility/index.html)

## Nvidia VGA
| 型号 | 显存 | 位宽 | 频率 | 建议电源 | 电源接口 | 参考价 |
| :----- | :----- | :----- | :----- | :----- | :----- | :----- |
| GeForce RTX 3090 | 24GB GDDR6X | 384bit | 19500MHz | 750W | 8pin+8pin | 12K |
| GeForce RTX 3080 | 10GB GDDR6X | 320bit | 19000MHz | 750W | 8pin+8pin | 5.5K |
| GeForce RTX 3070 | 8GB GDDR6 | 256bit | 14000MHz | 650W | 8pin | 3.9K |
| GeForce RTX 3060Ti | 8GB GDDR6 | 256bit | 14000MHz | 600W | 8pin | 3K |
| GeForce RTX 2080Ti | 11GB GDDR6 | 352bit | 14000MHz | - | 8pin+8pin | 10K |
| GeForce RTX 2080 SUPER | 8GB GDDR6 | 256bit | 15500MHz | 650W | 6pin+8pin | 8K |
| GeForce RTX 2080 | 8GB GDDR6 | 256bit | 14000MHz | - | 6pin+8pin | 14K |
| GeForce RTX 2070 SUPER | 8GB GDDR6 | 256bit | 14000MHz | 650W | 6pin+8pin | 3.2K |
| GeForce RTX 2070 | 8GB GDDR6 | 256bit | 14000MHz | - | 8pin | 6.6K |
| GeForce RTX 2060 SUPER | 8GB GDDR6 | 256bit | 14000MHz | 550W | 8pin | 4K |
| GeForce GTX 1080Ti | 11GB GDDR5X | 352bit | 11000MHz | - | 6pin+8pin | 8.7K |
| GeForce GTX 1080 | 8GB GDDR5X | 256bit | 10000MHz | - | 8pin | 4.9K |
| GeForce GTX 1070Ti | 8GB GDDR5 | 256bit | 8000MHz | - | 8pin | - |


## Project
| 名称 | 说明 | 备注 |
| :----- | :----- | :----- |
| [attentive-gan-derainnet](https://github.com/MaybeShewill-CV/attentive-gan-derainnet) | 去雨滴 | 无 |
| [crnn](https://github.com/MaybeShewill-CV/CRNN_Tensorflow) | 文本识别 | 无 |
| [east](https://github.com/argman/EAST) | 文本检测 | 无 |
| [mask-rcnn](https://github.com/matterport/Mask_RCNN) | 图片语义分割 | 无 |
| [multi-label-image](https://github.com/suraj-deshmukh/Keras-Multi-Label-Image-Classification) | 图片多标签分类 | 无 |
| [mtcnn](https://github.com/AITTSMD/MTCNN-Tensorflow) | 人脸检测 | 无 |
| [insightface](https://github.com/auroua/InsightFace_TF) | 人脸识别 | 无 |
| [deep-image-prior](https://github.com/DmitryUlyanov/deep-image-prior) | 降噪、修复、超分辨率 | 无 |


## Anaconda
- 从文件创建环境
```
conda env create -f environment.yml

name: env-name
channels:
  - pytorch
dependencies:
  - python=3.6
  - conda-package
  - pip:
    - pip-package

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
- 与pip协作
```
pip freeze > requirements.txt
pip install -r requirements.txt
conda list -e > requirements.txt
conda install --yes --file requirements.txt
```

## Config
- ~/.condarc & conda clean -i
```
channels:
  - defaults
show_channel_urls: true
channel_alias: https://mirrors.tuna.tsinghua.edu.cn/anaconda
default_channels:
  - https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/main
  - https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/free
  - https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/r
  - https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/pro
  - https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/msys2
custom_channels:
  conda-forge: https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud
  msys2: https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud
  bioconda: https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud
  menpo: https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud
  pytorch: https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud
  simpleitk: https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud
ssl_verify: true
remote_read_timeout_secs: 100.0
report_errors: false
```
- ~/.pip/pip.conf
```
[global]
timeout = 6000
index-url = https://pypi.tuna.tsinghua.edu.cn/simple
```

## 参考链接
- [资源下载-xavs](https://pan.baidu.com/s/1ADu8Qevh95qtWQPsDBuUpA#xavs)
- [TensorFlow Keras Version](https://docs.floydhub.com/guides/environments/)
- [TensorFlow CUDA Version](https://tensorflow.google.cn/install/source)
