# -*- coding: utf-8 -*-
"""
Created on Wed May 24 11:30:31 2017

@author: pudutta
"""
from __future__ import absolute_import, division, print_function
import nltk
from nltk.corpus import brown
from nltk.corpus import stopwords
import codecs
import glob
import multiprocessing
#import gensim
from gensim.models.keyedvectors import KeyedVectors
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
import sys

brown_train = brown.tagged_sents(categories=['news', 'government', 'reviews', 'Medicine'])
#pos tagger (brown)
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

#normal regex tagger
lmtzr = WordNetLemmatizer()

#gloveFile = 'C:/Users/pudutta/Desktop/gloves/glove2.txt'    
#def loadGloveModel(gloveFile):
    #print ("Loading Glove Model")
    #f = open(gloveFile,'r',encoding='utf-8')
    #model = {}
    #for line in f:
       # splitLine = line.split()
        #word = splitLine[0]
        #embedding = [float(val) for val in splitLine[1:]]
        #model[word] = embedding
    #print ("Done")
    #return model

# This is the glove vector representation. Uncomment to use instead of Word2vec

#nltk.download("stopwords")
#unigram_tagger = nltk.UnigramTagger(brown_train, backoff=regexp_tagger)

#bigram_tagger = nltk.BigramTagger(brown_train, backoff=unigram_tagger)


grammar= {}
grammar["NNP->NNP"] = "NNP"
#grammar["JJ->NN"] = "NN"
grammar["JJ->NNP"]="NNP"
grammar["NN->NN"] = "NNI"
grammar["NNI->NN"] = "NNI"
grammar["JJ->JJ"] = "JJ"
grammar["NNP->NN"]="NNP"
grammar["CD->NN"]="NN"
grammar["NN->CD"]="NNP"
grammar["NNP->CD"]="NNP"
grammar["CD->NNP"]="NNP"
#parameters for the word2vec model
#use this to train on the given document
#Instead use Google's pretrained word2vec and load w2v for out of vocabulary words
num_features = 300
min_word_count = 1
num_workers = multiprocessing.cpu_count()
context_size = 7
downsampling = 1e-3
seed=1

thrones2vec = w2v.Word2Vec(
        sg=1,
        seed=seed,
        workers=num_workers,
        size=num_features,
        min_count=min_word_count,
        window=context_size,
        sample=downsampling
        )
#initial grammar
#naive bias classifier will increase the probability accordingly.
useless=["WP", "WP$", "PRP", "PRP$", "DET", "WDT", "WRB", "PPS","PPSS", "DT", "C", "CS", "AT", "PP$", "HV"]
numbers=["one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"]
wh=["What", "Where", "How", "Which", "When", "Whose", "This", "That","A","Them", "Then", "Those", "They","don't",  "I", "Me", "My"]

