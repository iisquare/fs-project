"""
多项选择
@Video(https://www.bilibili.com/video/BV1FM4y1E77w/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/11-multiple_choice/multiple_choice.ipynb)
"""
import evaluate
import numpy as np
from typing import Any
import torch
from datasets import load_dataset
from transformers import AutoTokenizer, AutoModelForMultipleChoice, TrainingArguments, Trainer


class MultipleChoicePipeline:

    def __init__(self, model, tokenizer) -> None:
        self.model = model
        self.tokenizer = tokenizer
        self.device = model.device

    def preprocess(self, context, quesiton, choices):
        cs, qcs = [], []
        for choice in choices:
            cs.append(context)
            qcs.append(quesiton + " " + choice)
        return tokenizer(cs, qcs, truncation="only_first", max_length=256, return_tensors="pt")

    def predict(self, inputs):
        inputs = {k: v.unsqueeze(0).to(self.device) for k, v in inputs.items()}
        return self.model(**inputs).logits

    def postprocess(self, logits, choices):
        predition = torch.argmax(logits, dim=-1).cpu().item()
        return choices[predition]

    def __call__(self, context, question, choices) -> Any:
        inputs = self.preprocess(context, question, choices)
        logits = self.predict(inputs)
        result = self.postprocess(logits, choices)
        return result


def process_function(examples):
    # examples, dict, keys: ["context", "quesiton", "choice", "answer"]
    # examples, 1000
    # {"id":0,"context":["男：xxxx？","女：...."],"question":"xxxx？","choice":["a","b","c"],"answer":"a"}
    context = []
    question_choice = []
    labels = []
    for idx in range(len(examples["context"])):
        ctx = "\n".join(examples["context"][idx])
        question = examples["question"][idx]
        choices = examples["choice"][idx]
        for choice in choices:
            context.append(ctx)
            question_choice.append(question + " " + choice)
        if len(choices) < 4:
            for _ in range(4 - len(choices)):
                context.append(ctx)
                question_choice.append(question + " " + "不知道")
        labels.append(choices.index(examples["answer"][idx]))
    tokenized_examples = tokenizer(context, question_choice, truncation="only_first", max_length=256, padding="max_length")     # input_ids: 4000 * 256,
    tokenized_examples = {k: [v[i: i + 4] for i in range(0, len(v), 4)] for k, v in tokenized_examples.items()}     # 1000 * 4 *256
    tokenized_examples["labels"] = labels
    return tokenized_examples


def compute_metric(pred):
    predictions, labels = pred
    predictions = np.argmax(predictions, axis=-1)
    return accuracy.compute(predictions=predictions, references=labels)


c3 = load_dataset("c3", "mixed")  # mixed/dialog
tokenized_c3 = c3.map(process_function, batched=True)
tokenizer = AutoTokenizer.from_pretrained("hfl/chinese-macbert-base")
model = AutoModelForMultipleChoice.from_pretrained("hfl/chinese-macbert-base")
accuracy = evaluate.load("accuracy")
args = TrainingArguments(
    output_dir="../../data/muliple_choice",
    per_device_train_batch_size=16,
    per_gpu_eval_batch_size=16,
    num_train_epochs=3,
    logging_steps=50,
    evaluation_strategy="epoch",
    save_strategy="epoch",
    load_best_model_at_end=True,
    fp16=True
)
trainer = Trainer(
    model=model,
    args=args,
    train_dataset=tokenized_c3["train"],
    eval_dataset=tokenized_c3["validation"],
    compute_metrics=compute_metric
)
trainer.train()
pipe = MultipleChoicePipeline(model, tokenizer)
print(pipe("小明在北京上班", "小明在哪里上班？", ["北京", "上海", "河北", "海南", "河北", "海南"]))
