from uncertainty_ranker import UncertaintyRanker

from sklearn.metrics import accuracy_score, classification_report
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from collections import OrderedDict

class LogisticRegressionUncertaintyRanker(UncertaintyRanker):

    def __init__(self, X, y):
        self.X = X
        self.y = y

    def evaluate(self):
        self.model = LogisticRegression()
        self.y = self.y.astype(int)
        self.model.fit(self.X, self.y)

        # Determine feature importance
        coefficients = self.model.coef_
        column_names = self.X.columns.tolist()
        
        ranking_unsorted = {}

        for coefficient_element in coefficients:
            for i in range(len(coefficient_element)):
                item_name = column_names[i]
                item_value = coefficient_element[i]

                if item_name not in ranking_unsorted:
                    ranking_unsorted[item_name] = item_value
                else:
                    ranking_unsorted[item_name] = ranking_unsorted[item_name] + item_value

        self.ranking = OrderedDict(sorted(ranking_unsorted.items(), key=lambda item: item[1], reverse=True)) 

    def show_ranking_with_correctness_score(self)-> list[(str,int)]:
        result = []
        for key, value in self.ranking.items():
            result.append((key,float(value)))
        return result