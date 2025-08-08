package dev.arcovia.mitigation.sat;

import java.util.HashMap;
import java.util.Map;

/**
 * A BiMap is a bidirectional map that maintains a one-to-one relationship between keys and values.
 * It allows retrieval of keys by their corresponding values as well as values by their corresponding keys.
 *
 * This class ensures uniqueness of both keys and values, where a key can only map to one value
 * and a value can only map to one key. Adding a duplicate key or value will overwrite the
 * previous mapping and maintain the bidirectional consistency.
 *
 * @param <K> the type of keys maintained by this bimap
 * @param <V> the type of values mapped by this bimap
 */
public class BiMap<K, V> {
    private final Map<K, V> keyToValueMap;
    private final Map<V, K> valueToKeyMap;

    public BiMap() {
        keyToValueMap = new HashMap<>();
        valueToKeyMap = new HashMap<>();
    }

    public void put(K key, V value) {
        // Remove existing entries to maintain uniqueness
        if (keyToValueMap.containsKey(key)) {
            valueToKeyMap.remove(keyToValueMap.get(key));
        }
        if (valueToKeyMap.containsKey(value)) {
            keyToValueMap.remove(valueToKeyMap.get(value));
        }
        keyToValueMap.put(key, value);
        valueToKeyMap.put(value, key);
    }

    public V getValue(K key) {
        return keyToValueMap.get(key);
    }

    public K getKey(V value) {
        return valueToKeyMap.get(value);
    }

    public void removeByKey(K key) {
        if (keyToValueMap.containsKey(key)) {
            V value = keyToValueMap.get(key);
            keyToValueMap.remove(key);
            valueToKeyMap.remove(value);
        }
    }

    public void removeByValue(V value) {
        if (valueToKeyMap.containsKey(value)) {
            K key = valueToKeyMap.get(value);
            valueToKeyMap.remove(value);
            keyToValueMap.remove(key);
        }
    }

    public void clear() {
        keyToValueMap.clear();
        valueToKeyMap.clear();
    }

    public boolean containsKey(K key) {
        return keyToValueMap.containsKey(key);
    }

    public boolean containsValue(V value) {
        return valueToKeyMap.containsKey(value);
    }

    public int size() {
        return keyToValueMap.size();
    }
}