
from __future__ import absolute_import, division, print_function
import gensim
from gensim.models.doc2vec import TaggedDocument
from collections import namedtuple

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



# to tokenize the sentence
def tokenize_sentence( sentence):
        clean = re.sub("[^a-zA-Z0-9]"," ", sentence)
        words = clean.split()
        #tokens = nltk.word_tokenize(sentence)
       # tokens  = [w for w in tokens if not w in stopwords.words("english")]
        #print(tokens)
        return words 
    
SentimentDocument = namedtuple('SentimentDocument', 'words tags')
book_filenames = sorted(glob.glob("C:/Users/pudutta/Desktop/paragraph.txt"))
print("Found books:")
print(book_filenames)



corpus_raw = u""
for book_filename in book_filenames:
    print("Reading '{0}'...".format(book_filename))
    with codecs.open(book_filename, "r", "utf-8", errors="ignore") as book_file:
        corpus_raw += book_file.read()
    print("Corpus is now {0} characters long".format(len(corpus_raw)))
    print()
  # will hold all docs in original order
alldocs=[]
alldocs1=[]
alldocss=corpus_raw.split("\r\n\r")
for idx, docs in enumerate(alldocss):
    words=tokenize_sentence(docs)
    tags=[idx]
    alldocs1.append(docs)
    alldocs.append(SentimentDocument(words, tags))

print(alldocs)        
doc_list = alldocs[:] 



from gensim.models import Doc2Vec
import gensim.models.doc2vec
from collections import OrderedDict
import multiprocessing

cores = multiprocessing.cpu_count()

models = [
    # PV-DBOW 
    Doc2Vec(dm=0, dbow_words=1, size=100, window=4, min_count=1, iter=200, workers=cores),
    # PV-DM w/average
    Doc2Vec(dm=1, dm_mean=1, size=100, window=4, min_count=1, iter =200, workers=cores),
]
models[0].build_vocab(doc_list)
#print(str(models[0]))
models[1].build_vocab(doc_list)

#print(str(models[1]))

for model in models:
    model.train(doc_list)
    
import numpy as np
doc_id =0 # pick random doc; re-run cell for more examples
print('for doc %d...' % doc_id)
for model in models:
    inferred_docvec = model.infer_vector(alldocs[doc_id].words)
    print('%s:\n %s' % (model, model.docvecs.most_similar([inferred_docvec], topn=10)))