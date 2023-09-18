import os
from transformers import pipeline
if os.path.exists('./data/sentiment-analysis'):
    classifier = pipeline('sentiment-analysis', './data/sentiment-analysis')
else:
    classifier = pipeline('sentiment-analysis')
    classifier.save_pretrained('./data/sentiment-analysis')
r = classifier('We are very happy to introduce pipeline to the transformers repository.')
print(r)
