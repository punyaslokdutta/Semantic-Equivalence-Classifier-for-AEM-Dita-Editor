# -*- coding: utf-8 -*-
"""
Created on Sat May 27 18:00:35 2017

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
from nltk.stem.wordnet import WordNetLemmatizer
brown_train = brown.tagged_sents(categories=['news', 'government', 'reviews', 'Medicine'])

os.chdir("C:/Users/pudutta/Desktop/kea dataset")
titles=sorted(glob.glob("*justTitle.txt"))
docs= sorted(glob.glob("*.txt"))
for doc in docs:
    if(doc.endswith("justTitle.txt")):
        docs.remove(doc)
#print(docs)
#print(titles)
lemmatizer = WordNetLemmatizer()

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
                
    
    intersection=(commonwords)/(len(words_in_key)+len(words_in_title))/2
    return intersection
            
            
       
    
    


words2=[]
keywords=[]
tokenizer = nltk.data.load('tokenizers/punkt/english.pickle')
keys=sorted(glob.glob("*.key"))
for idx, key in enumerate(keys): 
    words = u""
    with codecs.open(key, "r", "utf-8", errors="ignore") as key:
         words+=key.read()
         words.rstrip().split('\n')
         #words.splitlines()
         keywords.append( words.split("\n"))
         #words.rstrip().split('\n')

         
         
        # print(words)
documents=[]         
for doc in docs:
     doci=u""
     with codecs.open(doc, "r", "utf-8", errors="ignore") as doc:
         doci+=doc.read()
         documents.append(doci)
         
justtitles=[]
for title in titles:
    titl=u""
    with codecs.open(title, "r", "utf-8", errors="ignore") as title:
         titl+=title.read()
         justtitles.append(titl)
#print(documents[0])

keywordsproperties=[]

for idx, doc in enumerate(documents):
    for jdx, keyxs in enumerate( keywords[idx]):
            print(keyxs)
            print("....................")
            print("\n")
            titleIntersection=getTitleIntersection(keyxs, justtitles[idx])
            #frequency=getFrequency()

    
     
    