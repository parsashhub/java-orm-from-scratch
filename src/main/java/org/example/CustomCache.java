package org.example;

import java.util.concurrent.ConcurrentHashMap;

public class CustomCache<K, V> implements Cache<K, V> {
    private ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void invalidate(K key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
