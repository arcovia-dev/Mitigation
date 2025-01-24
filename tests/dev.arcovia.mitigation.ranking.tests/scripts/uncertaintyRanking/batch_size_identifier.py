import numpy as np
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
from collections import OrderedDict


import pandas as pd

def show_clusters(data_dict):
    # Convert the dictionary to a pandas DataFrame for easier handling
    # The keys of the dictionary are the labels, and the values are the feature vectors
    features = np.array(list(data_dict.values()))
    features = features.reshape(-1, 1)
    k_values = range(2, 3)

    best_k = -1
    best_silhouette_score = -1

    for k in k_values:
        kmeans = KMeans(n_clusters=k, random_state=42)
        kmeans.fit(features)
        labels = kmeans.labels_
        silhouette_avg = silhouette_score(features, labels)
        
        if silhouette_avg > best_silhouette_score:
            best_silhouette_score = silhouette_avg
            best_k = k

    best_kmeans = KMeans(n_clusters=best_k, random_state=42)
    best_kmeans.fit(features)
    labels = best_kmeans.labels_
    predictions = best_kmeans.predict(features)

    # Create a DataFrame to view the results along with the original labels
    clustering_result = pd.DataFrame({
        'Label': labels,
        'Cluster': predictions,
        'Feature Vector': list(data_dict.values())
    })

    cluster_maxes = []
    for cluster_id in range(best_kmeans.n_clusters):
        cluster_data = clustering_result[clustering_result['Cluster'] == cluster_id]
        cluster_maxes.append(max(list(cluster_data['Feature Vector'])))

    cutoff = min(cluster_maxes)

    result = {key: value for key, value in data_dict.items() if value > cutoff}

    return OrderedDict(sorted(result.items(), key=lambda item: item[1], reverse=True))
