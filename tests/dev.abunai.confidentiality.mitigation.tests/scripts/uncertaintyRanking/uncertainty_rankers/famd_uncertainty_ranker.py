from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.decomposition import FactorAnalysis
from sklearn.preprocessing import OneHotEncoder
import pandas as pd
import numpy as np

from collections import OrderedDict

from uncertainty_ranker import UncertaintyRanker

class FAMDUncertaintyRanker(UncertaintyRanker):

    def __init__(self, X, y):
        self.X = X
        self.y = y

    def evaluate(self):
        allData = pd.concat([self.X, self.y], axis=1)
        encoder = OneHotEncoder(sparse_output=False)
        constraint_one_hot = pd.DataFrame(encoder.fit_transform(allData[['Constraint violated']]), columns=encoder.get_feature_names_out())
        constraint_one_hot_vio = constraint_one_hot.drop(columns=['Constraint violated_False'])
        data = allData
        data = pd.concat([data, constraint_one_hot_vio], axis=1)
        data = data.drop(columns=['Constraint violated'])

        # No FAMD based ranking possible if just one data row exists so return all non irrelevant entries as ranking
        if data.shape[0] == 1:
            self.ranking = {}
            for name in data.columns.tolist():
                self.ranking[name] = 1
            return

        # Standardize data
        scaler = StandardScaler()
        data_standardized = scaler.fit_transform(data)

        # Additional PCA to determine amount of factors
        pca_full = PCA()
        pca_full.fit(data_standardized)
        
        # Choose number of factors
        eigenvalues = pca_full.explained_variance_
        n_factors = np.sum(eigenvalues > 1)

        fa = FactorAnalysis(n_components=n_factors, random_state=0)
        fa.fit(data_standardized)

        loadings = fa.components_.T
        column_names = data.columns.tolist()
        ranking = {}

        for i in range(len(loadings)):
            if column_names[i] == "Constraint violated_True":
                continue
            ranking[column_names[i]] = sum(abs(loadings[i]))

        self.ranking = OrderedDict(sorted(ranking.items(), key=lambda item: item[1], reverse=True))


    def show_ranking_with_correctness_score(self)-> list[(str,float)]:
        result = []
        for key, value in self.ranking.items():
            result.append((key,float(value)))
        return result