from uncertainty_ranker import UncertaintyRanker

from sklearn.metrics import accuracy_score, classification_report
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error, r2_score

class LinearRegressionUncertaintyRanker(UncertaintyRanker):

    def __init__(self, X, X_train, X_test, y, y_train, y_test):
        self.X = X
        self.X_train = X_train
        self.X_test = X_test
        self.y = y
        self.y_train = y_train
        self.y_test = y_test

    def evaluate(self):
        self.model = LinearRegression()
        self.model.fit(self.X_train, self.y_train)

        # Step 5: Evaluate the model
        y_pred = self.model.predict(self.X_test)
        mse = mean_squared_error(self.y_test, y_pred)
        r2 = r2_score(self.y_test, y_pred)


    def show_ranking_with_correctness_score(self)-> list[(str,int)]:
        # Determine feature importance
        coefficients = self.model.coef_

        # Pair feature names with their corresponding coefficients
        feature_importance_tuples = list(zip(self.X.columns, [abs(c) for c in coefficients]))

        # Optionally, sort the list of tuples by the absolute value of importance
        feature_importance_tuples_sorted = sorted(feature_importance_tuples, key=lambda x: abs(x[1]), reverse=True)

        return feature_importance_tuples_sorted