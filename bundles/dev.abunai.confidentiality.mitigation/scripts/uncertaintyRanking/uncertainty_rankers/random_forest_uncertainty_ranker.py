from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import pandas as pd
from uncertainty_ranker import UncertaintyRanker

class RandomForestUncertaintyRanker(UncertaintyRanker):

    def __init__(self, X, X_train, X_test, y, y_train, y_test):
        self.X = X
        self.X_train = X_train
        self.X_test = X_test
        self.y = y
        self.y_train = y_train
        self.y_test = y_test

    def evaluate(self):
        self.rf_model = RandomForestClassifier(n_estimators=100, random_state=42)

        # Train the model
        self.rf_model.fit(self.X_train, self.y_train)

        y_pred = self.rf_model.predict(self.X_test)

        # Evaluate the model
        accuracy = accuracy_score(self.y_test, y_pred)
        report = classification_report(self.y_test, y_pred)

        #print(f"Accuracy: {accuracy}")
        #print("Classification Report:\n", report)


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