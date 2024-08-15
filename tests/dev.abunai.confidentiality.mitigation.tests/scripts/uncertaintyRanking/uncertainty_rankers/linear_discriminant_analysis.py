from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
import pandas as pd

from collections import OrderedDict

from uncertainty_ranker import UncertaintyRanker

class LinearDiscriminantAnalysisRanker(UncertaintyRanker):

    def __init__(self, X, y):
        self.X = X
        self.y = y

    def evaluate(self):
        # No LDA based ranking possible if just one data row exists so return all non irrelevant entries as ranking
        if self.X.shape[0] == 1:
            self.ranking = {}
            for name in self.X.columns.tolist():
                self.ranking[name] = 1
            return
        
        lda = LinearDiscriminantAnalysis()
        lda.fit(self.X, self.y)

        coefs = lda.coef_
        column_names = self.X.columns.tolist()
        self.ranking = {}

        for i in range(len(column_names)):
            self.ranking[column_names[i]] = coefs[0][i]

        self.ranking = OrderedDict(sorted(self.ranking.items(), key=lambda item: item[1], reverse=True))

    def show_ranking_with_correctness_score(self)-> list[(str,float)]:
        result = []
        for key, value in self.ranking.items():
            result.append((key,float(value)))
        return result