package dev.abunai.confidentiality.mitigation.tests;

import java.util.HashMap;
import java.util.Map;

public class BiMap<K, V> {
    private Map<K, V> keyToValueMap;
    private Map<V, K> valueToKeyMap;

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