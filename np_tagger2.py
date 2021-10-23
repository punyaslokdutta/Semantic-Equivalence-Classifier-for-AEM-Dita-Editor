# -*- coding: utf-8 -*-
"""
Created on Wed May 24 11:30:31 2017

@author: pudutta
"""
from __future__ import absolute_import, division, print_function
import nltk
from nltk.corpus import brown
from nltk.corpus import stopwords
from nltk.tag import StanfordNERTagger
import codecs
import glob
import multiprocessing
import os
import pprint
import re
import nltk
import gensim.models.word2vec as w2v
import math
import sklearn.manifold
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
from itertools import chain 
from scipy import spatial
from collections import Counter
from operator import itemgetter
from xlwt import Workbook
import xlsxwriter
from openpyxl import load_workbook
from openpyxl.compat import range
from openpyxl.utils import get_column_letter
from openpyxl import Workbook
brown_train = brown.tagged_sents(categories=['news', 'government', 'reviews', 'Medicine'])
regexp_tagger = nltk.RegexpTagger(
    [(r'^-?[0-9]+(.[0-9]+)?$', 'CD'),
     (r'(-|:|;)$', ':'),
     (r'\'*$', 'MD'),
     (r'(The|the|A|a|An|an)$', 'AT'),
     (r'.*able$', 'JJ'),
     (r'.*ness$', 'NN'),
     (r'^[A-Z].*$', 'NNP'),
     (r'.*ly$', 'RB'),
     (r'.*s$', 'NNS'),
     (r'.*ing$', 'VBG'),
     (r'.*', 'NN'),
     (r'.*ed$', 'VBD'),
     
     
])

gloveFile = 'C:/Users/pudutta/Desktop/gloves/glove2.txt'    
def loadGloveModel(gloveFile):
    print ("Loading Glove Model")
    f = open(gloveFile,'r',encoding='utf-8')
    model = {}
    for line in f:
        splitLine = line.split()
        word = splitLine[0]
        embedding = [float(val) for val in splitLine[1:]]
        model[word] = embedding
    print ("Done")
    return model



nltk.download("stopwords")
unigram_tagger = nltk.UnigramTagger(brown_train, backoff=regexp_tagger)
bigram_tagger = nltk.BigramTagger(brown_train, backoff=unigram_tagger)


grammar= {}
grammar["NNP->NNP"] = "NNP"
grammar["JJ->NN"] = "NNP"
grammar["JJ->NNP"]="NNP"
grammar["NN->NN"] = "NNI"
grammar["NNI->NN"] = "NNI"
grammar["JJ->JJ"] = "JJ"
grammar["NNP->NN"]="NNP"
grammar["CD->NN"]="NNP"
grammar["NN->CD"]="NNP"
grammar["NNP->CD"]="NNP"
grammar["CD->NNP"]="NNP"
#parameters for the word2vec model
num_features = 300
min_word_count = 3
num_workers = multiprocessing.cpu_count()
context_size = 7
downsampling = 1e-3
seed=1
#initial grammar
#naive bias classifier will increase the probability accordingly.
useless=["WP", "WP$", "PRP", "PRP$", "DET", "WDT", "WRB", "PPS","PPSS", "DT"]
numbers=["one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"]
wh=["What", "Where", "How", "Which", "When", "Whose", "This", "That", "Them", "Then", "Those", "They", "I", "Me", "My"]

all_word_vector_matrix=[]
class NPExtractor(object):

    def __init__(self, sentence):
        self.sentence = sentence

   
    def tokenize_sentence(self, sentence):
        tokens = nltk.word_tokenize(sentence)
       # tokens  = [w for w in tokens if not w in stopwords.words("english")]
        #print(tokens)
        return tokens

    
    def normalize_tags(self, tagged):
        n_tagged = []
        
        #print(tagged)
        for idx, t in enumerate(tagged):
            if(t[0][0].isupper() and t[0] not in useless and t[0] not in wh and (tagged[idx][1]=="NN" and (tagged[idx+1][1]!="NN"or tagged[idx+1][1]!="NNS" or tagged[idx+1][1]!="NNP") )  ):
                #print(t[0])
                #print(t[1])
                n_tagged.append((t[0],"NNP"))
            else:
               
                if(t[0].endswith("n't")):
                    n_tagged.append((t[0], "DT"))
                    continue
                if t[1] == "NP-TL" or t[1] == "NP":
                    n_tagged.append((t[0], "NNP"))
                    continue
                if t[1].endswith("-TL"):
                    n_tagged.append((t[0], t[1][:-3]))
                    continue
                if t[1].endswith("S"):
                    n_tagged.append((t[0], t[1][:-1]))
                    continue
                n_tagged.append((t[0], t[1]))
                #print(n_tagged)   
        return n_tagged

 
    def extract(self):
        
        tokens=self.tokenize_sentence(self.sentence)

        #print(tokens)
        #all_word_vectors_matrix = thrones2vec.wv.syn0
        
        thrones2vec = w2v.Word2Vec(
        sg=1,
        seed=seed,
        workers=num_workers,
        size=num_features,
        min_count=min_word_count,
        window=context_size,
        sample=downsampling
        )
        tokens2=[]
        for token in tokens:
            tokens2.append(token.lower())

        thrones2vec.build_vocab(tokens2)
        print(".....................")
        print(len(thrones2vec.wv.vocab))
        tags = self.normalize_tags(bigram_tagger.tag(tokens))
        #print(tags)
        #namedEnt = nltk.ne_chunk(tags, binary=False)
        #print(namedEnt)
        #namedEnt.draw()
        #st = StanfordNERTagger('/usr/share/stanford-ner/classifiers/english.all.3class.distsim.crf.ser.gz',
                       #'/usr/share/stanford-ner/stanford-ner.jar',
                      # encoding='utf-8')
       # classified_text = st.tag(tokens)
        #print(classified_text)
        tags2=[]
        for idx, tag in enumerate(tags):
            word1=tags[idx][0]
            #print(word1)
            word1=word1.lower()
            tags2.append((word1, tags[idx][1]))
        #print(tags2)    
        merge = True
        while merge:
            merge = False
            for x in range(0, len(tags2) - 1):
                t1 = tags2[x]
                #word1=t1[0]
                #word1=word1.lower()
                t2 = tags2[x + 1]
                #word2=t2[0]
                #word2=word2.lower()
                #t2[0]=t2[0].lower()
                #print(t1[0])
                #print(t2[0])
                key = "%s->%s" %(t1[1], t2[1])
                #print(key)
                value = grammar.get(key, '')
                if value:
                    merge = True
                    tags2.pop(x)
                    tags2.pop(x)
                    match = (t1[0], t2[0])
                    pos = value
                    tags2.insert(x, (match, pos))
                    #print((x, (match, pos)))
                    break
           
        matches = []
        for t in tags2:
            if t[1] == "NNP" or t[1]=="NNI":
                    #print(t[0])
                    #print(".....")
                    matches.append(t[0])
        return matches


