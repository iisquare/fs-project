"""
生成式对话机器人
@Video(https://www.bilibili.com/video/BV11r4y197Ht/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/16-generative_chatbot/chatbot.ipynb)
@Env(bert-cn)
"""
from datasets import Dataset
from transformers import pipeline
from transformers import AutoTokenizer, AutoModelForCausalLM, DataCollatorForSeq2Seq, TrainingArguments, Trainer


def process_func(example):
    MAX_LENGTH = 256
    input_ids, attention_mask, labels = [], [], []
    instruction = tokenizer("\n".join(["Human: " + example["instruction"], example["input"]]).strip() + "\n\nAssistant: ")
    response = tokenizer(example["output"] + tokenizer.eos_token)
    input_ids = instruction["input_ids"] + response["input_ids"]
    attention_mask = instruction["attention_mask"] + response["attention_mask"]
    labels = [-100] * len(instruction["input_ids"]) + response["input_ids"]
    if len(input_ids) > MAX_LENGTH:
        input_ids = input_ids[:MAX_LENGTH]
        attention_mask = attention_mask[:MAX_LENGTH]
        labels = labels[:MAX_LENGTH]
    return {
        "input_ids": input_ids,
        "attention_mask": attention_mask,
        "labels": labels
    }


ds = Dataset.load_from_disk("../../data/alpaca_data_zh/")
tokenizer = AutoTokenizer.from_pretrained("Langboat/bloom-389m-zh")
tokenized_ds = ds.map(process_func, remove_columns=ds.column_names)
print(tokenizer.decode(tokenized_ds[1]["input_ids"]))
print(tokenizer.decode(list(filter(lambda x: x != -100, tokenized_ds[1]["labels"]))))
model = AutoModelForCausalLM.from_pretrained("Langboat/bloom-389m-zh")
args = TrainingArguments(
    output_dir="../../data/chatbot",
    per_device_train_batch_size=4,
    gradient_accumulation_steps=8,
    logging_steps=10,
    num_train_epochs=2
)
trainer = Trainer(
    model=model,
    args=args,
    train_dataset=tokenized_ds,
    data_collator=DataCollatorForSeq2Seq(tokenizer=tokenizer, padding=True)
)
trainer.train()
pipe = pipeline("text-generation", model=model, tokenizer=tokenizer, device=0)
ipt = "Human: {}\n{}".format("考试有哪些技巧？", "").strip() + "\n\nAssistant: "
print(pipe(ipt, max_length=256, do_sample=True, ))
