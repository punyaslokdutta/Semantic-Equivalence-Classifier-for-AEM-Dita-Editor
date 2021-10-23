import pandas
from pandas.tools.plotting import scatter_matrix
import matplotlib.pyplot as plt
from sklearn import cross_validation
from sklearn.metrics import classification_report
from sklearn.metrics import confusion_matrix
from sklearn.metrics import accuracy_score
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.naive_bayes import GaussianNB
from sklearn.svm import SVC
url="C:/Users/pudutta/Desktop/Demo/attributesfromMAUI.txt"
#loads the data from the source
names=[ 'frequency', 'distance', 'len_phrase','nnp','nn', 'jj', 'vb', 'cd', 'sumsimilarity'  'class']
dataset=pandas.read_csv(url)
array = dataset.values
X = array[:,:9]#indexing and slicing 
Y = array[:,9]
#validation_size = 0.33
#seed = 7
#X_train, X_validation, Y_train, Y_validation = cross_validation.train_test_split(X, Y, test_size=validation_size, random_state=seed)
#num_folds = 10
#num_instances = len(X_train)

#scoring = 'accuracy'
#models = []
#models.append(('NB', GaussianNB()))
#results = []
#names = []
#for name, model in models:
	#kfold = cross_validation.KFold(n=num_instances, n_folds=num_folds, random_state=seed)
	#cv_results = cross_validation.cross_val_score(model, X_train, Y_train, cv=kfold, scoring=scoring)
	#results.append(cv_results)
	#names.append(name)
	#msg = "%s: %f (%f)" % (name, cv_results.mean(), cv_results.std())
	#print(msg)
#fig = plt.figure()
#fig.suptitle('Algorithm Comparison')
#ax = fig.add_subplot(111)
#plt.boxplot(results)
#ax.set_xticklabels(names)
#plt.show()
model = GaussianNB()
model.fit(X,Y)
 #y_pred_gnb = gnb.fit(X_train, y_train).predict(X_test)

#ytest=model.fit(X, Y).predict([0,-1,0.0625,0,0,0,0,0,0.0946903880686])
#   predicted= model.predict([0,-1,0.0625,0,0,0,0,0,0.0946903880686])
#print (predicted)
print(model.predict([0,-1,0.0625,1,1,0,0,0,0.162468416616]))