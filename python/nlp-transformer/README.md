# Transformers

Transformers: State-of-the-art Machine Learning for Pytorch, TensorFlow, and JAX.

## 如何使用

### 模型固化
- 手动下载
```
https://huggingface.co/models
```
- 工具下载
```
pip install huggingface_hub
from huggingface_hub import snapshot_download
snapshot_download(repo_id="bert-base-chinese")
# Out[2]: '~/.cache/huggingface/hub/bert-base-chinese.main.4b1f5fb6deac3583018fcf351473024a3d65b2d4'
```
- 自动下载
```
from transformers import pipeline
classifier = pipeline('sentiment-analysis')
classifier.save_pretrained('./data/sentiment-analysis')
```
- 本地加载
```
from transformers import pipeline
classifier = pipeline('sentiment-analysis', model='./data/sentiment-analysis')
```
- 缓存目录
```
from transformers import BertTokenizer
tokenizer = BertTokenizer.from_pretrained('bert-base-chinese', cache_dir='./cache')
```

## 参考链接
- [github:huggingface/transformers](https://github.com/huggingface/transformers)
- [Transformer和BERT的前世今生](https://www.bilibili.com/video/BV11v4y137sN/)
- [模型量化加速](https://www.bilibili.com/video/BV1xf4y1f7wn/)
