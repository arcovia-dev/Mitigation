from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
import pandas as pd
from collections import OrderedDict
from uncertainty_ranker import UncertaintyRanker

class PrincipalComponentUncertaintyRanker(UncertaintyRanker):

    def __init__(self, X, X_train, X_test, y, y_train, y_test):
        self.X = X
        self.X_train = X_train
        self.X_test = X_test
        self.y = y
        self.y_train = y_train
        self.y_test = y_test

    def evaluate(self):
        allData = pd.concat([self.X, self.y], axis=1)
        data_with_label_filtered = allData[allData['Constraint violated'] == True]
        data = data_with_label_filtered.drop(columns=['Constraint violated'])
        cols_to_drop = [col for col in data.columns if col[-1] == 'I']

        data = data.drop(cols_to_drop, axis=1)

        # Print each row as a list
        #for index, row in data.iterrows():
         #   row_as_list = row.values.tolist()
          #  print(row_as_list)

        # Standardize the data
        scaler = StandardScaler()
        data_standardized = scaler.fit_transform(data)
        # Perform PCA to determine the explained variance
        pca_full = PCA()
        pca_full.fit(data_standardized)
        # Choose the number of components (e.g., to explain 95% of the variance)
        explained_variance = 0.99
        n_components = next(i for i, cumulative_variance in enumerate(pca_full.explained_variance_ratio_.cumsum(), 1) if cumulative_variance >= explained_variance)

        # Perform PCA with the chosen number of components
        pca = PCA(n_components=n_components)
        pca.fit(data_standardized)

        # Extract principal component vectors
        principal_components = pca.components_

        #print(principal_components)
        #print(n_components)
        #print("Column names:", data.columns.tolist())
        column_names = data.columns.tolist()

        ranking = {}
        for comp in principal_components:
            for i in range(len(comp)):
                key = column_names[i]
                value = comp[i]
                if not key in ranking:
                    ranking[key] = abs(value)
                else:
                    ranking[key] = ranking[key] + abs(value)
        
        self.ranking = OrderedDict(sorted(ranking.items(), key=lambda item: item[1], reverse=True))

    def show_ranking_with_correctness_score(self)-> list[(str,float)]:
        result = []
        for key, value in self.ranking.items():
            result.append((key,float(value)))
        return result