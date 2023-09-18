"""
文本相似度
@Video(https://www.bilibili.com/video/BV13P411C7UD/)
@See(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/12-sentence_similarity/cross_model.ipynb)
"""
import evaluate
from datasets import load_dataset
from transformers import pipeline, DataCollatorWithPadding
from transformers import AutoTokenizer, AutoModelForSequenceClassification, Trainer, TrainingArguments


def process_function(examples):
    tokenized_examples = tokenizer(examples["sentence1"], examples["sentence2"], max_length=128, truncation=True)
    tokenized_examples["labels"] = [float(label) for label in examples["label"]]
    return tokenized_examples


def eval_metric(eval_predict):
    predictions, labels = eval_predict
    predictions = [int(p > 0.5) for p in predictions]
    labels = [int(l) for l in labels]
    # predictions = predictions.argmax(axis=-1)
    acc = acc_metric.compute(predictions=predictions, references=labels)
    f1 = f1_metirc.compute(predictions=predictions, references=labels)
    acc.update(f1)
    return acc


dataset = load_dataset("json", data_files="../../data/single/train_pair_1w.json", split="train")
datasets = dataset.train_test_split(test_size=0.2)
tokenizer = AutoTokenizer.from_pretrained("hfl/chinese-macbert-base")
tokenized_datasets = datasets.map(process_function, batched=True, remove_columns=datasets["train"].column_names)
model = AutoModelForSequenceClassification.from_pretrained("hfl/chinese-macbert-base", num_labels=1)
acc_metric = evaluate.load("accuracy")
f1_metirc = evaluate.load("f1")
train_args = TrainingArguments(output_dir="../../data/cross_model",      # 输出文件夹
                               per_device_train_batch_size=32,  # 训练时的batch_size
                               per_device_eval_batch_size=32,   # 验证时的batch_size
                               logging_steps=10,                # log 打印的频率
                               evaluation_strategy="epoch",     # 评估策略
                               save_strategy="epoch",           # 保存策略
                               save_total_limit=3,              # 最大保存数
                               learning_rate=2e-5,              # 学习率
                               weight_decay=0.01,               # weight_decay
                               metric_for_best_model="f1",      # 设定评估指标
                               load_best_model_at_end=True)     # 训练完成后加载最优模型
trainer = Trainer(model=model,
                  args=train_args,
                  train_dataset=tokenized_datasets["train"],
                  eval_dataset=tokenized_datasets["test"],
                  data_collator=DataCollatorWithPadding(tokenizer=tokenizer),
                  compute_metrics=eval_metric)
trainer.train()
trainer.evaluate(tokenized_datasets["test"])
model.config.id2label = {0: "不相似", 1: "相似"}
pipe = pipeline("text-classification", model=model, tokenizer=tokenizer, device=0)
result = pipe({"text": "我喜欢北京", "text_pair": "天气怎样"}, function_to_apply="none")
result["label"] = "相似" if result["score"] > 0.5 else "不相似"
print(result)
