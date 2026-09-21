package org.jugsaxony.tdd.demo8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An open addressing hash map supporting unbound capacity, allocation-free storage
 * (except backing arrays), null values, and no null keys.
 *
 * Collision resolution is done via linear probing with backward-shift deletion
 * to avoid tombstone buildup.
 */
public class TDDHashMap<K, V> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private Object[] keys;
    private Object[] values;
    private int size;
    private int threshold;

    public TDDHashMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public TDDHashMap(int initialCapacity) {
        int capacity = 1;
        while (capacity < initialCapacity) {
            capacity <<= 1;
        }
        this.keys = new Object[capacity];
        this.values = new Object[capacity];
        this.size = 0;
        this.threshold = (int) (capacity * DEFAULT_LOAD_FACTOR);
    }

    private int hash(final K key, int length) {
        int h = key.hashCode();
        h ^= (h >>> 16);
        return (h & 0x7fffffff) & (length - 1);
    }

    @SuppressWarnings("unchecked")
    public V get(final K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (size == 0) {
            return null;
        }

        final Object[] kArray = this.keys;
        final int mask = kArray.length - 1;
        int idx = hash(key, kArray.length);

        while (true) {
            final Object existingKey = kArray[idx];
            if (existingKey == null) {
                return null;
            }
            if (existingKey.equals(key)) {
                return (V) values[idx];
            }
            idx = (idx + 1) & mask;
        }
    }

    @SuppressWarnings("unchecked")
    public V put(final K key, final V value) {
        Objects.requireNonNull(key, "Key cannot be null");

        if (size >= threshold) {
            resize(keys.length << 1);
        }

        final Object[] kArray = this.keys;
        final Object[] vArray = this.values;
        final int mask = kArray.length - 1;
        int idx = hash(key, kArray.length);

        while (true) {
            final Object existingKey = kArray[idx];
            if (existingKey == null) {
                kArray[idx] = key;
                vArray[idx] = value;
                size++;
                return null;
            }
            if (existingKey.equals(key)) {
                V oldVal = (V) vArray[idx];
                vArray[idx] = value;
                return oldVal;
            }
            idx = (idx + 1) & mask;
        }
    }

    @SuppressWarnings("unchecked")
    public V remove(final K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (size == 0) {
            return null;
        }

        final Object[] kArray = this.keys;
        final Object[] vArray = this.values;
        final int mask = kArray.length - 1;
        int idx = hash(key, kArray.length);

        while (true) {
            final Object existingKey = kArray[idx];
            if (existingKey == null) {
                return null;
            }
            if (existingKey.equals(key)) {
                V oldValue = (V) vArray[idx];
                deleteAndShift(idx);
                size--;
                return oldValue;
            }
            idx = (idx + 1) & mask;
        }
    }

    @SuppressWarnings("unchecked")
    private void deleteAndShift(int slotToRemove) {
        final Object[] kArray = this.keys;
        final Object[] vArray = this.values;
        final int mask = kArray.length - 1;

        int i = slotToRemove;
        int j = (i + 1) & mask;

        while (kArray[j] != null) {
            int k = hash((K) kArray[j], kArray.length);
            // Check if k is cyclically between i and j:
            // k is strictly before i or after j in cyclic order -> needs to be shifted to i
            boolean between;
            if (i <= j) {
                between = (k <= i || k > j);
            } else {
                between = (k <= i && k > j);
            }

            if (between) {
                kArray[i] = kArray[j];
                vArray[i] = vArray[j];
                i = j;
            }
            j = (j + 1) & mask;
        }

        kArray[i] = null;
        vArray[i] = null;
    }

    @SuppressWarnings("unchecked")
    private void resize(int newCapacity) {
        Object[] oldKeys = this.keys;
        Object[] oldValues = this.values;

        Object[] newKeys = new Object[newCapacity];
        Object[] newValues = new Object[newCapacity];
        int mask = newCapacity - 1;

        for (int i = 0; i < oldKeys.length; i++) {
            Object key = oldKeys[i];
            if (key != null) {
                int idx = hash((K) key, newCapacity);
                while (newKeys[idx] != null) {
                    idx = (idx + 1) & mask;
                }
                newKeys[idx] = key;
                newValues[idx] = oldValues[i];
            }
        }

        this.keys = newKeys;
        this.values = newValues;
        this.threshold = (int) (newCapacity * DEFAULT_LOAD_FACTOR);
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    public List<K> keys() {
        List<K> result = new ArrayList<>(size);
        for (Object key : keys) {
            if (key != null) {
                result.add((K) key);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<V> values() {
        List<V> result = new ArrayList<>(size);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                result.add((V) values[i]);
            }
        }
        return result;
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        size = 0;
    }
}
