"""
掩码语言模型
@Video(https://www.bilibili.com/video/BV1B44y1c7x2/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/14-language_model/masked_lm.ipynb)
"""
from datasets import load_dataset, Dataset
from torch.utils.data import DataLoader
from transformers import pipeline
from transformers import AutoTokenizer, AutoModelForMaskedLM, DataCollatorForLanguageModeling, TrainingArguments, Trainer


def process_func(examples):
    return tokenizer(examples["completion"], max_length=384, truncation=True)


ds = Dataset.load_from_disk("../../data/wiki_cn_filtered/")
tokenizer = AutoTokenizer.from_pretrained("hfl/chinese-macbert-base")
tokenized_ds = ds.map(process_func, batched=True, remove_columns=ds.column_names)
dl = DataLoader(tokenized_ds, batch_size=2, collate_fn=DataCollatorForLanguageModeling(tokenizer, mlm=True, mlm_probability=0.15))
print(next(enumerate(dl)))
model = AutoModelForMaskedLM.from_pretrained("hfl/chinese-macbert-base")
args = TrainingArguments(
    output_dir="./masked_lm",
    per_device_train_batch_size=32,
    logging_steps=10,
    num_train_epochs=1
)
trainer = Trainer(
    args=args,
    model=model,
    train_dataset=tokenized_ds,
    data_collator=DataCollatorForLanguageModeling(tokenizer, mlm=True, mlm_probability=0.15)
)
trainer.train()
pipe = pipeline("fill-mask", model=model, tokenizer=tokenizer, device=0)
print(pipe("西安交通[MASK][MASK]博物馆（Xi'an Jiaotong University Museum）是一座位于西安交通大学的博物馆"))
