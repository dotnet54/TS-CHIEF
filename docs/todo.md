

we have shown the compytational complecities and we are just trying to show an indication...

-----planning-----
Improved File loading
CrossValidation
Experiment Repetition
Exporting Results
Bagging/Boosting
Multithreading at tree level
Evaluation
Serialization
Dynamic Options
--

Options options = new Options()
options.set("target_column", "last")
options.set("separator", ",")
options.set("file_type", "csv")
options.set("verbose", "true", dType.Boolean)


UTSDataset train = new UTSDataset(trainFileName, options)
UTSDataset test = new UTSDataset(testFileName, options)

train.save(fileName, options)

UTSDataset dataset = DataLoader.loadCsv(fileName, options)
UTSDataset dataset = DataLoader.loadArff(fileName, options)

UTSDataset dataset = DataLoader.Ucr2015.ItalyPowerDemand();



Options options = new TSChiefOptions()
options.set("k", 100)
options.set("threads", 0)
options.set("seed", 0)

TSChief model = new TSChief(options)
model.setParam("s", "ee:5") //updates the option, might invalidate the last fit

model.fit(train)

score = model.predict(test) //chainable

score.predictions()
score.accuracy()
score.recall()
score.stats()

model.save(fileName)

Classifier.load(model)
TSChief.load(model)


27-3-2020
normalization,
measure timing, 
m3 job, 
dimension correlation, 
progress prints
inception results, 
multivariate inception,
transpose data storage, 
latex write up, 
dm param support, 
multi thread knn, 
make sure that train and test both use same label encoder