all_word_vector_matrix=[]
tags=[]
class NPExtractor(object):
    
    def __init__(self, sentence):
        self.sentence = sentence

   
    def tokenize_sentence(self, sentence):
        
        clean = re.sub("[^a-zA-Z]"," ", sentence)
        words = clean.split()
        #tokens = nltk.word_tokenize(sentence)
       # tokens  = [w for w in tokens if not w in stopwords.words("english")]
        #print(tokens)
        return words

    
    def normalize_tags(self, tagged):
        
        n_tagged = []
        
        #print(tagged)
        for idx, t in enumerate(tagged):
            if(t[0][0].isupper() and t[1] not in useless and t[0] not in wh and idx!=len(tagged)-1 and (tagged[idx][1]=="NN" and (tagged[idx+1][1]!="NN"or tagged[idx+1][1]!="NNS" or tagged[idx+1][1]!="NNP") )  ):
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
        
        sentences=[]
        #tokens=self.tokenize_sentence(self.sentence)
        tokenizer = nltk.data.load('tokenizers/punkt/english.pickle')
        raw_sentences = tokenizer.tokenize(self.sentence)
        for raw_sentence in raw_sentences:
            if(len(raw_sentence)>0):
                tokens2=self.tokenize_sentence(raw_sentence)
                #print(tokens2)
                #print("\n")
                sentences.append(tokens2)
            
        
        
        #print(sentences)
        #print(tokens)
        #all_word_vectors_matrix = thrones2vec.wv.syn0
        #print(len(sentences))
        

        thrones2vec.build_vocab(sentences)
        thrones2vec.train(sentences, total_examples=thrones2vec.corpus_count, epochs=thrones2vec.iter)
        #thrones2vec.train(sentences)
       # model =gensim.models.KeyedVectors.load('C:/Users/pudutta/Downloads/GoogleNews-vectors')
       # print(".....................")
        #print(len(thrones2vec.wv.vocab))
       # for t in thrones2vec.wv.vocab:
            #print(thrones2vec.wv[t])
            #print(t)
            #print(len(thrones2vec.wv[t]))
            #print("\n")
        
        #tags = self.normalize_tags(bigram_tagger.tag())
        tags1=[]
        for idx, sentence in enumerate(sentences):
                tags1.append(self.normalize_tags(nltk.pos_tag(sentence)))
                #tags1.append(self.normalize_tags(bigram_tagger.tag(sentence)))
            
        tags=[]
        for sentence in tags1:
            for tagssx in sentence:
                tags.append(tagssx)
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
            lmtzr.lemmatize(word1)
            tags2.append((word1, tags[idx][1]))
        #print(tags2)    
        merge = True
        while merge:
            merge = False
            for x in range(0, len(tags) - 1):
                t1 = tags[x]
                #word1=t1[0]
                #word1=word1.lower()
                t2 = tags[x + 1]
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
                    tags.pop(x)
                    tags.pop(x)
                    matchx= (t1[0], t2[0])
                    pos = value
                    tags.insert(x, (matchx, pos))
                    #print((x, (match, pos)))
                    break
           
        matchesx = []
        for t in tags:
            #print(t)
            if t[1] == "NNP" or t[1]=="NNI":
                    #print(t[0])
                    #print(".....")
                    matchesx.append(t[0])
        return matchesx


#elements=[]
def getallelements( fd):
    #print(fd)
   
   
    if(len(fd)!=2):
       # print(fd)
        elements.append(fd) #dp
        #print("end")
        #print(fd)
        return
        
    else:
        #print(fd[1])
        if(len(fd[0])==1 and len(fd[1])==1):
            elements.append(fd)
        else:    
            elements.append(getallelements(fd[0]))
            elements.append(getallelements(fd[1]))
        


w22v=[]          
sumsimilarity=[]
el_tagged=[]
el_len=[]
elements=[]   
keyphrases=[]
fds=[]
def main():
    #filepath=sys.argv[1]
    #$corpus="""What is Adobe Experience Manager?
    corpus=""" Machine learning is the subfield of computer science that, according to Arthur Samuel in 1959, gives "computers the ability to learn without being explicitly programmed."[1] Evolved from the study of pattern recognition and computational learning theory in artificial intelligence,[2] machine learning explores the study and construction of algorithms that can learn from and make predictions on data[3] – such algorithms overcome following strictly static program instructions by making data-driven predictions or decisions,[4]:2 through building a model from sample inputs. Machine learning is employed in a range of computing tasks where designing and programming explicit algorithms with good performance is difficult or infeasible; example applications include email filtering, detection of network intruders or malicious insiders working towards a data breach,[5] optical character recognition (OCR),[6] learning to rank, and computer vision."""
