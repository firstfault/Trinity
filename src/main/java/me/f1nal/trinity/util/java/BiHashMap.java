package me.f1nal.trinity.util.java;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BiHashMap<K, V> {
    private Map<K, V> forwardMap = new HashMap<>();
    private Map<V, K> reverseMap = new HashMap<>();

    public void put(K key, V value) {
        forwardMap.put(key, value);
        reverseMap.put(value, key);
    }

    public V getValue(K key) {
        return forwardMap.get(key);
    }

    public K getKey(V value) {
        return reverseMap.get(value);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return forwardMap.entrySet();
    }
}