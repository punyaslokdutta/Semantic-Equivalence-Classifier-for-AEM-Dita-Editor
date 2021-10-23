import os
import pandas as pd
import nltk
import gensim
import scipy
from gensim import corpora, models, similarities

os.chdir("C:/Users/pudutta/Desktop");
df=pd.read_csv('battles.csv');



x=df['name'].values.tolist()
y=df['location'].values.tolist()

corpus= x+y
  
tok_corp= [nltk.word_tokenize(sent.decode('utf-8')) for sent in corpus]
       
           
model = gensim.models.Word2Vec(tok_corp, min_count=1, size = 32)

model.save('testmodel')
model = gensim.models.Word2Vec.load('test_model')
model.most_similar('Golden')
#model.most_similar([vector])