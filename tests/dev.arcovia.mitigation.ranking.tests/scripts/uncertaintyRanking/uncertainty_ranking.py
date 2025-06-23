import pandas as pd
from sklearn.preprocessing import OneHotEncoder

from collections import OrderedDict
import math
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), 'uncertainty_rankers'))

from principal_component_uncertainty_ranker import PrincipalComponentUncertaintyRanker
from inverse_principal_component_uncertainty_ranker import InversePrincipalComponentUncertaintyRanker
from famd_uncertainty_ranker import FAMDUncertaintyRanker
from inverse_famd_uncertainty_ranker import InverseFAMDUncertaintyRanker
from random_forest_uncertainty_ranker import RandomForestUncertaintyRanker
from linear_regression_uncertainty_ranker import LinearRegressionUncertaintyRanker
from logistic_regression_uncertainty_ranker import LogisticRegressionUncertaintyRanker
from linear_discriminant_analysis import LinearDiscriminantAnalysisRanker
import batch_size_identifier

import warnings

# Disable all warnings
warnings.filterwarnings("ignore")

# Constants
TRAIN_FILES_DIR = sys.argv[1]
RELEVANT_UNCERTAINTIES_LENGTH = int(sys.argv[2])
RANKER_TYPE = sys.argv[3]
AGGREGATION_TYPE = sys.argv[4]
BATCH_SIZE_OPTIMIZATION = sys.argv[5] # Y or N
SEPERATOR = ';'


'''
    Aggregrations the results of previously computed uncertainty rankings
    for different constraints
'''
def aggregate_rankings_by_summing_up_linear(rankings:list[list[(str,float)]]):
    aggregatedRanking = {}
    for ranking in rankings:
        ranking_ordered = OrderedDict(sorted(dict(ranking).items(), key=lambda item: item[1], reverse=True))
        i = 0
        uncertainty_names = []
        while i < RELEVANT_UNCERTAINTIES_LENGTH and bool(ranking_ordered):
            element = ranking_ordered.popitem(last=False)
            element_name = element[0]
            uncertainty_name = '_'.join(element_name.split('_')[:-1])
            if uncertainty_name in uncertainty_names:
                continue
            if not element_name in aggregatedRanking:
                aggregatedRanking[element_name] = element[1]
            else:
                aggregatedRanking[element_name] = aggregatedRanking[element_name] + element[1]
            if uncertainty_name not in uncertainty_names:
                i = i + 1
                uncertainty_names.append(uncertainty_name)


    return OrderedDict(sorted(aggregatedRanking.items(), key=lambda item: item[1], reverse=True))

'''
    Aggregrations the results of previously computed uncertainty rankings
    for different constraints
'''
def aggregate_rankings_by_summing_up_exponential(rankings:list[list[(str,float)]]):
    aggregatedRanking = {}
    for ranking in rankings:
        ranking_ordered = OrderedDict(sorted(dict(ranking).items(), key=lambda item: item[1], reverse=True))
        i = 0
        uncertainty_names = []
        
        while i < RELEVANT_UNCERTAINTIES_LENGTH and bool(ranking_ordered):
            element = ranking_ordered.popitem(last=False)
            element_name = element[0]
            uncertainty_name = '_'.join(element_name.split('_')[:-1])
            if uncertainty_name in uncertainty_names:
                continue
            if not element_name in aggregatedRanking:
                aggregatedRanking[element_name] = math.exp(-i)
            else:
                aggregatedRanking[element_name] = aggregatedRanking[element_name] + math.exp(-i)
            if uncertainty_name not in uncertainty_names:
                i = i + 1
                uncertainty_names.append(uncertainty_name)

    return OrderedDict(sorted(aggregatedRanking.items(), key=lambda item: item[1], reverse=True))


'''
    Aggregrations the results of previously computed uncertainty rankings
    for different constraints
'''
def aggregate_rankings_by_taking_top_3(rankings:list[list[(str,float)]]):
    aggregatedRanking = {}
    for ranking in rankings:
        ranking_ordered = OrderedDict(sorted(dict(ranking).items(), key=lambda item: item[1], reverse=True))
        i = 0
        uncertainty_names = []
        while bool(ranking_ordered):
            element = ranking_ordered.popitem(last=False)
            element_name = element[0]
            uncertainty_name = '_'.join(element_name.split('_')[:-1])
            if uncertainty_name in uncertainty_names:
                continue
            if not element_name in aggregatedRanking:
                aggregatedRanking[element_name] = 3-i if i < 3 else 0
            elif i < 3 :
                aggregatedRanking[element_name] = aggregatedRanking[element_name] + 3-i 
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
            sum = sum + abs(ranking_element[1])
        
        # If every element has score 0 normalization is already done
        if (sum == 0):
            return rankings
        
        new_ranking = []
        for ranking_element in ranking:
            new_ranking.append((ranking_element[0], (sum + ranking_element[1])/(2*sum)))
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

    cols_to_drop = [col for col in X.columns if col[-1] == 'I']
    X = X.drop(cols_to_drop, axis=1)
    cols_to_drop = [col for col in X.columns if X[col].eq(0).all()]
    X = X.drop(cols_to_drop, axis=1)

    if RANKER_TYPE == "LDA":
        uncertainty_ranker = LinearDiscriminantAnalysisRanker(X, y)
    elif RANKER_TYPE == "P":
        uncertainty_ranker = PrincipalComponentUncertaintyRanker(X, y)
    elif RANKER_TYPE == "IP":
        uncertainty_ranker = InversePrincipalComponentUncertaintyRanker(X, y)
    elif RANKER_TYPE == "F":
        uncertainty_ranker = FAMDUncertaintyRanker(X, y)
    elif RANKER_TYPE == "IF":
        uncertainty_ranker = InverseFAMDUncertaintyRanker(X, y)
    elif RANKER_TYPE == "RF":
        uncertainty_ranker = RandomForestUncertaintyRanker(X, y)
    elif RANKER_TYPE == "LR":
        uncertainty_ranker = LinearRegressionUncertaintyRanker(X, y)
    elif RANKER_TYPE == "LGR":
        uncertainty_ranker = LogisticRegressionUncertaintyRanker(X,y)
    else:
        uncertainty_ranker = PrincipalComponentUncertaintyRanker(X, y)

    uncertainty_ranker.evaluate()
    rating = uncertainty_ranker.show_ranking_with_correctness_score()
    allRatings.append(rating)

allRatings = normalize_rankings(allRatings)
if AGGREGATION_TYPE == "L":
    final_ranking = aggregate_rankings_by_summing_up_linear(allRatings)
elif AGGREGATION_TYPE == "E":
    final_ranking = aggregate_rankings_by_summing_up_exponential(allRatings)
else:
    final_ranking = aggregate_rankings_by_taking_top_3(allRatings)

relevant_uncertainties = []
printedCount = 0

if BATCH_SIZE_OPTIMIZATION == 'Y':
    final_ranking = batch_size_identifier.show_clusters(final_ranking)

while printedCount < RELEVANT_UNCERTAINTIES_LENGTH and bool(final_ranking):
    item = final_ranking.popitem(last=False)

    column = item[0].split('_').pop()
    uncertainty_name = '_'.join(item[0].split('_')[:-1])


    if(uncertainty_name.startswith('_Cluster-Separator')):
        relevant_uncertainties.append(uncertainty_name)

    # Do not add uncertainties that are already in the ranking 
    elif uncertainty_name not in relevant_uncertainties:
        relevant_uncertainties.append(uncertainty_name)
        printedCount = printedCount + 1

for relevant_uncertainty in relevant_uncertainties:
    print(relevant_uncertainty)