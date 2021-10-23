# -*- coding: utf-8 -*-
"""
Created on Tue Jun  6 16:51:00 2017

@author: pudutta
"""
import gensim
import gensim.models.word2vec as w2v
from gensim.models.keyedvectors import KeyedVectors
#model = KeyedVectors.load_word2vec_format('C:/Users/pudutta/Downloads/GoogleNews-vectors-negative300.bin.gz', binary=True) 
#model.save('C:/Users/pudutta/Downloads/GoogleNews-vectors')
model =gensim.models.KeyedVectors.load('C:/Users/pudutta/Downloads/GoogleNews-vectors')
#model.wv.most_similar(positive=['woman', 'king'], negative=['man'])
#model.build_vocab(new_sentences, update=True)
#model.train(new_sentences) 


