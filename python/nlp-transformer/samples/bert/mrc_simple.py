"""
机器阅读理解(MRC,Machine Reading Comprehension)
@Video(https://www.bilibili.com/video/BV1rs4y1k7FX/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/10-question_answering/mrc_simple_version.ipynb)
"""
from transformers import pipeline
from datasets import load_dataset, DatasetDict
from transformers import AutoTokenizer, AutoModelForQuestionAnswering, TrainingArguments, Trainer, DefaultDataCollator


def process_func(examples):
    tokenized_examples = tokenizer(text=examples["question"],
                                   text_pair=examples["context"],
                                   return_offsets_mapping=True,
                                   max_length=384, truncation="only_second", padding="max_length")
    offset_mapping = tokenized_examples.pop("offset_mapping")
    start_positions = []
    end_positions = []
    for idx, offset in enumerate(offset_mapping):
        answer = examples["answers"][idx]
        start_char = answer["answer_start"][0]
        end_char = start_char + len(answer["text"][0])
        # 定位答案在token中的起始位置和结束位置
        # 一种策略，我们要拿到context的起始和结束，然后从左右两侧向答案逼近
        context_start = tokenized_examples.sequence_ids(idx).index(1)
        context_end = tokenized_examples.sequence_ids(idx).index(None, context_start) - 1
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

    tokenized_examples["start_positions"] = start_positions
    tokenized_examples["end_positions"] = end_positions
    return tokenized_examples


datasets = load_dataset("cmrc2018")
tokenizer = AutoTokenizer.from_pretrained("hfl/chinese-macbert-base")
sample_dataset = datasets["train"].select(range(10))
tokenized_examples = tokenizer(text=sample_dataset["question"],
                               text_pair=sample_dataset["context"],
                               return_offsets_mapping=True,
                               max_length=512, truncation="only_second", padding="max_length")
offset_mapping = tokenized_examples.pop("offset_mapping")
for idx, offset in enumerate(offset_mapping):
    answer = sample_dataset[idx]["answers"]
    start_char = answer["answer_start"][0]
    end_char = start_char + len(answer["text"][0])
    # 定位答案在token中的起始位置和结束位置
    # 一种策略，我们要拿到context的起始和结束，然后从左右两侧向答案逼近

    context_start = tokenized_examples.sequence_ids(idx).index(1)
    context_end = tokenized_examples.sequence_ids(idx).index(None, context_start) - 1

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

    print(answer, start_char, end_char, context_start, context_end, start_token_pos, end_token_pos)
    print("token answer decode:",
          tokenizer.decode(tokenized_examples["input_ids"][idx][start_token_pos: end_token_pos + 1]))
tokenied_datasets = datasets.map(process_func, batched=True, remove_columns=datasets["train"].column_names)
model = AutoModelForQuestionAnswering.from_pretrained("hfl/chinese-macbert-base")
args = TrainingArguments(
    output_dir="../../data/models_for_qa",
    per_device_train_batch_size=32,
    per_device_eval_batch_size=32,
    evaluation_strategy="epoch",
    save_strategy="epoch",
    logging_steps=50,
    num_train_epochs=3
)
trainer = Trainer(
    model=model,
    args=args,
    train_dataset=tokenied_datasets["train"],
    eval_dataset=tokenied_datasets["validation"],
    data_collator=DefaultDataCollator()
)
trainer.train()
pipe = pipeline("question-answering", model=model, tokenizer=tokenizer, device=0)
pipe(question="小明在哪里上班？", context="小明在北京上班。")
