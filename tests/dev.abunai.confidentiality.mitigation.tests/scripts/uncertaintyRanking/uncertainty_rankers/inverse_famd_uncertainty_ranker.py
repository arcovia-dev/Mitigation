from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.decomposition import FactorAnalysis
from sklearn.manifold import TSNE
import pandas as pd
import numpy as np

from collections import OrderedDict

from uncertainty_ranker import UncertaintyRanker

class InverseFAMDUncertaintyRanker(UncertaintyRanker):

    def __init__(self, X, y):
        self.X = X
        self.y = y

    def evaluate(self):
        allData = pd.concat([self.X, self.y], axis=1)
        
        # Just take violation data into account
        data_violations = allData[allData['Constraint violated'] == True]
        data = data_violations.drop(columns=['Constraint violated'])
        
        cols_to_drop = [col for col in data.columns if col[-1] == 'I']
        data = data.drop(cols_to_drop, axis=1)
        cols_to_drop = [col for col in data.columns if data[col].eq(0).all()]
        data = data.drop(cols_to_drop, axis=1)

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
            ranking[column_names[i]] = 1 / sum(abs(loadings[i]))

        self.ranking = OrderedDict(sorted(ranking.items(), key=lambda item: item[1], reverse=True))

    def show_ranking_with_correctness_score(self)-> list[(str,float)]:
        result = []
        for key, value in self.ranking.items():
            result.append((key,float(value)))
        return result