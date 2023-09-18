"""
文本摘要
@Video(https://www.bilibili.com/video/BV1Kp4y137ar/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/15-text_summarization/summarization.ipynb)
@Env(bert-cn)
"""
import numpy as np
from datasets import Dataset
from rouge_chinese import Rouge
from transformers import pipeline, Seq2SeqTrainingArguments
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, DataCollatorForSeq2Seq, Seq2SeqTrainer


def process_func(exmaples):
    contents = ["摘要生成: \n" + e for e in exmaples["content"]]
    inputs = tokenizer(contents, max_length=384, truncation=True)
    labels = tokenizer(text_target=exmaples["title"], max_length=64, truncation=True)
    inputs["labels"] = labels["input_ids"]
    return inputs


def compute_metric(evalPred):
    predictions, labels = evalPred
    decode_preds = tokenizer.batch_decode(predictions, skip_special_tokens=True)
    labels = np.where(labels != -100, labels, tokenizer.pad_token_id)
    decode_labels = tokenizer.batch_decode(labels, skip_special_tokens=True)
    decode_preds = [" ".join(p) for p in decode_preds]
    decode_labels = [" ".join(l) for l in decode_labels]
    scores = rouge.get_scores(decode_preds, decode_labels, avg=True)
    return {
        "rouge-1": scores["rouge-1"]["f"],
        "rouge-2": scores["rouge-2"]["f"],
        "rouge-l": scores["rouge-l"]["f"],
    }


# title,content
ds = Dataset.load_from_disk("../../data/nlpcc_2017/")
ds = ds.train_test_split(100, seed=42)
tokenizer = AutoTokenizer.from_pretrained("Langboat/mengzi-t5-base")
tokenized_ds = ds.map(process_func, batched=True)
print(tokenizer.decode(tokenized_ds["train"][0]["input_ids"]))
print(tokenizer.decode(tokenized_ds["train"][0]["labels"]))
model = AutoModelForSeq2SeqLM.from_pretrained("Langboat/mengzi-t5-base")

rouge = Rouge()
args = Seq2SeqTrainingArguments(
    output_dir="../../data/summary",
    per_device_train_batch_size=4,
    per_device_eval_batch_size=8,
    gradient_accumulation_steps=8,
    logging_steps=8,
    evaluation_strategy="epoch",
    save_strategy="epoch",
    metric_for_best_model="rouge-l",
    predict_with_generate=True
)
trainer = Seq2SeqTrainer(
    args=args,
    model=model,
    train_dataset=tokenized_ds["train"],
    eval_dataset=tokenized_ds["test"],
    compute_metrics=compute_metric,
    tokenizer=tokenizer,
    data_collator=DataCollatorForSeq2Seq(tokenizer=tokenizer)
)
trainer.train()
pipe = pipeline("text2text-generation", model=model, tokenizer=tokenizer, device=0)
print(pipe("摘要生成:\n" + ds["test"][-1]["content"], max_length=64, do_sample=True))
