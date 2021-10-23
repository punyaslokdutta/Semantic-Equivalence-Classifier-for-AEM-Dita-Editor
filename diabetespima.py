# -*- coding: utf-8 -*-
"""
Created on Fri Jun  2 14:38:24 2017

@author: pudutta
"""

# read the data into a Pandas DataFrame
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.naive_bayes import GaussianNB
from sklearn import metrics
url = 'C:/Users/pudutta/Desktop/keypropwiki 2.txt'
col_names = [ 'frequency', 'dist_phrase', 'len_phrase','nnp', 'nn', 'jj', 'vb', 'cd', 'sum' , 'label']
pima = pd.read_csv(url, header=None, names=col_names)
feature_cols = ['frequency', 'dist_phrase', 'len_phrase', 'nnp', 'nn', 'jj', 'vb','sum',  'cd']
X = pima[feature_cols]
y = pima.label
from sklearn.cross_validation import train_test_split
X_train, X_test, y_train, y_test = train_test_split(X, y, random_state=0)
#logreg = LogisticRegression()
#logreg.fit(X_train, y_train)
gnb=GaussianNB()
gnb.fit(X_train, y_train)
y_pred_class = gnb.predict(X_test)
print(metrics.accuracy_score(y_test, y_pred_class))

y_test.value_counts()
print(metrics.confusion_matrix(y_test, y_pred_class))
confusion = metrics.confusion_matrix(y_test, y_pred_class)
TP = confusion[1, 1]
TN = confusion[0, 0]
FP = confusion[0, 1]
FN = confusion[1, 0]
print((TP + TN) / float(TP + TN + FP + FN))
#print(metrics.accuracy_score(y_test, y_pred_class))#classification accuracy
#print(TP / float(TP + FN))
#print(metrics.recall_score(y_test, y_pred_class))#sensitivity
#print(TN / float(TN + FP))#specificity
print(gnb.predict(X_test)[0:10])
gnb.predict_proba(X_test)[0:10, :]
import matplotlib.pyplot as plt
y_pred_prob = gnb.predict_proba(X_test)[:, 1]
plt.rcParams['font.size'] = 14
from sklearn.preprocessing import binarize
y_pred_class = binarize([y_pred_prob], 0.3)[0]
from sklearn.cross_validation import cross_val_score
cross_val_score(gnb, X, y, cv=10, scoring='roc_auc').mean()            