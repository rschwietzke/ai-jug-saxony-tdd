package org.jugsaxony.tdd.demo1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Open hashing map with open addressing and no wrappers for storage.
 * Uses linear probing with backward-shift deletion for zero tombstones and allocation-free operations.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class TDDHashMap<K, V> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private Object[] keys;
    private Object[] values;
    private int size;
    private int capacity;
    private int mask;
    private int threshold;
    private final float loadFactor;

    public TDDHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public TDDHashMap(final int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public TDDHashMap(final int initialCapacity, final float loadFactor) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Initial capacity must be positive: " + initialCapacity);
        }
        if (loadFactor <= 0.0f || Float.isNaN(loadFactor) || loadFactor >= 1.0f) {
            throw new IllegalArgumentException("Load factor must be between 0 and 1: " + loadFactor);
        }

        this.capacity = tableSizeFor(initialCapacity);
        this.mask = this.capacity - 1;
        this.loadFactor = loadFactor;
        this.threshold = (int) (this.capacity * this.loadFactor);
        this.keys = new Object[this.capacity];
        this.values = new Object[this.capacity];
        this.size = 0;
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the key, or null if not found
     * @throws NullPointerException if the key is null
     */
    @SuppressWarnings("unchecked")
    public V get(final K key) {
        Objects.requireNonNull(key, "Key cannot be null");

        int slot = hash(key) & mask;
        while (true) {
            final Object currKey = keys[slot];
            if (currKey == null) {
                return null;
            }
            if (currKey.equals(key)) {
                return (V) values[slot];
            }
            slot = (slot + 1) & mask;
        }
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping for key
     * @throws NullPointerException if the key is null
     */
    @SuppressWarnings("unchecked")
    public V put(final K key, final V value) {
        Objects.requireNonNull(key, "Key cannot be null");

        if (size >= threshold) {
            resize(capacity << 1);
        }

        int slot = hash(key) & mask;
        while (true) {
            final Object currKey = keys[slot];
            if (currKey == null) {
                keys[slot] = key;
                values[slot] = value;
                size++;
                return null;
            }
            if (currKey.equals(key)) {
                final V oldValue = (V) values[slot];
                values[slot] = value;
                return oldValue;
            }
            slot = (slot + 1) & mask;
        }
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there was no mapping for key
     * @throws NullPointerException if the key is null
     */
    @SuppressWarnings("unchecked")
    public V remove(final K key) {
        Objects.requireNonNull(key, "Key cannot be null");

        int slot = hash(key) & mask;
        while (true) {
            final Object currKey = keys[slot];
            if (currKey == null) {
                return null;
            }
            if (currKey.equals(key)) {
                final V oldValue = (V) values[slot];
                shiftDelete(slot);
                return oldValue;
            }
            slot = (slot + 1) & mask;
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    /**
     * Returns a list of all keys contained in this map.
     *
     * @return a list view of the keys
     */
    @SuppressWarnings("unchecked")
    public List<K> keys() {
        final List<K> result = new ArrayList<>(size);
        for (final Object k : keys) {
            if (k != null) {
                result.add((K) k);
            }
        }
        return result;
    }

    /**
     * Returns a list of all values contained in this map.
     *
     * @return a list view of the values
     */
    @SuppressWarnings("unchecked")
    public List<V> values() {
        final List<V> result = new ArrayList<>(size);
        for (int i = 0; i < capacity; i++) {
            if (keys[i] != null) {
                result.add((V) values[i]);
            }
        }
        return result;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        size = 0;
    }

    /**
     * Performs backward-shift deletion for linear probing (Knuth's Algorithm R).
     * Clears slot i and shifts subsequent colliding entries to maintain probe chain continuity.
     *
     * @param emptySlot the index of the slot to delete
     */
    private void shiftDelete(int emptySlot) {
        keys[emptySlot] = null;
        values[emptySlot] = null;
        size--;

        int j = emptySlot;
        while (true) {
            j = (j + 1) & mask;
            final Object k = keys[j];
            if (k == null) {
                break;
            }
            final int kIdealSlot = hash(k) & mask;
            final int distCurrentToIdeal = (j - kIdealSlot) & mask;
            final int distCurrentToEmpty = (j - emptySlot) & mask;

            if (distCurrentToEmpty <= distCurrentToIdeal) {
                keys[emptySlot] = keys[j];
                values[emptySlot] = values[j];
                keys[j] = null;
                values[j] = null;
                emptySlot = j;
            }
        }
    }

    /**
     * Resizes the backing arrays to a new capacity (power of 2) and rehashes all elements.
     *
     * @param newCapacity the new capacity (power of 2)
     */
    private void resize(final int newCapacity) {
        final Object[] oldKeys = this.keys;
        final Object[] oldValues = this.values;
        final int oldCapacity = this.capacity;

        this.capacity = newCapacity;
        this.mask = newCapacity - 1;
        this.threshold = (int) (newCapacity * this.loadFactor);
        this.keys = new Object[newCapacity];
        this.values = new Object[newCapacity];

        for (int i = 0; i < oldCapacity; i++) {
            final Object key = oldKeys[i];
            if (key != null) {
                int slot = hash(key) & mask;
                while (keys[slot] != null) {
                    slot = (slot + 1) & mask;
                }
                keys[slot] = key;
                values[slot] = oldValues[i];
            }
        }
    }

    /**
     * Spread higher bits of hash code to lower bits for power-of-two table sizing.
     */
    private static int hash(final Object key) {
        final int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    /**
     * Computes the nearest power of 2 greater than or equal to the requested capacity.
     */
    private static int tableSizeFor(final int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= (1 << 30)) ? (1 << 30) : n + 1;
    }
}

