# -*- coding: utf-8 -*-
"""
Created on Tue Jun 27 12:35:21 2017

@author: pudutta
"""

# -*- coding: utf-8 -*-


from __future__ import absolute_import, division, print_function
import nltk
import gensim
from nltk.corpus import brown
from nltk.corpus import stopwords
from nltk.tag import StanfordNERTagger
import codecs
import glob
from gensim.models.keyedvectors import KeyedVectors
import multiprocessing
import os
import random
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
from nltk.stem.wordnet import WordNetLemmatizer
import cPickle as pickle
import sys



keywords=[]
keyproperties=[]
#print(docs)
#print(titles)
lemmatizer = WordNetLemmatizer()
num_features = 300
min_word_count = 1
num_workers = multiprocessing.cpu_count()
context_size = 5
downsampling = 1e-3
seed=1

#thrones2vec=gensim.models.Word2Vec( size=300, window=5, min_count=1, workers=num_workers, seed=seed, sg=1,sample=downsampling)
thrones2vec = w2v.Word2Vec(
        sg=1,
        seed=seed,
        workers=num_workers,
        size=num_features,
        min_count=min_word_count,
        window=context_size,
        sample=downsampling
        )
words2=[]
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

#unigram_tagger = nltk.UnigramTagger(brown_train, backoff=regexp_tagger)
#bigram_tagger = nltk.BigramTagger(brown_train, backoff=unigram_tagger)
tokenizer = nltk.data.load('tokenizers/punkt/english.pickle')



def tokenize_sentence( keys):
        cleansx = re.sub("[^a-zA-Z0-9]"," ", keys)
        wordsx = cleansx.split()
        #print(wordsx)
        #tokens = nltk.word_tokenize(sentence)
       # tokens  = [w for w in tokens if not w in stopwords.words("english")]
        #print(tokens)
        return wordsx    
def sent_to_wordlist(raw):
    clean = re.sub("[^a-zA-Z]"," ", raw)
    words = clean.split()
    words = [w for w in words if not w in stopwords.words("english")]
    for w in words:
        w=w.lower()
        lemmatizer.lemmatize(w)
       
    return words 
         
def getTitleIntersection(key, title):
    words_in_key=sent_to_wordlist(key)
    words_in_title=sent_to_wordlist(title)
    commonwords=0
    for wik in words_in_key:
        for wit in words_in_title:
            if(wik==wit):
                commonwords=commonwords+1
                
    
    intersection=(commonwords)/((len(words_in_key)+len(words_in_title))/2)
    return intersection
         

def getFrequency(key, doc):
    nStr = doc
    pattern = key
    #print(pattern)
    #print(nStr)
    count =0
    flag=True
    start=0
    while flag:
        a = nStr.find(pattern,start)  # find() returns -1 if the word is not found, 
                                      #start i the starting index from the search starts(default value is 0)
        if a==-1:          #if pattern not found set flag to False
            flag=False
        else:               # if word is found increase count and set starting index to a+1
            count+=1        
            start=a+1
    #print(count)        
    return count
        
   


def getDistPhrase(key, doc):
    return doc.find(key)
        # print(words)
def getposseq(key):
    words=tokenize_sentence(key)
    tags =nltk.pos_tag(words)
    posarray=[0]*5
    #cd, vb*, jj*, nn*, nnp
    for tag in tags:
        if(tag[1]=="NNP"):
            posarray[0]=1
        if(tag[1].startswith("NN") and tag[1]!="NNP"):
            posarray[1]=1
        if(tag[1].startswith("JJ")):
            posarray[2]=1
        if(tag[1].startswith("VB")):
            posarray[3]=1
        if(tag[1]=="CD"):
            posarray[4]=1                                
    
    return posarray    
            
tokenizer = nltk.data.load('tokenizers/punkt/english.pickle') 
#broken function       
def getSumsimilarity( keywords):
    w22v=[]
    words_in_key=[]
   
    for keys in keywords:
        #print(keys)
        #print("\n")
        words_in_key=tokenize_sentence(keys)
            #print(words_in_key)
        
        #print(len(words_in_key))
        sum1=[0]*300
        if(len(words_in_key)!=0):
            for idx, words in enumerate(words_in_key):
                    sum1=[sum(x) for x in zip(sum1, thrones2vec[words] )]
               
                    #print(keys)
                      # keywords[ldx].remove(keys)
                    
            for idx, x in enumerate(sum1):
                    sum1[idx]=sum1[idx]/len(words_in_key)
                        #keywords.remove(keys)
                        #print(keys)
            #print(sum1)            
            w22v.append(( sum1))    
    w=len(w22v)
    h=len(w22v)
    similarity= [[0.00 for x in range(w)] for y in range(h)] 
    hash=[[0 for x in range(w)] for y in range(h)] 
    sumsimilarity=[0.00]*len(w22v)     
    
    for idx,w1 in enumerate(w22v):
        for jdx, w2 in enumerate(w22v):
            if (idx!=jdx and hash[idx][jdx]==0):
                similarity[idx][jdx]=1 - spatial.distance.cosine(w22v[idx],w22v[jdx])
                hash[idx][jdx]=1
                hash[jdx][idx]=1
            else:
                similarity[idx][jdx]=1
                hash[idx][jdx]=1    
    for idx, w1 in enumerate(w22v):
        for jdx, w2 in enumerate(w22v):
            try:
                sumsimilarity[idx]=sumsimilarity[idx]+w22v[idx][jdx]
            except:
                print("not found")

    return sumsimilarity        


