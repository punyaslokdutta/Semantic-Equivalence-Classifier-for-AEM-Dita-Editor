# -*- coding: utf-8 -*-
"""
Created on Sat Jun 17 17:07:43 2017

@author: pudutta
"""

import os

import numpy as np
from sklearn.datasets import fetch_20newsgroups
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.cross_validation import train_test_split
from gensim.models.keyedvectors import KeyedVectors
from scipy import spatial
import codecs
import glob
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
import string
import nltk
from gensim.models.word2vec import Word2Vec
from operator import itemgetter
import xlsxwriter

lemma = WordNetLemmatizer()
nltk.download("punkt")
nltk.download("stopwords")
lemmatizer = WordNetLemmatizer()

stop = set(stopwords.words('english'))
exclude = set(string.punctuation) 

def clean(doc):
    stop_free = " ".join([i for i in doc.lower().split() if i not in stop])
    punc_free = ''.join(ch for ch in stop_free if ch not in exclude)
    normalized = " ".join(lemma.lemmatize(word) for word in punc_free.split())
    return normalized




if not os.path.exists("C:/Users/Punyaslok Dutta/Desktop/word embeddings/embed.dat"):
    print("Caching word embeddings in memmapped format...")
    model = KeyedVectors.load_word2vec_format(
        "C:/Users/Punyaslok Dutta/Desktop/word embeddings/GoogleNews-vectors-negative300.bin.gz",
        binary=True)
    fp = np.memmap("C:/Users/Punyaslok Dutta/Desktop/word embeddings/embed.dat", dtype=np.double, mode='w+', shape=model.wv.syn0norm.shape)
    fp[:] = model.wv.syn0norm[:]
#with open("C:/Users/pudutta/Desktop/embed.vocab", "w") as f:
    #for _, w in sorted((voc.index, word) for word, voc in model.wv.vocab.items()):
        #print(w, file=f)
#del fp, model.wv

W = np.memmap("C:/Users/Punyaslok Dutta/Desktop/word embeddings/embed.dat", dtype=np.double, mode="r", shape=(3000000, 300))
with open("C:/Users/Punyaslok Dutta/Desktop/word embeddings/embed.vocab") as f:
    vocab_list = map(str.strip, f.readlines())
    
vocab_dict = {w: k for k, w in enumerate(vocab_list)} 

book_filenames = sorted(glob.glob("C:/Users/Punyaslok Dutta/Desktop/paragraphs.txt"))
print("Found books:")
print(book_filenames)


corpus_raw = u""
for book_filename in book_filenames:
    print("Reading '{0}'...".format(book_filename))
    with codecs.open(book_filename, "r", "utf-8", errors="ignore") as book_file:
        corpus_raw += book_file.read()
    print("Corpus is now {0} characters long".format(len(corpus_raw)))
    print()


paragraphs=[]
    
doc_completes=corpus_raw.split("\r\n\r")
doc_clean = [clean(doc).split() for doc in doc_completes] 
for idx, doccleans in enumerate(doc_clean):
    sent=u""
    for words in doccleans:
        sent+=words+u" "
    paragraphs.append((sent, doc_completes[idx]))


finalpara=[]
w=len(paragraphs)
h=len(paragraphs)
#similarity= [[0.00 for x in range(w)] for y in range(h)] 
hash=[[0 for x in range(w)] for y in range(h)] 
for idx, w1 in enumerate(paragraphs):
    for jdx, w2 in enumerate(paragraphs):
        if(idx!=jdx and hash[idx][jdx]==0):
            vect = CountVectorizer(stop_words="english").fit([w1[0], w2[0]])
            print("Features:",  ", ".join(vect.get_feature_names()))
            v_1, v_2 = vect.transform([w1[0], w2[0]])
            v_1 = v_1.toarray().ravel()
            v_2 = v_2.toarray().ravel()
            finalpara.append((w1[1], w2[1], 1 - spatial.distance.cosine(v_1, v_2)))
            hash[idx][jdx]=1
            hash[jdx][idx]=1    
                    
                 
        
fds=sorted(finalpara,key=itemgetter(2), reverse=True)

#fds=sorted(finalsent.items(), key=lambda item: item[1],reverse=True)




#workbook = xlsxwriter.Workbook('Desktop/WMDonparawiki.xlsx')
#worksheet = workbook.add_worksheet()
#worksheet.set_column(1, 1, 15)
#bold = workbook.add_format({'bold': 1})
#worksheet.write('A1', 'PARA1', bold)
#worksheet.write('B1', 'PARA2', bold)
#worksheet.write('C1', 'Similarity_Score', bold)
#row = 1
#col = 0
#for Sentence1,Sentence2, Similarity_Score in (fds):
     # Convert the date string into a datetime object.
    
 #    worksheet.write_string(row, col,  Sentence1            )
   #  worksheet.write_string(row, col + 1, Sentence2 )
    # worksheet.write_number(row, col + 2, Similarity_Score)
     #row += 1
     
     
#workbook.close()       

#vect = CountVectorizer(stop_words="english").fit([d1, d2])
#print("Features:",  ", ".join(vect.get_feature_names()))


#from scipy.spatial.distance import cosine
#v_1, v_2 = vect.transform([d1, d2])
#v_1 = v_1.toarray().ravel()
#v_2 = v_2.toarray().ravel()
#print(v_1, v_2)
#print("cosine(doc_1, doc_2) = {:.2f}".format(cosine(v_1, v_2)))
#print(1 - spatial.distance.cosine(v_1, v_2))



