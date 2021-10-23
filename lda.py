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
from nltk.corpus import stopwords 
from nltk.stem.wordnet import WordNetLemmatizer
import string
import gensim
from gensim import corpora
from gensim.summarization import summarize
from gensim.summarization import keywords

stop = set(stopwords.words('english'))
exclude = set(string.punctuation) 
lemma = WordNetLemmatizer()

def clean(doc):
    stop_free = " ".join([i for i in doc.lower().split() if i not in stop])
    punc_free = ''.join(ch for ch in stop_free if ch not in exclude)
    normalized = " ".join(lemma.lemmatize(word) for word in punc_free.split())
    return normalized

  

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
    
doc_completes=corpus_raw.split("\r\n\r")
doc_clean = [clean(doc).split() for doc in doc_completes]   
#print(summarize(doc_completes[2]))
#print(keywords(doc_completes[2]))
