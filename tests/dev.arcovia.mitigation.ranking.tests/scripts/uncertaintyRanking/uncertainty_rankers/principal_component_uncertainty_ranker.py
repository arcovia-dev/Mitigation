from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.preprocessing import OneHotEncoder

import pandas as pd

from collections import OrderedDict

from uncertainty_ranker import UncertaintyRanker

class PrincipalComponentUncertaintyRanker(UncertaintyRanker):

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

        # Remove cols for scenarios that just appear in not constraint violating rows
        cols_to_drop = [col for col in data.columns if data[col].eq(0).all()]
        data = data.drop(cols_to_drop, axis=1)
        
        # No PCA based ranking possible if just one data row exists so return all non irrelevant entries as ranking
        if data.shape[0] == 1:
            self.ranking = {}
            for name in data.columns.tolist():
                self.ranking[name] = 1
            return

        # Standardize data
        scaler = StandardScaler()
        data_standardized = scaler.fit_transform(data)
        
        # Additional PCA to determine amount of components
        pca_full = PCA()
        pca_full.fit(data_standardized)
        
        # Choose number of components
        explained_variance = 0.8
        n_components = next(i for i, cumulative_variance in enumerate(pca_full.explained_variance_ratio_.cumsum(), 1) if cumulative_variance >= explained_variance)

        # PCA with chosen number of components
        pca = PCA(n_components=n_components)
        pca.fit(data_standardized)
        explained_variance = pca_full.explained_variance_ratio_

        # Create ranking based on pca result
        principal_components = pca_full.components_
        loadings = principal_components.T
        column_names = data.columns.tolist()
        ranking = {}
        
        for i in range(len(loadings)):
            if column_names[i] == "Constraint violated_True":
                continue
            ranking[column_names[i]] = sum(loadings[i])
        
        self.ranking = OrderedDict(sorted(ranking.items(), key=lambda item: item[1], reverse=True))

    def show_ranking_with_correctness_score(self)-> list[(str,float)]:
        result = []
        for key, value in self.ranking.items():
            result.append((key,float(value)))
        return result