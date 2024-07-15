import pandas as pd
from sklearn.preprocessing import OneHotEncoder

from collections import OrderedDict
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), 'uncertainty_rankers'))

from principal_component_uncertainty_ranker import PrincipalComponentUncertaintyRanker
from famd_uncertainty_ranker import FAMDUncertaintyRanker

# Constants
TRAIN_FILES_DIR = sys.argv[1]
RELEVANT_UNCERTAINTIES_LENGTH = int(sys.argv[2])
SEPERATOR = ';'

'''
    Aggregrations the results of previously computed uncertainty rankings
    for different constraints
'''
def aggregate_rankings_by_sum_of_ratings(rankings:list[list[(str,float)]]):
    aggregatedRanking = {}
    for ranking in rankings:
        for ranking_element in ranking:
            if ranking_element[0] not in aggregatedRanking:
                aggregatedRanking[ranking_element[0]] = ranking_element[1]
            else:
                aggregatedRanking[ranking_element[0]] = aggregatedRanking[ranking_element[0]] + ranking_element[1]

    
    return OrderedDict(sorted(aggregatedRanking.items(), key=lambda item: item[1], reverse=True))

'''
    Aggregrations the results of previously computed uncertainty rankings
    for different constraints
'''
def aggregate_rankings_by_taking_top_n_of_each_ranking(rankings:list[list[(str,float)]],n:int):
    aggregatedRanking = {}
    for ranking in rankings:
        ranking_ordered = OrderedDict(sorted(dict(ranking).items(), key=lambda item: item[1], reverse=True))
        i = 0
        uncertainty_names = []
        while i < n and bool(ranking_ordered):
            element = ranking_ordered.popitem(last=False)
            element_name = element[0]
            uncertainty_name = '_'.join(element_name.split('_')[:-1])
            if uncertainty_name in uncertainty_names:
                continue
            if not element_name in aggregatedRanking:
                aggregatedRanking[element_name] = n-i
            else:
                aggregatedRanking[element_name] = aggregatedRanking[element_name] + (n-i)
            if uncertainty_name not in uncertainty_names:
                i = i + 1
                uncertainty_names.append(uncertainty_name)


    return OrderedDict(sorted(aggregatedRanking.items(), key=lambda item: item[1], reverse=True))


'''
    Normalized by diving each ranking entry by the sum of all ranking values of the rating
'''
def normalize_rankings(rankings:list[list[(str,float)]]):
    new_rankings = []
    for ranking in rankings:
        # Find max value of ranking
        sum = 0
        for ranking_element in ranking:
            sum = sum + ranking_element[1]

        new_ranking = []
        for ranking_element in ranking:
            new_ranking.append((ranking_element[0], ranking_element[1]/sum))
        new_rankings.append(new_ranking)
    
    return new_rankings


filenames = os.listdir(TRAIN_FILES_DIR)
allRatings = []

for filename in filenames:
    df = pd.read_csv(f'{TRAIN_FILES_DIR}/{filename}', sep=SEPERATOR)

    X = df.drop('Constraint violated', axis=1)
    y = df['Constraint violated']

    # Identify categorical columns
    categorical_cols = X.select_dtypes(include=['object', 'category']).columns

    # One-hot encoding for categorical columns
    encoder = OneHotEncoder(sparse_output=False)
    X_encoded = pd.DataFrame(encoder.fit_transform(X[categorical_cols]), columns=encoder.get_feature_names_out())

    # Drop original categorical columns and concatenate one hot encoded columns
    X = X.drop(categorical_cols, axis=1)
    X = pd.concat([X, X_encoded], axis=1)

    uncertainty_ranker = PrincipalComponentUncertaintyRanker(X, y)
    uncertainty_ranker.evaluate()
    rating = uncertainty_ranker.show_ranking_with_correctness_score()
    
    allRatings.append(rating)

allRatings = normalize_rankings(allRatings)
final_ranking = aggregate_rankings_by_taking_top_n_of_each_ranking(allRatings,int(RELEVANT_UNCERTAINTIES_LENGTH))
relevant_uncertainties = []
printedCount = 0

while printedCount < RELEVANT_UNCERTAINTIES_LENGTH and bool(final_ranking):
    item = final_ranking.popitem(last=False)
    column = item[0].split('_').pop()
    uncertainty_name = '_'.join(item[0].split('_')[:-1])
    
    # Do not add uncertainties that are already in the ranking 
    if uncertainty_name not in relevant_uncertainties:
        relevant_uncertainties.append(uncertainty_name)
        printedCount = printedCount + 1

for relevant_uncertainty in relevant_uncertainties:
    print(relevant_uncertainty)