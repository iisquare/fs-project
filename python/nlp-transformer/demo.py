import jieba
from keybert import KeyBERT

text = """
自然语言处理是一门博大精深的学科，掌握理论才能发挥出HanLP的全部性能。
《自然语言处理入门》是一本配套HanLP的NLP入门书，助你零起点上手自然语言处理。
"""
full_text = " ".join(jieba.cut(text))

model1 = KeyBERT('bert-base-chinese')
model2 = KeyBERT('paraphrase-multilingual-MiniLM-L12-v2')
print(model1.extract_keywords(text, keyphrase_ngram_range=(1, 1),  top_n=20, highlight=True))
print(model1.extract_keywords(full_text, keyphrase_ngram_range=(1, 1),  top_n=20, highlight=True))
print(model2.extract_keywords(text, keyphrase_ngram_range=(1, 1),  top_n=20, highlight=True))
print(model2.extract_keywords(full_text, keyphrase_ngram_range=(1, 1),  top_n=20, highlight=True))
