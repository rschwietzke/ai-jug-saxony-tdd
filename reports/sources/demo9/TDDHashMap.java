package org.jugsaxony.tdd.demo9;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Open hashing (open addressing) hash map implementation with allocation-free storage.
 *
 * <p>Uses linear probing with Knuth backward-shift deletion, completely avoiding
 * tombstone markers and wrapper objects for stored entries.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class TDDHashMap<K, V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    private Object[] keys;
    private Object[] values;
    private int size;
    private int threshold;

    public TDDHashMap() {
        this.keys = new Object[DEFAULT_CAPACITY];
        this.values = new Object[DEFAULT_CAPACITY];
        this.size = 0;
        this.threshold = (int) (DEFAULT_CAPACITY * LOAD_FACTOR);
    }

    public V get(final K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        final int mask = keys.length - 1;
        int idx = hash(key) & mask;

        while (keys[idx] != null) {
            if (keys[idx].equals(key)) {
                @SuppressWarnings("unchecked")
                final V val = (V) values[idx];
                return val;
            }
            idx = (idx + 1) & mask;
        }

        return null;
    }

    public V put(final K key, final V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        if (size >= threshold) {
            resize();
        }

        final int mask = keys.length - 1;
        int idx = hash(key) & mask;

        while (keys[idx] != null) {
            if (keys[idx].equals(key)) {
                @SuppressWarnings("unchecked")
                final V previousValue = (V) values[idx];
                values[idx] = value;
                return previousValue;
            }
            idx = (idx + 1) & mask;
        }

        keys[idx] = key;
        values[idx] = value;
        size++;
        return null;
    }

    public V remove(final K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        final int mask = keys.length - 1;
        int i = hash(key) & mask;

        while (keys[i] != null) {
            if (keys[i].equals(key)) {
                @SuppressWarnings("unchecked")
                final V previousValue = (V) values[i];
                shiftDelete(i);
                size--;
                return previousValue;
            }
            i = (i + 1) & mask;
        }

        return null;
    }

    public int size() {
        return size;
    }

    public List<K> keys() {
        final List<K> result = new ArrayList<>(size);
        for (final Object k : keys) {
            if (k != null) {
                @SuppressWarnings("unchecked")
                final K typedKey = (K) k;
                result.add(typedKey);
            }
        }
        return result;
    }

    public List<V> values() {
        final List<V> result = new ArrayList<>(size);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                @SuppressWarnings("unchecked")
                final V typedValue = (V) values[i];
                result.add(typedValue);
            }
        }
        return result;
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        size = 0;
    }

    /**
     * Backward-shift deletion for linear probing.
     * Moves any subsequent colliding entry in the cluster backward if its ideal hash slot permits,
     * maintaining probe chain continuity without tombstones.
     *
     * @param hole the index of the slot to delete
     */
    private void shiftDelete(int hole) {
        final int mask = keys.length - 1;
        int j = (hole + 1) & mask;

        while (keys[j] != null) {
            final int k = hash(keys[j]) & mask;

            // Check if hole is cyclically between ideal position k and current position j
            if (((hole - k) & mask) < ((j - k) & mask)) {
                keys[hole] = keys[j];
                values[hole] = values[j];
                hole = j;
            }
            j = (j + 1) & mask;
        }

        keys[hole] = null;
        values[hole] = null;
    }

    private void resize() {
        final int oldCapacity = keys.length;
        if (oldCapacity >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        final int newCapacity = oldCapacity << 1;
        final Object[] oldKeys = keys;
        final Object[] oldValues = values;

        this.keys = new Object[newCapacity];
        this.values = new Object[newCapacity];
        this.threshold = (int) (newCapacity * LOAD_FACTOR);
        final int mask = newCapacity - 1;

        for (int i = 0; i < oldCapacity; i++) {
            final Object k = oldKeys[i];
            if (k != null) {
                int idx = hash(k) & mask;
                while (keys[idx] != null) {
                    idx = (idx + 1) & mask;
                }
                keys[idx] = k;
                values[idx] = oldValues[i];
            }
        }
    }

    private int hash(final Object key) {
        final int h = key.hashCode();
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }
}
