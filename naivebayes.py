import pandas
from sklearn.naive_bayes import GaussianNB
from sklearn.svm import SVC
import  cPickle as  pickle
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
#print(model.predict([3,31,0.09090909090909091,0,1,0,0,0,-0.00144521609764]))

#save the trained naive bayes model as pickle format

f = open('C:/Users/pudutta/Desktop/Demo/Naiveclassifier.pickle', 'wb')
pickle.dump(model, f)
f.close()
