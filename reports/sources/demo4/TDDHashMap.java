package org.jugsaxony.tdd.demo4;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TDDHashMap<K, V> {

    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private K[] keys;
    private V[] values;
    private int size;
    private int capacity;

    @SuppressWarnings("unchecked")
    public TDDHashMap() {
        this.capacity = INITIAL_CAPACITY;
        this.keys = (K[]) new Object[capacity];
        this.values = (V[]) new Object[capacity];
        this.size = 0;
    }

    public V put(final K key, final V value) {
        if (key == null) {
            throw new IllegalArgumentException("Null keys are not permitted");
        }

        if (size >= capacity * LOAD_FACTOR) {
            rehash();
        }

        int idx = hash(key);
        while (keys[idx] != null) {
            if (keys[idx].equals(key)) {
                V old = values[idx];
                values[idx] = value;
                return old;
            }
            idx = (idx + 1) % capacity;
        }

        keys[idx] = key;
        values[idx] = value;
        size++;
        return null;
    }

    public V get(final K key) {
        if (key == null) {
            throw new IllegalArgumentException("Null keys are not permitted");
        }

        int idx = hash(key);
        int startIdx = idx;
        while (keys[idx] != null) {
            if (keys[idx].equals(key)) {
                return values[idx];
            }
            idx = (idx + 1) % capacity;
            if (idx == startIdx) break;
        }
        return null;
    }

    public V remove(final K key) {
        if (key == null) {
            throw new IllegalArgumentException("Null keys are not permitted");
        }

        int idx = hash(key);
        while (keys[idx] != null) {
            if (keys[idx].equals(key)) {
                V val = values[idx];
                keys[idx] = null;
                values[idx] = null;
                size--;
                rehashCluster(idx);
                return val;
            }
            idx = (idx + 1) % capacity;
        }
        return null;
    }

    public int size() {
        return size;
    }

    public List<K> keys() {
        List<K> list = new ArrayList<>(size);
        for (int i = 0; i < capacity; i++) {
            if (keys[i] != null) {
                list.add(keys[i]);
            }
        }
        return list;
    }

    public List<V> values() {
        List<V> list = new ArrayList<>(size);
        for (int i = 0; i < capacity; i++) {
            if (keys[i] != null) {
                list.add(values[i]);
            }
        }
        return list;
    }

    public void clear() {
        for (int i = 0; i < capacity; i++) {
            keys[i] = null;
            values[i] = null;
        }
        size = 0;
    }

    private int hash(K key) {
        return (Objects.hashCode(key) & 0x7fffffff) % capacity;
    }

    @SuppressWarnings("unchecked")
    private void rehash() {
        K[] oldKeys = keys;
        V[] oldValues = values;
        capacity *= 2;
        keys = (K[]) new Object[capacity];
        values = (V[]) new Object[capacity];
        size = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null) {
                put(oldKeys[i], oldValues[i]);
            }
        }
    }

    private void rehashCluster(int hole) {
        int curr = (hole + 1) % capacity;
        while (keys[curr] != null) {
            K keyToRedo = keys[curr];
            V valToRedo = values[curr];
            keys[curr] = null;
            values[curr] = null;
            size--;
            put(keyToRedo, valToRedo);
            curr = (curr + 1) % capacity;
        }
    }
}