#It’s the leading digital experience management solution that helps your business deliver compelling content across experiences — such as web, mobile, and the IOT — at the scale you need to build your brand and drive engagement.
#Content backbone, as agile as it is robust.
#Your customers and employees want each interaction with you to feel genuine. The right capabilities and workflows help you meet this expectation with efficiency and speed.
#What is Experience Manager Sites?
#It’s a content management system within Experience Manager that gives you one place to create, manage, and deliver digital experiences across websites, mobile sites, and on-site screens to make them global in reach, yet personally relevant and engaging.
#What is Adobe Experience Manager Assets?
#It's the 21st century digital asset management system that uniquely connects to existing creative workflows, providing distributed teams with a centralized location to mange and deliver engaging, channel optimized experiences across your customer’s journey.
#What is Experience Manager Forms?
#It's an Experience Manager capability that helps you make form and document processes paperless, efficient, and automated. It transforms digital enrollment, onboarding, and ongoing correspondence into simple, streamlined experiences.
#What is Adobe Experience Manager Communities?
#It’s an Experience Manager capability that helps you create online community experiences, including forums, user groups, learning resources, and other social features that are valuable to customers, employees, and your brand.
#What is Adobe Experience Manager Livefyre?
 
#It’s an all-new Experience Manager capability that lets you tap into everything shared on the web to create a constant flow of fresh and high-quality content on your own sites."""        
    #with codecs.open(filepath, "r", errors="ignore") as doc:
           # corpus=doc.read()
    
    
    #gloveFile = 'C:/Users/pudutta/Desktop/gloves/glove2.txt'  
   
   # model=loadGloveModel(gloveFile)
    #print(corpus)
    
    
    np_extractor = NPExtractor(corpus)
    result = np_extractor.extract()
  
   # print(result)
    fd=nltk.FreqDist(result)
    
    fds=sorted(fd.items(), key=lambda item: item[1],reverse=True)
    #print(fds)
    #for fd in fds:
        #print(fd)
        #print("\n")
   # print(elements)
    #thrones2vec.build_vocab(sentence)
    #print("........................................")
    #print(len(thrones2vec.wv.vocab))
    for idx,fd in enumerate(fds):
        getallelements(fd[0])
        while(None in elements):    
            elements.remove(None)
        #print(elements)    
        keyphrase= ""  
        # Have to handle for improper DITA dumping.
        if(len(elements)<5):
            for element in elements:
               # print(element)
                #print("\n")
                keyphrase+=element+ " "
            keyphrases.append(keyphrase)   
        elements[:] = []    
        #sum1=[0]*300
        #for idx, words in enumerate(elements):
           # try:
               # sum1=[sum(x) for x in zip(sum1, thrones2vec.wv[words] )]
            #except:
                #print(words)
        #for idx, x in enumerate(sum1):
           # sum1[idx]=sum1[idx]/len(elements)
                
            #print(elements)
        #tagss=[]
        #tagss = bigram_tagger.tag(elements)
        #for t in tagss:
           # el_tagged.append(t[1])
        #el_len.append(len(elements))    
           #print(tagss)
            #print(len(sum1))
       # w22v.append(( sum1))
      
        #print(w22v)
            #print(sum1)
        #print("\n")
        #del elements[:]
        #print(fd) 
       # print("\n")
            
       
    #w=len(w22v)
    #h=len(w22v)
    #similarity= [[0.00 for x in range(w)] for y in range(h)] 
    #hash=[[0 for x in range(w)] for y in range(h)] 
    #sumsimilarity=[0.00]*len(w22v)  
    
    #for idx,w1 in enumerate(w22v):
        #for jdx, w2 in enumerate(w22v):
            #if (idx!=jdx and hash[idx][jdx]==0):
                #similarity[idx][jdx]=1 - spatial.distance.cosine(w22v[idx], w22v[jdx])
               # hash[idx][jdx]=1
                #hash[jdx][idx]=1
               
   
    #print(w2v)
    
   
    #print(fds) 
    #print(len(fds))       
    #for idx, w1 in enumerate(w22v):
        #for jdx, w2 in enumerate(w22v):
            #sumsimilarity[idx]=(sumsimilarity[idx]+w22v[idx][jdx])
    #for id in w2v:
       # print(id)
       # print("\n") 
    #print(sumsimilarity)
   # print(len(sumsimilarity)) 
    #print(max(sumsimilarity))
    #print(thrones2vec.wv['sansa'])
    
#sumsimilarity
    for keyphrase in keyphrases:
        print(keyphrase)
        print(",")
        
    #print(el_tagged)
    #print(el_len)
    
                      
main()