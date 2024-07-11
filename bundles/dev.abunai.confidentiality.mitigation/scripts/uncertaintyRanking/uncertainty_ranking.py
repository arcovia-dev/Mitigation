import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import OneHotEncoder
from collections import OrderedDict
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), 'uncertainty_rankers'))
from linear_regression_uncertainty_ranker import LinearRegressionUncertaintyRanker
from random_forest_uncertainty_ranker import RandomForestUncertaintyRanker 
from principal_component_uncertainty_ranker import PrincipalComponentUncertaintyRanker

TRAIN_FILES_DIR = sys.argv[1]
RELEVANT_UNCERTAINTIES_LENGTH = int(sys.argv[2])
SEPERATOR = ';'

def getAggregatedRanking(rankings:list[list[(str,float)]]):
    aggregatedRanking = {}
    for ranking in rankings:
        for ranking_element in ranking:
            if ranking_element[0] not in aggregatedRanking:
                aggregatedRanking[ranking_element[0]] = ranking_element[1]
            else:
                aggregatedRanking[ranking_element[0]] = aggregatedRanking[ranking_element[0]] + ranking_element[1]

    
    return OrderedDict(sorted(aggregatedRanking.items(), key=lambda item: item[1], reverse=True))

# Get a list of all filenames in the specified directory
filenames = os.listdir(TRAIN_FILES_DIR)

allRatings = []

for filename in filenames:
    # Load the CSV file
    df = pd.read_csv(f'{TRAIN_FILES_DIR}/{filename}', sep=SEPERATOR)

    # Assuming 'target' is the column we want to predict
    X = df.drop('Constraint violated', axis=1)
    y = df['Constraint violated']

    # Identify categorical columns
    categorical_cols = X.select_dtypes(include=['object', 'category']).columns

    # One-hot encode categorical columns
    encoder = OneHotEncoder(sparse_output=False)
    X_encoded = pd.DataFrame(encoder.fit_transform(X[categorical_cols]), columns=encoder.get_feature_names_out())

    # Drop original categorical columns and concatenate encoded columns
    X = X.drop(categorical_cols, axis=1)
    X = pd.concat([X, X_encoded], axis=1)

    # Split the data into training and testing sets
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    uncertainty_ranker = PrincipalComponentUncertaintyRanker(X, X_train, X_test, y, y_train, y_test)
    uncertainty_ranker.evaluate()
    rating = uncertainty_ranker.show_ranking_with_correctness_score()
    
    #print(f'Rating for file: {filename}')
    #print(rating)
    #print('------------------------------------------------------------------------------')
    
    allRatings.append(rating)

#print('Final Ranking:')
final_ranking = getAggregatedRanking(allRatings)
relevant_uncertainties = []
printedCount = 0
while printedCount < RELEVANT_UNCERTAINTIES_LENGTH and bool(final_ranking):
    item = final_ranking.popitem(last=False)
    column = item[0].split('_').pop()
    uncertainty_name = '_'.join(item[0].split('_')[:-1])
    if uncertainty_name not in relevant_uncertainties and column != 'Irrelevant':
        relevant_uncertainties.append(uncertainty_name)
        printedCount = printedCount + 1

for relevant_uncertainty in relevant_uncertainties:
    print(relevant_uncertainty)