def prepareThrones2vec(corpus):
    #print("Entered prepareThrones2Vec\n")
    #model =gensim.models.KeyedVectors.load('C:/Users/pudutta/Downloads/GoogleNews-vectors')
    #print("Passed keyvectors load\n")

    sentences=[]
    raw_sentences = tokenizer.tokenize(corpus)
    for raw_sentence in raw_sentences:
        if(len(raw_sentence)>0):
            tokens2=tokenize_sentence(raw_sentence)
                #print(tokens2)
                #print("\n")
            sentences.append(tokens2)
    #thrones2vec=gensim.models.Word2Vec(sentences, size=300, window=5, min_count=1, workers=num_workers, seed=seed, sg=1,sample=downsampling)
    #thrones2vec.wv=model
    thrones2vec.build_vocab(sentences)
    thrones2vec.train(sentences)
    #thrones2vec.train(sentences, total_examples=thrones2vec.corpus_count, epochs=thrones2vec.iter)
    

sumsimilarityxx=[]    
    
def main():        
           
    doc="C:/Users/pudutta/Desktop/Demo/DitaOut2.txt"  
    doci="" 
    
    with codecs.open(doc, "r", errors="ignore") as doc:
             doci=doc.read()
             #documents.append(doci)
             
    #print(doci)
    #for title in titles:
        #titl=u""
        #with codecs.open(title, "r", "utf-8", errors="ignore") as title:
             #titl+=title.read()
             #justtitles.append(titl)
    
   # keyfornonkey=[]
    words=""
    keywordslist=[]
    keys= "C:/Users/pudutta/Desktop/Demo/CandidateKeywords.txt"
    with codecs.open(keys, "r",  errors="ignore") as key:
            words+=key.read()
            words.rstrip().split('\n')
            keywords.append(words.split("\n"))
            keywordslist=keywords[0]
        
         #words.rstrip().split('\n')         
    #print(documents[0])
    #print(keywords[0])
    
    
                #print(frequency)
   
    #keywords.remove(keywords[len(keywords)-1])  
    #print(keywords)          
    prepareThrones2vec(doci)
    
    
    sumsimilarity=[]           
        #print(doc)
    keywordslist=keywordslist[:len(keywordslist)-1]
    #print(keywordslist)
   # print(keywordslist)
    sumsimilarity=getSumsimilarity(keywordslist)
    #print(sumsimilarity)
        
        #print(sumsimilarity)
        #print("\n")
        #print(len(sumsimilarity))
    for  idx, keyxs in  enumerate(keywordslist):
                
                #if(keyxs==''):
                     #jdx=jdx-1
                     #keywords[idx].remove(keyxs)
                   # print(idx)
                   # print(keyxs[0])
                    
                #print("....................")
                #print("\n")
                
                    #titleIntersection=getTitleIntersection(keyxs, justtitles[idx])
                    #print(keyxs)
                    frequency=getFrequency(keyxs, doci)
                    dist_phrase=getDistPhrase(keyxs, doci)
                    len_phrase=1/len(keyxs)
                    posseq=getposseq(keyxs)
                    nnp=posseq[0]
                    nn=posseq[1]
                    jj=posseq[2]
                    vb=posseq[3]
                    cd=posseq[4]
                    
                    #postag
                    #print(keyxs)
                   #print(jdx)
                    keyproperties.append((frequency, dist_phrase, len_phrase,nnp, nn, jj, vb, cd,sumsimilarity[idx]))
    f = open('C:/Users/pudutta/Desktop/Demo/Naiveclassifier.pickle', 'rb')
    classifier = pickle.load(f)
    for idx, candidatekeyword in enumerate(keyproperties):
        if(classifier.predict([candidatekeyword])==1):
            file=open("C:/Users/pudutta/Desktop/Demo/FilteredKeywords.txt", "w")    
            file.write(str(keywordslist[idx]))
            print(keywordslist[idx])
            print(",")
            file.write("\n")    
        else:
            file=open("C:/Users/pudutta/Desktop/Demo/NonKeywords.txt", "w")
           # print(keywordslist[idx])
            file.write(str(keywordslist[idx]))
            #print(keywordslist[idx])
            file.write(", ")
               
        #print(len(keyproperties)) 
        
    
main()     

#(NN|NNS|NNP|NNPS|JJ)*(NN|NNS|NNP|NNPS|VBG)    