"""
命名实体识别
@Video(https://www.bilibili.com/video/BV1gW4y197CT/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/09-token_classification/ner.ipynb)
"""
import evaluate
import numpy as np
from datasets import load_dataset
from transformers import AutoTokenizer, AutoModelForTokenClassification, TrainingArguments, Trainer, \
    DataCollatorForTokenClassification, pipeline


def process_function(examples):
    tokenized_exmaples = tokenizer(examples["tokens"], max_length=128, truncation=True, is_split_into_words=True)
    labels = []
    for i, label in enumerate(examples["ner_tags"]):
        word_ids = tokenized_exmaples.word_ids(batch_index=i)
        label_ids = []
        for word_id in word_ids:
            if word_id is None:
                label_ids.append(-100)
            else:
                label_ids.append(label[word_id])
        labels.append(label_ids)
    tokenized_exmaples["labels"] = labels
    return tokenized_exmaples


def eval_metric(pred):
    predictions, labels = pred
    predictions = np.argmax(predictions, axis=-1)

    # 将id转换为原始的字符串类型的标签
    true_predictions = [
        [label_list[p] for p, l in zip(prediction, label) if l != -100]
        for prediction, label in zip(predictions, labels)
    ]

    true_labels = [
        [label_list[l] for p, l in zip(prediction, label) if l != -100]
        for prediction, label in zip(predictions, labels)
    ]

    result = seqeval.compute(predictions=true_predictions, references=true_labels, mode="strict", scheme="IOB2")

    return {
        "f1": result["overall_f1"]
    }


ner_datasets = load_dataset("peoples_daily_ner")
# datasets = DatasetDict.load_from_disk("../../data/peoples_daily_ner")
tokenizer = AutoTokenizer.from_pretrained("hfl/chinese-macbert-base")
# 对于已经做好tokenize的数据，要指定is_split_into_words参数为True
tokenizer(ner_datasets["train"][0]["tokens"], is_split_into_words=True)
tokenized_datasets = ner_datasets.map(process_function, batched=True)
# 对于所有的非二分类任务，切记要指定num_labels，否则就会device错误
label_list = ner_datasets["train"].features["ner_tags"].feature.names
model = AutoModelForTokenClassification.from_pretrained("hfl/chinese-macbert-base", num_labels=len(label_list))
seqeval = evaluate.load("../../models/demo/seqeval_metric.py")
args = TrainingArguments(
    output_dir="../../data/bert-ner",
    per_device_train_batch_size=64,
    per_device_eval_batch_size=128,
    evaluation_strategy="epoch",
    save_strategy="epoch",
    metric_for_best_model="f1",
    load_best_model_at_end=True,
    logging_steps=50,
    num_train_epochs=3
)
trainer = Trainer(
    model=model,
    args=args,
    train_dataset=tokenized_datasets["train"],
    eval_dataset=tokenized_datasets["validation"],
    compute_metrics=eval_metric,
    data_collator=DataCollatorForTokenClassification(tokenizer=tokenizer)
)
trainer.train()
trainer.evaluate(eval_dataset=tokenized_datasets["test"])
# 使用pipeline进行推理，要指定id2label
model.config.id2label = {idx: label for idx, label in enumerate(label_list)}
# 如果模型是基于GPU训练的，那么推理时要指定device
# 对于NER任务，可以指定aggregation_strategy为simple，得到具体的实体的结果，而不是token的结果
ner_pipe = pipeline("token-classification", model=model, tokenizer=tokenizer, device=0, aggregation_strategy="simple")
text = "小明在北京上班"
res = ner_pipe(text)
print(res)
# 根据start和end取实际的结果
ner_result = {}
for r in res:
    if r["entity_group"] not in ner_result:
        ner_result[r["entity_group"]] = []
    ner_result[r["entity_group"]].append(text[r["start"]: r["end"]])
print(ner_result)
