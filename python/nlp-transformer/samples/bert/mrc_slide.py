"""
机器阅读理解(MRC,Machine Reading Comprehension)
@Video(https://www.bilibili.com/video/BV1uN411D7oy/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/10-question_answering/mrc_slide_version.ipynb)
"""
import collections
import numpy as np
from transformers import pipeline
from datasets import load_dataset, DatasetDict
from transformers import AutoTokenizer, AutoModelForQuestionAnswering, TrainingArguments, Trainer, DefaultDataCollator
import sys
sys.path.append("../..")
from models.demo.cmrc_eval import evaluate_cmrc


def metirc(pred):
    start_logits, end_logits = pred[0]
    if start_logits.shape[0] == len(tokenied_datasets["validation"]):
        p, r = get_result(start_logits, end_logits, datasets["validation"], tokenied_datasets["validation"])
    else:
        p, r = get_result(start_logits, end_logits, datasets["test"], tokenied_datasets["test"])
    return evaluate_cmrc(p, r)


def process_func(examples):
    tokenized_examples = tokenizer(text=examples["question"],
                                   text_pair=examples["context"],
                                   return_offsets_mapping=True,
                                   return_overflowing_tokens=True,
                                   stride=128,
                                   max_length=384, truncation="only_second", padding="max_length")
    sample_mapping = tokenized_examples.pop("overflow_to_sample_mapping")
    start_positions = []
    end_positions = []
    example_ids = []
    for idx, _ in enumerate(sample_mapping):
        answer = examples["answers"][sample_mapping[idx]]
        start_char = answer["answer_start"][0]
        end_char = start_char + len(answer["text"][0])
        # 定位答案在token中的起始位置和结束位置
        # 一种策略，我们要拿到context的起始和结束，然后从左右两侧向答案逼近
        context_start = tokenized_examples.sequence_ids(idx).index(1)
        context_end = tokenized_examples.sequence_ids(idx).index(None, context_start) - 1
        offset = tokenized_examples.get("offset_mapping")[idx]
        # 判断答案是否在context中
        if offset[context_end][1] < start_char or offset[context_start][0] > end_char:
            start_token_pos = 0
            end_token_pos = 0
        else:
            token_id = context_start
            while token_id <= context_end and offset[token_id][0] < start_char:
                token_id += 1
            start_token_pos = token_id
            token_id = context_end
            while token_id >= context_start and offset[token_id][1] > end_char:
                token_id -= 1
            end_token_pos = token_id
        start_positions.append(start_token_pos)
        end_positions.append(end_token_pos)
        example_ids.append(examples["id"][sample_mapping[idx]])
        tokenized_examples["offset_mapping"][idx] = [
            (o if tokenized_examples.sequence_ids(idx)[k] == 1 else None)
            for k, o in enumerate(tokenized_examples["offset_mapping"][idx])
        ]

    tokenized_examples["example_ids"] = example_ids
    tokenized_examples["start_positions"] = start_positions
    tokenized_examples["end_positions"] = end_positions
    return tokenized_examples


def get_result(start_logits, end_logits, exmaples, features):

    predictions = {}
    references = {}

    # example 和 feature的映射
    example_to_feature = collections.defaultdict(list)
    for idx, example_id in enumerate(features["example_ids"]):
        example_to_feature[example_id].append(idx)

    # 最优答案候选
    n_best = 20
    # 最大答案长度
    max_answer_length = 30

    for example in exmaples:
        example_id = example["id"]
        context = example["context"]
        answers = []
        for feature_idx in example_to_feature[example_id]:
            start_logit = start_logits[feature_idx]
            end_logit = end_logits[feature_idx]
            offset = features[feature_idx]["offset_mapping"]
            start_indexes = np.argsort(start_logit)[::-1][:n_best].tolist()
            end_indexes = np.argsort(end_logit)[::-1][:n_best].tolist()
            for start_index in start_indexes:
                for end_index in end_indexes:
                    if offset[start_index] is None or offset[end_index] is None:
                        continue
                    if end_index < start_index or end_index - start_index + 1 > max_answer_length:
                        continue
                    answers.append({
                        "text": context[offset[start_index][0]: offset[end_index][1]],
                        "score": start_logit[start_index] + end_logit[end_index]
                    })
        if len(answers) > 0:
            best_answer = max(answers, key=lambda x: x["score"])
            predictions[example_id] = best_answer["text"]
        else:
            predictions[example_id] = ""
        references[example_id] = example["answers"]["text"]

    return predictions, references


datasets = load_dataset("cmrc2018")
tokenizer = AutoTokenizer.from_pretrained("hfl/chinese-macbert-base")
sample_dataset = datasets["train"].select(range(10))
tokenized_examples = tokenizer(text=sample_dataset["question"],
                               text_pair=sample_dataset["context"],
                               return_offsets_mapping=True,
                               return_overflowing_tokens=True,
                               stride=128,
                               max_length=384, truncation="only_second", padding="max_length")
sample_mapping = tokenized_examples.pop("overflow_to_sample_mapping")
for idx, _ in enumerate(sample_mapping):
    answer = sample_dataset["answers"][sample_mapping[idx]]
    start_char = answer["answer_start"][0]
    end_char = start_char + len(answer["text"][0])
    # 定位答案在token中的起始位置和结束位置
    # 一种策略，我们要拿到context的起始和结束，然后从左右两侧向答案逼近

    context_start = tokenized_examples.sequence_ids(idx).index(1)
    context_end = tokenized_examples.sequence_ids(idx).index(None, context_start) - 1

    offset = tokenized_examples.get("offset_mapping")[idx]
    example_ids = []

    # 判断答案是否在context中
    if offset[context_end][1] < start_char or offset[context_start][0] > end_char:
        start_token_pos = 0
        end_token_pos = 0
    else:
        token_id = context_start
        while token_id <= context_end and offset[token_id][0] < start_char:
            token_id += 1
        start_token_pos = token_id
        token_id = context_end
        while token_id >= context_start and offset[token_id][1] > end_char:
            token_id -= 1
        end_token_pos = token_id
        example_ids.append(end_token_pos)

    print(answer, start_char, end_char, context_start, context_end, start_token_pos, end_token_pos)
    print("token answer decode:",
          tokenizer.decode(tokenized_examples["input_ids"][idx][start_token_pos: end_token_pos + 1]))
tokenied_datasets = datasets.map(process_func, batched=True, remove_columns=datasets["train"].column_names)
example_to_feature = collections.defaultdict(list)
for idx, example_id in enumerate(tokenied_datasets["train"]["example_ids"][:10]):
    example_to_feature[example_id].append(idx)
model = AutoModelForQuestionAnswering.from_pretrained("hfl/chinese-macbert-base")
args = TrainingArguments(
    output_dir="../../data/models_for_qa",
    per_device_train_batch_size=32,
    per_device_eval_batch_size=32,
    evaluation_strategy="steps",
    eval_steps=200,
    save_strategy="epoch",
    logging_steps=50,
    num_train_epochs=3
)
trainer = Trainer(
    model=model,
    args=args,
    train_dataset=tokenied_datasets["train"],
    eval_dataset=tokenied_datasets["validation"],
    data_collator=DefaultDataCollator(),
    compute_metrics=metirc
)
trainer.train()
pipe = pipeline("question-answering", model=model, tokenizer=tokenizer, device=0)
pipe(question="小明在哪里上班？", context="小明在北京上班")
