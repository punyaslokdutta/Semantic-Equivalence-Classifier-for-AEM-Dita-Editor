# -*- coding: utf-8 -*-
"""
Created on Fri May 19 12:20:09 2017

@author: pudutta
"""

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


nltk.download("punkt")
nltk.download("stopwords")


book_filenames = sorted(glob.glob("Desktop/alldata-id.txt"))
print("Found books:")
print(book_filenames)


corpus_raw = u""
for book_filename in book_filenames:
    print("Reading '{0}'...".format(book_filename))
    with codecs.open(book_filename, "r", "utf-8", errors="ignore") as book_file:
        corpus_raw += book_file.read()
    print("Corpus is now {0} characters long".format(len(corpus_raw)))
    print()
    
    
raw_sentences=[]    
tokenizer = nltk.data.load('tokenizers/punkt/english.pickle')
raw_sentences = tokenizer.tokenize(corpus_raw)
#print(raw_sentences[45])

lemmatizer = WordNetLemmatizer()
for idx, raw_sentence in enumerate(raw_sentences):
    raw_sentence=raw_sentence.lower()
    raw_sentences[idx]=raw_sentence
    
words=[]
def sentence_to_wordlist(raw):
    clean = re.sub("[^a-zA-Z]"," ", raw)
    words = clean.split()
    words = [w for w in words if not w in stopwords.words("english")]
    for w in words:
        lemmatizer.lemmatize(w)
       
    return words 



sentences = []
maprawsent=[]

for raw_sentence in raw_sentences:
    if (len(raw_sentence)>15):
        sentences.append(sentence_to_wordlist(raw_sentence))
        maprawsent.append((raw_sentence, sentence_to_wordlist(raw_sentence)))
        for sentence in sentences:
           for idx, w in enumerate(sentence):
             w=w.lower()
             sentence[idx]=w    
    else:
        raw_sentences.remove(raw_sentence)
        
#print(maprawsent[0])
#print(maprawsent[1])

#for idx, w in enumerate(maprawsent):
    #print(w)
    #print("\n")
    
    
    
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


model=loadGloveModel(gloveFile)
notfoundwords=[]
sumsent=[]
print(len(maprawsent))
w, h = 50 , len(maprawsent)
sumsent = [[0.00 for x in range(w)] for y in range(h)] 
for idx, maprawsen in enumerate(maprawsent):  
    for jdx, words in enumerate(maprawsen[1]):       
       if(words in model):          
           continue
       else:
           #print(words)
           #print(idx)
           notfoundwords.append(maprawsen)            
           break
   
    
#print(notfoundwords)
for ldx, words in enumerate(notfoundwords):
    maprawsent.remove(words)
        


       
#print(model['admin'])
#print(model['control'])
#print(model['map'])
#print(sumsent[1])
#print(maprawsent[1])
nonspecifiedlength=[]
for idx, maprawsen in enumerate(maprawsent):
   if((len(maprawsen[1])<4) or (len(maprawsen[1])>10)):
      nonspecifiedlength.append(maprawsen)
      #print(maprawsen)
 
for words in nonspecifiedlength:
    maprawsent.remove(words)   
    
for idx, maprawsen in enumerate(maprawsent):
   for jdx, words in enumerate(maprawsen[1]):
       sumsent[idx]=[sum(x) for x in zip(sumsent[idx], model[words] )]   
             
for idx, sent in enumerate(sumsent):
    for jdx,vec in enumerate(sent):
        if(idx< len(maprawsent)):
            sumsent[idx][jdx]=sumsent[idx][jdx]/len(maprawsent[idx][1])
    
print(len(maprawsent))

sumsent=sumsent[:len(maprawsent)]   
#print(sumsent)
print("\n")
#print(maprawsent)
  
w, h = len(maprawsent),len(maprawsent);
hash=[[0 for x in range(w)] for y in range(h)]  
similarity= [[0.00 for x in range(w)] for y in range(h)] 
for idx, maprawsen1 in enumerate(maprawsent):
        for jdx,maprawsent2 in enumerate(maprawsent):
            if(hash[idx][jdx]==0 and idx!=jdx):
                #print(sumsent[idx])
                #print(sumsent[jdx])
                similarity[idx][jdx]=1 - spatial.distance.cosine(sumsent[idx], sumsent[jdx])
                hash[idx][jdx]=1
                hash[jdx][idx]=1    
            else:
                similarity[idx][jdx]=0;
        
#print(len(notfoundwords))  
#for idx, notfound in enumerate(notfoundwords):
    #print(notfound)
   # print(idx)
   # print("\n")             
#print(len(maprawsent))  
#print(maprawsent[1][1])

#print(maprawsent)
#for idx, sent in enumerate(maprawsent):
    
   #print(idx)
   #print(sumsent[idx])
   #print(maprawsent[idx])

#print(len(sumsent))
#print(len(maprawsent))
#print(len(sumsent))
#print(len(notfoundwords))
#print(sumsent[220])
  
               
            
          
#print(similarity)

finalsent=[]      
for idx, w1 in enumerate(similarity):
    for jdx, w2 in enumerate(similarity):
        finalsent.append((similarity[idx][jdx], maprawsent[idx], maprawsent[jdx]))    


final=[]
final2=[]
final=sorted(finalsent,key=itemgetter(0), reverse=True)
for idx, sent in enumerate(final):
    #print(sent)
    final2.append((sent[1][0], sent[2][0], sent[0]))
  
#for idx, w in enumerate(final):
    #print(w)
    #print("\n")    
final2=final2[:5000]
workbook = xlsxwriter.Workbook('Sentence_Similarity.xlsx')
worksheet = workbook.add_worksheet()
worksheet.set_column(1, 1, 15)
bold = workbook.add_format({'bold': 1})
worksheet.write('A1', 'Sentence1', bold)
worksheet.write('B1', 'Sentence2', bold)
worksheet.write('C1', 'Similarity_Score', bold)
row = 1
col = 0
for Sentence1,Sentence2, Similarity_Score in (final2):
     # Convert the date string into a datetime object.
    
     worksheet.write_string(row, col,  Sentence1            )
     worksheet.write_string(row, col + 1, Sentence2 )
     worksheet.write_number(row, col + 2, Similarity_Score)
     row += 1
     
     
workbook.close()     

