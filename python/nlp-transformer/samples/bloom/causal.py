"""
因果语言
@Video(https://www.bilibili.com/video/BV1B44y1c7x2/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/14-language_model/causal_lm.ipynb)
@Env(bert-cn)
"""
from datasets import load_dataset, Dataset
from torch.utils.data import DataLoader
from transformers import pipeline
from transformers import AutoTokenizer, AutoModelForCausalLM, DataCollatorForLanguageModeling, TrainingArguments, Trainer, Bloo


def process_func(examples):
    contents = [e + tokenizer.eos_token for e in examples["completion"]]
    return tokenizer(contents, max_length=384, truncation=True)


ds = Dataset.load_from_disk("../../data/wiki_cn_filtered/")
tokenizer = AutoTokenizer.from_pretrained("Langboat/bloom-389m-zh")
tokenized_ds = ds.map(process_func, batched=True, remove_columns=ds.column_names)
dl = DataLoader(tokenized_ds, batch_size=2, collate_fn=DataCollatorForLanguageModeling(tokenizer, mlm=False))
model = AutoModelForCausalLM.from_pretrained("Langboat/bloom-389m-zh")
args = TrainingArguments(
    output_dir="./causal_lm",
    per_device_train_batch_size=4,
    gradient_accumulation_steps=8,
    logging_steps=10,
    num_train_epochs=1
)
trainer = Trainer(
    args=args,
    model=model,
    train_dataset=tokenized_ds,
    data_collator=DataCollatorForLanguageModeling(tokenizer, mlm=False)
)
trainer.train()
pipe = pipeline("text-generation", model=model, tokenizer=tokenizer, device=0)
print(pipe("西安交通大学博物馆（Xi'an Jiaotong University Museum）是一座位于西安", max_length=128, do_sample=True))
print(pipe("下面是一则游戏新闻。小编报道，近日，游戏产业发展的非常", max_length=128, do_sample=True))