elements=[]
def getallelements( fd):
    #print(fd)
   
    if(len(fd)!=2):
       # print(fd)
        elements.append(fd)
        #print("end")
        #print(fd)
        return
        
    else:
        #print(fd[1])
        elements.append(getallelements(fd[0]))
        elements.append(getallelements(fd[1]))
        


        

    
def main():
    sentence = """ 
Keyword extraction attracts much attention for its
significant role in various natural language processing
tasks. While some existing methods for keyword
extraction have considered using single type
of semantic relatedness between words or inherent
attributes of words, almost all of them ignore two
important issues: 1) how to fuse multiple types of
semantic relations between words into a uniform
semantic measurement and automatically learn the
weights of the edges between the words in the word
graph of each document, and 2) how to integrate
the relations between words and words’ intrinsic
features into a unified model. In this work, we
tackle the two issues based on the supervised random
walk model. We propose a supervised ranking
based method for keyword extraction, which
is called SEAFARER1
. It can not only automatically
learn the weights of the edges in the unified
graph of each document which includes multiple
semantic relations but also combine the merits of
semantic relations of edges and intrinsic attributes
of nodes together. We conducted extensive experimental
study on an established benchmark and the
experimental results demonstrate that SEAFARER
outperforms the state-of-the-art supervised and unsupervised
methods.
  """
    w2v=[]  
    gloveFile = 'C:/Users/pudutta/Desktop/gloves/glove2.txt'  
   
    model=loadGloveModel(gloveFile)
    np_extractor = NPExtractor(sentence)
    result = np_extractor.extract()
    fd=nltk.FreqDist(result)
    fds=sorted(fd.items(), key=lambda item: item[1],reverse=True)
    #thrones2vec.build_vocab(sentence)
    #print("........................................")
    #print(len(thrones2vec.wv.vocab))
    for fd in fds:
        #print(fd)
        getallelements(fd[0])
        while(None in elements):    
            elements.remove(None)
        
        sum1=[0]*50
        for idx, words in enumerate(elements):
            try:
                sum1=[sum(x) for x in zip(sum1, model[words] )]
            except:
                print(words)
        for idx, x in enumerate(sum1):
            sum1[idx]=sum1[idx]/len(elements)
        print(elements)
        #print(len(sum1))
        w2v.append(( sum1))
        del elements[:]
        #print(fd) 
        print("\n")
        
   
    w=len(w2v)
    h=len(w2v)
    similarity= [[0.00 for x in range(w)] for y in range(h)] 
    hash=[[0 for x in range(w)] for y in range(h)] 
    sumsimilarity=[0.00]*len(w2v)     
    
    for idx,w1 in enumerate(w2v):
        for jdx, w2 in enumerate(w2v):
            if (idx!=jdx and hash[idx][jdx]==0):
                similarity[idx][jdx]=1 - spatial.distance.cosine(w2v[idx], w2v[jdx])
                hash[idx][jdx]=1
                hash[jdx][idx]=1
               
   
    #print(w2v)
    
   
    #print(fds) 
    print(len(fds))       
    for idx, w1 in enumerate(w2v):
        for jdx, w2 in enumerate(w2v):
            sumsimilarity[idx]=sumsimilarity[idx]+w2v[idx][jdx]
    #for id in w2v:
       # print(id)
       # print("\n") 
    print(sumsimilarity)
    print(len(sumsimilarity))                     
main()