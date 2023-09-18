"""
文本摘要
@Video(https://www.bilibili.com/video/BV1Kp4y137ar/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/15-text_summarization/summarization_glm.ipynb)
@Env(bert-cn)
"""
import torch
from datasets import Dataset
from rouge_chinese import Rouge
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, Seq2SeqTrainer
from transformers import Seq2SeqTrainingArguments


def process_func(exmaples):
    contents = ["摘要生成: \n" + e + tokenizer.mask_token for e in exmaples["content"]]
    inputs = tokenizer(contents, max_length=384, truncation=True, padding="max_length", return_tensors="pt")
    inputs = tokenizer.build_inputs_for_generation(inputs, targets=exmaples['title'], padding=True, max_gen_length=64)
    return inputs


def predict_test():
    predict = []
    with torch.inference_mode():
        for d in ds["test"]:
            inputs = tokenizer("摘要生成: \n" + d["content"] + tokenizer.mask_token, return_tensors="pt")
            inputs = tokenizer.build_inputs_for_generation(inputs, max_gen_length=64)
            inputs = inputs.to("cuda")
            output = model.generate(**inputs, max_new_tokens=64, eos_token_id=tokenizer.eop_token_id, do_sample=True)
            predict.append(tokenizer.decode(output[0].tolist()).split("<|startofpiece|>")[1].replace("<|endofpiece|>", "").strip())
            print("curID:", len(predict))
    return predict


ds = Dataset.load_from_disk("../../data/nlpcc_2017/")
ds = ds.train_test_split(100, seed=42)
tokenizer = AutoTokenizer.from_pretrained("THUDM/glm-large-chinese", trust_remote_code=True)
tokenized_ds = ds.map(process_func, batched=True, remove_columns=ds["train"].column_names)
model = AutoModelForSeq2SeqLM.from_pretrained("THUDM/glm-large-chinese", trust_remote_code=True)
args = Seq2SeqTrainingArguments(
    output_dir="../../data/summary_glm",
    per_device_train_batch_size=4,
    per_device_eval_batch_size=8,
    gradient_accumulation_steps=8,
    logging_steps=8,
    num_train_epochs=1
)
trainer = Seq2SeqTrainer(
    args=args,
    model=model,
    train_dataset=tokenized_ds["train"],
    tokenizer=tokenizer,
)
trainer.train()
input_text = ds["test"][-1]["content"]
inputs = tokenizer("摘要生成: \n" + input_text + tokenizer.mask_token, return_tensors="pt")
inputs = tokenizer.build_inputs_for_generation(inputs, max_gen_length=64)
inputs = inputs.to("cuda")
output = model.generate(**inputs, max_new_tokens=64, eos_token_id=tokenizer.eop_token_id, do_sample=True)
print(tokenizer.decode(output[0].tolist()))

model = model.eval()
result = predict_test()
print(result)


rouge = Rouge()
docode_preds = [" ".join(p) for p in result]
decode_labels = [" ".join(l) for l in ds["test"]["title"]]
scores = rouge.get_scores(docode_preds, decode_labels, avg=True)
print(scores)
