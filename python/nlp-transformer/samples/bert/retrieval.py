"""
检索机器人
@Video(https://www.bilibili.com/video/BV1Lh4y117KJ/)
@Code(https://github.com/zyds/transformers-code/blob/master/02-NLP%20Tasks/13-retrieval_chatbot/retrieval_bot.ipynb)
"""
from typing import Optional

import faiss
import pandas as pd
import torch
from torch.nn import CosineSimilarity, CosineEmbeddingLoss
from tqdm import tqdm
from transformers import AutoTokenizer
from transformers import BertPreTrainedModel, BertModel
from transformers.configuration_utils import PretrainedConfig


class DualModel(BertPreTrainedModel):

    def __init__(self, config: PretrainedConfig, *inputs, **kwargs):
        super().__init__(config, *inputs, **kwargs)
        self.bert = BertModel(config)
        self.post_init()

    def forward(
        self,
        input_ids: Optional[torch.Tensor] = None,
        attention_mask: Optional[torch.Tensor] = None,
        token_type_ids: Optional[torch.Tensor] = None,
        position_ids: Optional[torch.Tensor] = None,
        head_mask: Optional[torch.Tensor] = None,
        inputs_embeds: Optional[torch.Tensor] = None,
        labels: Optional[torch.Tensor] = None,
        output_attentions: Optional[bool] = None,
        output_hidden_states: Optional[bool] = None,
        return_dict: Optional[bool] = None,
    ):
        return_dict = return_dict if return_dict is not None else self.config.use_return_dict

        # Step1 分别获取sentenceA 和 sentenceB的输入
        senA_input_ids, senB_input_ids = input_ids[:, 0], input_ids[:, 1]
        senA_attention_mask, senB_attention_mask = attention_mask[:, 0], attention_mask[:, 1]
        senA_token_type_ids, senB_token_type_ids = token_type_ids[:, 0], token_type_ids[:, 1]

        # Step2 分别获取sentenceA 和 sentenceB的向量表示
        senA_outputs = self.bert(
            senA_input_ids,
            attention_mask=senA_attention_mask,
            token_type_ids=senA_token_type_ids,
            position_ids=position_ids,
            head_mask=head_mask,
            inputs_embeds=inputs_embeds,
            output_attentions=output_attentions,
            output_hidden_states=output_hidden_states,
            return_dict=return_dict,
        )

        senA_pooled_output = senA_outputs[1]    # [batch, hidden]

        senB_outputs = self.bert(
            senB_input_ids,
            attention_mask=senB_attention_mask,
            token_type_ids=senB_token_type_ids,
            position_ids=position_ids,
            head_mask=head_mask,
            inputs_embeds=inputs_embeds,
            output_attentions=output_attentions,
            output_hidden_states=output_hidden_states,
            return_dict=return_dict,
        )

        senB_pooled_output = senB_outputs[1]    # [batch, hidden]

        # step3 计算相似度

        cos = CosineSimilarity()(senA_pooled_output, senB_pooled_output)    # [batch, ]

        # step4 计算loss

        loss = None
        if labels is not None:
            loss_fct = CosineEmbeddingLoss(0.3)
            loss = loss_fct(senA_pooled_output, senB_pooled_output, labels)

        output = (cos,)
        return ((loss,) + output) if loss is not None else output


# title,reply
data = pd.read_csv("../../data/single/law_faq.csv")

dual_model = DualModel.from_pretrained("../../data/dual_model/checkpoint-250/")
dual_model = dual_model.cuda()
dual_model.eval()
tokenzier = AutoTokenizer.from_pretrained("hfl/chinese-macbert-base")

questions = data["title"].to_list()
vectors = []
with torch.inference_mode():
    for i in tqdm(range(0, len(questions), 32)):
        batch_sens = questions[i: i + 32]
        inputs = tokenzier(batch_sens, return_tensors="pt", padding=True, max_length=128, truncation=True)
        inputs = {k: v.to(dual_model.device) for k, v in inputs.items()}
        vector = dual_model.bert(**inputs)[1]
        vectors.append(vector)
vectors = torch.concat(vectors, dim=0).cpu().numpy()

index = faiss.IndexFlatIP(768)
faiss.normalize_L2(vectors)
index.add(vectors)

quesiton = "寻衅滋事"
with torch.inference_mode():
    inputs = tokenzier(quesiton, return_tensors="pt", padding=True, max_length=128, truncation=True)
    inputs = {k: v.to(dual_model.device) for k, v in inputs.items()}
    vector = dual_model.bert(**inputs)[1]
    q_vector = vector.cpu().numpy()
faiss.normalize_L2(q_vector)
scores, indexes = index.search(q_vector, 10)
topk_result = data.values[indexes[0].tolist()]
print(topk_result)

from transformers import BertForSequenceClassification
corss_model = BertForSequenceClassification.from_pretrained("../../data/cross_model/checkpoint-750/")
corss_model = corss_model.cuda()
corss_model.eval()

canidate = topk_result[:, 0].tolist()
ques = [quesiton] * len(canidate)
inputs = tokenzier(ques, canidate, return_tensors="pt", padding=True, max_length=128, truncation=True)
inputs = {k: v.to(corss_model.device) for k, v in inputs.items()}
with torch.inference_mode():
    logits = corss_model(**inputs).logits.squeeze()
    result = torch.argmax(logits, dim=-1)
canidate_answer = topk_result[:, 1].tolist()
match_quesiton = canidate[result.item()]
final_answer = canidate_answer[result.item()]
print(match_quesiton, final_answer)
