"""
填空
@Video(https://www.bilibili.com/video/BV1a44y1H7Jc)
@Code(https://github.com/lansinuote/Huggingface_Toturials/blob/main/8.%E4%B8%AD%E6%96%87%E5%A1%AB%E7%A9%BA.ipynb)
"""
import torch
from datasets import load_dataset
from transformers import BertTokenizer, BertModel, AdamW


class Dataset(torch.utils.data.Dataset):
    def __init__(self, split):
        dataset = load_dataset(path='lansinuote/ChnSentiCorp', split=split)

        def f(data):
            return len(data['text']) > 30

        self.dataset = dataset.filter(f)

    def __len__(self):
        return len(self.dataset)

    def __getitem__(self, i):
        text = self.dataset[i]['text']

        return text


class Model(torch.nn.Module):
    def __init__(self):
        super().__init__()
        self.decoder = torch.nn.Linear(768, token.vocab_size, bias=False)
        self.bias = torch.nn.Parameter(torch.zeros(token.vocab_size))
        self.decoder.bias = self.bias

    def forward(self, input_ids, attention_mask, token_type_ids):
        with torch.no_grad():
            out = pretrained(input_ids=input_ids,
                             attention_mask=attention_mask,
                             token_type_ids=token_type_ids)

        out = self.decoder(out.last_hidden_state[:, 15])

        return out


def collate_fn(data):
    data = token.batch_encode_plus(batch_text_or_text_pairs=data,
                                   truncation=True,
                                   padding='max_length',
                                   max_length=30,
                                   return_tensors='pt',
                                   return_length=True)
    # input_ids:编码之后的数字
    # attention_mask:是补零的位置是0,其他位置是1
    input_ids = data['input_ids'].to(device)
    attention_mask = data['attention_mask'].to(device)
    token_type_ids = data['token_type_ids'].to(device)
    # 把第15个词固定替换为mask
    labels = input_ids[:, 15].reshape(-1).clone().to(device)
    input_ids[:, 15] = token.get_vocab()[token.mask_token]
    # print(data['length'], data['length'].max())
    return input_ids, attention_mask, token_type_ids, labels


def test():
    model.eval()
    correct = 0
    total = 0

    loader_test = torch.utils.data.DataLoader(dataset=Dataset('test'),
                                              batch_size=32,
                                              collate_fn=collate_fn,
                                              shuffle=True,
                                              drop_last=True)

    for i, (input_ids, attention_mask, token_type_ids, labels) in enumerate(loader_test):

        if i == 15:
            break
        with torch.no_grad():
            out = model(input_ids=input_ids,
                        attention_mask=attention_mask,
                        token_type_ids=token_type_ids)
        out = out.argmax(dim=1)
        correct += (out == labels).sum().item()
        total += len(labels)
        print(token.decode(input_ids[0]))
        print(token.decode(labels[0]), token.decode(labels[0]))

    print(correct / total)


dataset = Dataset('train')
device = 'cuda' if torch.cuda.is_available() else 'cpu'
loader = torch.utils.data.DataLoader(dataset=dataset,
                                     batch_size=16,
                                     collate_fn=collate_fn,
                                     shuffle=True,
                                     drop_last=True)
token = BertTokenizer.from_pretrained('bert-base-chinese')
pretrained = BertModel.from_pretrained('bert-base-chinese')
pretrained.to(device)
model = Model()
model.to(device)
optimizer = AdamW(model.parameters(), lr=5e-4)
criterion = torch.nn.CrossEntropyLoss()

model.train()
for epoch in range(5):
    for i, (input_ids, attention_mask, token_type_ids,
            labels) in enumerate(loader):
        out = model(input_ids=input_ids,
                    attention_mask=attention_mask,
                    token_type_ids=token_type_ids)

        loss = criterion(out, labels)
        loss.backward()
        optimizer.step()
        optimizer.zero_grad()

        if i % 50 == 0:
            out = out.argmax(dim=1)
            accuracy = (out == labels).sum().item() / len(labels)

            print(epoch, i, loss.item(), accuracy)
test()
