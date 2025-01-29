import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
from collections import OrderedDict

def show_clusters(data_dict):
    features = np.array(list(data_dict.values())).reshape(-1, 1)
    k_values = range(2, 8)

    best_k = -1
    best_silhouette_score = -np.inf

    # Find the best k using silhouette score
    for k in k_values:
        kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
        kmeans.fit(features)
        labels = kmeans.labels_
        silhouette_avg = silhouette_score(features, labels)

        if silhouette_avg > best_silhouette_score:
            best_silhouette_score = silhouette_avg
            best_k = k

    # Fit the best model
    best_kmeans = KMeans(n_clusters=best_k, random_state=42, n_init=10)
    best_kmeans.fit(features)
    labels = best_kmeans.labels_

    # Create DataFrame with original labels
    clustering_result = pd.DataFrame({
        'Label': list(data_dict.keys()),
        'Cluster': labels,
        'Feature Vector': list(data_dict.values())
    })

    # Get max feature vector per cluster
    cluster_maxes = []
    for cluster_id in range(best_kmeans.n_clusters):
        cluster_data = clustering_result[clustering_result['Cluster'] == cluster_id]
        cluster_maxes.append(cluster_data['Feature Vector'].max())

    cutoff = min(cluster_maxes)

    # Filter data based on cutoff
    result = {key: value for key, value in data_dict.items() if value > cutoff}

    return OrderedDict(sorted(result.items(), key=lambda item: item[1], reverse=True))
