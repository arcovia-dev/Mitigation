from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split
import pandas as pd
#from uncertainty_ranker import UncertaintyRanker

class RandomForestUncertaintyRanker():

    def __init__(self, X, y):
        self.X = X
        self.y = y

    def evaluate(self):
        self.rf_model = RandomForestClassifier(n_estimators=100, random_state=42)
        self.rf_model.fit(self.X, self.y)


    def show_ranking_with_correctness_score(self)-> list[(str,int)]:
        feature_importances = self.rf_model.feature_importances_

        # Map the feature importances to feature names
        feature_names = self.X.columns
        importances_df = pd.DataFrame({'Feature': feature_names, 'Importance': feature_importances})

        # Sort the importances in descending order
        importances_df = importances_df.sort_values(by='Importance', ascending=False)

        uncertainties_ranked = importances_df['Feature'].tolist()
        uncertainties_ranking_scores = importances_df['Importance'].tolist()

        return [(uncertainties_ranked[i],uncertainties_ranking_scores[i]) for i in range(len(uncertainties_ranked))]