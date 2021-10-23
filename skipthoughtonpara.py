# -*- coding: utf-8 -*-
"""
Created on Fri Jun 16 19:03:17 2017

@author: pudutta
"""

# -*- coding: utf-8 -*-

from __future__ import absolute_import, division, print_function
import codecs
import glob
import multiprocessing
import os
import pprint
import re
import nltk
import gensim.models.word2vec as w2v

import sklearn.manifold
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
import numpy as np
import string
a=np.loadtxt('C:/Users/pudutta/Downloads/skip-thoughts-master/skip-thoughts-master/testpara.txt', dtype=float)
#b=np.loadtxt('C:/Users/pudutta/Downloads/skip-thoughts-master/skip-thoughts-master/marketting.txt', dtype=float)
#a=a[:200]
#b=b[:600]
stop = set(stopwords.words('english'))
exclude = set(string.punctuation) 
lemma = WordNetLemmatizer()
def clean(doc):
    stop_free = " ".join([i for i in doc.lower().split() if i not in stop])
    punc_free = ''.join(ch for ch in stop_free if ch not in exclude)
    normalized = " ".join(lemma.lemmatize(word) for word in punc_free.split())
    return normalized
nltk.download("punkt")
nltk.download("stopwords")
lemmatizer = WordNetLemmatizer()


book_filenames = sorted(glob.glob("C:/Users/pudutta/Desktop/alldata-id.txt"))
print("Found books:")
print(book_filenames)


corpus_raw = u""
for book_filename in book_filenames:
    print("Reading '{0}'...".format(book_filename))
    with codecs.open(book_filename, "r", "utf-8", errors="ignore") as book_file:
        corpus_raw += book_file.read()
    print("Corpus is now {0} characters long".format(len(corpus_raw)))
    print()
    
    





lemmatizer = WordNetLemmatizer()

sentences=[]
    
doc_completes=corpus_raw.split("\r\n\r")
doc_clean = [clean(doc).split() for doc in doc_completes] 
sentences=[]
for doccleans in doc_clean:
    sent=u""
    for words in doccleans:
        sent+=words+u" "
    sentences.append(sent)


               


finalsent=[]
w=len(sentences)
h=len(sentences)
#similarity= [[0.00 for x in range(w)] for y in range(h)] 
hash=[[0 for x in range(w)] for y in range(h)] 
for idx, w1 in enumerate(sentences):
    for jdx, w2 in enumerate(sentences):
        if(idx!=jdx and hash[idx][jdx]==0):
            finalsent.append((w1, w2, 1 - spatial.distance.cosine(a[idx],a[jdx])))
            hash[idx][jdx]=1
            hash[jdx][idx]=1    
                    
                 
        
fds=sorted(finalsent,key=itemgetter(2), reverse=True)

#fds=sorted(finalsent.items(), key=lambda item: item[1],reverse=True)




workbook = xlsxwriter.Workbook('Desktop/Skipthoughtsonparagraphs.xlsx')
worksheet = workbook.add_worksheet()
worksheet.set_column(1, 1, 15)
bold = workbook.add_format({'bold': 1})
worksheet.write('A1', 'Sentence1(tech)', bold)
worksheet.write('B1', 'Sentence2(mark)', bold)
worksheet.write('C1', 'Similarity_Score', bold)
row = 1
col = 0
for Sentence1,Sentence2, Similarity_Score in (fds):
     # Convert the date string into a datetime object.
    
     worksheet.write_string(row, col,  Sentence1            )
     worksheet.write_string(row, col + 1, Sentence2 )
     worksheet.write_number(row, col + 2, Similarity_Score)
     row += 1
     
     
workbook.close()       