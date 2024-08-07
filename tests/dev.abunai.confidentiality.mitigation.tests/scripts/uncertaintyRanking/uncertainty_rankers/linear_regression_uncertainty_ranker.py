from uncertainty_ranker import UncertaintyRanker

from sklearn.metrics import accuracy_score, classification_report
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.model_selection import train_test_split


class LinearRegressionUncertaintyRanker(UncertaintyRanker):

    def __init__(self, X, y):
        self.X = X
        self.y = y

    def evaluate(self):
        self.model = LinearRegression()
        self.model.fit(self.X, self.y)

    def show_ranking_with_correctness_score(self)-> list[(str,int)]:
        # Determine feature importance
        coefficients = self.model.coef_

        # Pair feature names with their corresponding coefficients
        feature_importance_tuples = list(zip(self.X.columns, [abs(c) for c in coefficients]))

        # Optionally, sort the list of tuples by the absolute value of importance
        feature_importance_tuples_sorted = sorted(feature_importance_tuples, key=lambda x: abs(x[1]), reverse=True)

        return feature_importance_tuples_sorted