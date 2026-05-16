import os
from pathlib import Path
from typing import Dict, List

import torch
from transformers import BertModel, BertConfig, BertTokenizer

from fs_gpt.bert.bert import Actuator
from fs_gpt.data.JSONLDataset import JSONLDataset


class BertForMultiLabel(torch.nn.Module):
    def __init__(self, config):
        super(BertForMultiLabel, self).__init__()
        self.bert = BertModel(config)
        self.dropout = torch.nn.Dropout(config.hidden_dropout_prob)
        self.classifier = torch.nn.Linear(config.hidden_size, config.num_labels)
        self.sigmoid = torch.nn.Sigmoid()

    def forward(self, input_ids, token_type_ids=None, attention_mask=None):
        outputs = self.bert(input_ids, token_type_ids=token_type_ids, attention_mask=attention_mask)
        pooled_output = outputs[1]
        pooled_output = self.dropout(pooled_output)
        logist = self.classifier(pooled_output)
        return self.sigmoid(logist)

class ClassifyActuator(Actuator):

    def __init__(self, args: Dict) -> None:
        super().__init__(args)
        self.labels = args.get("classify_labels", ['UNK'])
        self.model_name_or_path = args.get("model_name_or_path")
        self.output_dir = self.args.get("output_dir", f"logs/{(os.path.basename(self.model_name_or_path))}")
        self.device = args.get("device", torch.device("cuda" if torch.cuda.is_available() else "cpu"))
        print(f"Load model from {self.model_name_or_path}")
        self.config = BertConfig.from_pretrained(self.model_name_or_path, num_labels=len(self.labels))
        self.tokenizer = BertTokenizer.from_pretrained(self.model_name_or_path)
        self.model = BertForMultiLabel(self.config).to(self.device)

    def collate(self, data):
        texts = [i['text'] for i in data]
        labels = []
        for item in data:
            elements = item['label'].split(',')
            row = [1 if label in elements else 0 for label in self.labels]
            labels.append(row)
        encoding = self.batch_encode(texts)
        input_ids = encoding['input_ids']
        attention_mask = encoding['attention_mask']
        token_type_ids = encoding['token_type_ids']
        return input_ids, attention_mask, token_type_ids, torch.FloatTensor(labels).to(self.device)

    def train(self):
        criterion = torch.nn.CrossEntropyLoss()
        optimizer = torch.optim.AdamW(self.model.parameters(), lr=self.args.get("learning_rate", 1e-5))

        print(f"Load train dataset with {self.args['train_dataset_names']}")
        train_dataset = JSONLDataset(self.args["train_dataset_names"], args=self.args)
        train_loader = torch.utils.data.DataLoader(dataset=train_dataset,
                                     batch_size=self.args.get("train_batch_size", 1),
                                     collate_fn=self.collate,
                                     shuffle=True,
                                     drop_last=True)
        print(f"Load evaluate dataset with {self.args['eval_dataset_names']}")
        eval_dataset = JSONLDataset(self.args["eval_dataset_names"], args=self.args)
        eval_loader = torch.utils.data.DataLoader(dataset=eval_dataset,
                                                   batch_size=self.args.get("eval_batch_size", 1),
                                                   collate_fn=self.collate,
                                                   shuffle=True,
                                                   drop_last=True)
        resume_from_checkpoint = self.args.get("resume_from_checkpoint", None)
        if resume_from_checkpoint is not None:
            print(f"Load checkpoint from {resume_from_checkpoint}")
            self.load_checkpoint(str(resume_from_checkpoint))
        print(f"Train...")
        for epoch in range(self.args.get("num_train_epochs", 1)):
            self.model.train()
            train_loss = 0
            for batch in train_loader:
                input_ids, attention_mask, token_type_ids, labels = batch
                optimizer.zero_grad()
                outputs = self.model(input_ids, token_type_ids=token_type_ids, attention_mask=attention_mask)
                loss = criterion(outputs, labels)
                loss.backward()
                optimizer.step()
                train_loss += loss.item()
            print(f'Epoch {epoch + 1}, Train accuracy: {train_loss / len(train_loader)}')
        print(f"Evaluate...")
        self.model.eval()
        eval_loss = 0
        with torch.no_grad():
            for batch in eval_loader:
                input_ids, attention_mask, token_type_ids, labels = batch
                outputs = self.model(input_ids, token_type_ids=token_type_ids, attention_mask=attention_mask)
                eval_loss += criterion(outputs, labels).item()
            print(f'Validation accuracy: {eval_loss / len(eval_loader)}')
        print(f"Save model to {self.output_dir}")
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)
        self.config.save_pretrained(self.output_dir)
        checkpoint_path = self.checkpoint_path(self.output_dir)
        torch.save(self.model.state_dict(), checkpoint_path) # 保存状态字典
        self.tokenizer.save_pretrained(self.output_dir)
        print(f'Done.')

    def checkpoint_path(self, model_path: str):
        return str(Path(model_path).joinpath("model.pt").absolute())

    def load_checkpoint(self, model_path: str):
        checkpoint_path = self.checkpoint_path(model_path)
        print(f"load sate dict from {checkpoint_path}")
        checkpoint = torch.load(checkpoint_path, weights_only=True)
        self.model.load_state_dict(checkpoint)
        return self.model

    def batch_encode(self, texts: List[str]):
        return self.tokenizer.batch_encode_plus(
            texts,
            max_length=self.config.max_position_embeddings,
            padding='max_length',
            truncation=True,
            return_tensors="pt",
        ).to(self.device)

    def classify(self, texts: List[str], score: float = 0.65):
        encoding = self.batch_encode(texts)
        predict = self.model(
            encoding['input_ids'],
            token_type_ids=encoding['token_type_ids'],
            attention_mask=encoding['attention_mask']
        ).cpu().detach().numpy().tolist()
        classify = []
        for index in range(len(texts)):
            labels = []
            for i, label in enumerate(self.labels):
                if predict[index][i] >= score:
                    labels.append(label)
            classify.append(labels)
        return predict, classify

    def predict(self):
        self.load_checkpoint(self.model_name_or_path).eval()
        score = self.args.get("classify_min_score", 0.65)
        print(f"Classify min_score: {score}")
        while True:
            text = input("Input: ")
            if not text:
                break
            predict, classify = self.classify([text], score=score)
            print(f"predict: {predict}")
            print(f"classify: {classify}")
        print(f"Bye!")
