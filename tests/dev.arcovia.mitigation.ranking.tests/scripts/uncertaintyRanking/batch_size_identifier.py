import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
from collections import OrderedDict

def show_clusters(data_dict):
    features = np.array(list(data_dict.values())).reshape(-1, 1)

    min_k = 6
    max_k = 8
    k_values = range(min_k,max_k+1)

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

    result_list = []
    prev_cluster = None

    for _, row in clustering_result.iterrows():
        cluster_id = row['Cluster']
        
        if prev_cluster is not None and prev_cluster != cluster_id:
            result_list.append(("_Cluster-Separator_"+ str(cluster_id) +"_D", None))
        
        result_list.append((row['Label'], row['Feature Vector']))
        
        prev_cluster = cluster_id  # Update previous cluster

    result = OrderedDict(result_list)
    return result

