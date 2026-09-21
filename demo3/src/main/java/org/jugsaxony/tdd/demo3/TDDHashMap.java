package org.jugsaxony.tdd.demo3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A non-thread-safe, open-addressed hash map using linear probing.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class TDDHashMap<K, V> {
    private static final int INITIAL_CAPACITY = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;
    private static final byte DELETED = 2;

    private Object[] keys;
    private Object[] values;
    private byte[] states;
    private int size;
    private int usedSlots;
    private int resizeThreshold;

    public TDDHashMap() {
        keys = new Object[INITIAL_CAPACITY];
        values = new Object[INITIAL_CAPACITY];
        states = new byte[INITIAL_CAPACITY];
        resizeThreshold = resizeThreshold(INITIAL_CAPACITY);
    }

    public V get(final K key) {
        requireKey(key);
        int index = findKey(key);
        return index < 0 ? null : valueAt(index);
    }

    public V put(final K key, final V value) {
        requireKey(key);

        int index = findKey(key);
        if (index >= 0) {
            V previousValue = valueAt(index);
            values[index] = value;
            return previousValue;
        }

        index = findInsertionSlot(key);
        if (size + 1 > resizeThreshold) {
            resize(grownCapacity());
            index = findInsertionSlot(key);
        } else if (states[index] == EMPTY && usedSlots + 1 > resizeThreshold) {
            resize(keys.length);
            index = findInsertionSlot(key);
        }

        if (states[index] == EMPTY) {
            usedSlots++;
        }
        keys[index] = key;
        values[index] = value;
        states[index] = OCCUPIED;
        size++;
        return null;
    }

    public V remove(final K key) {
        requireKey(key);
        int index = findKey(key);
        if (index < 0) {
            return null;
        }

        V previousValue = valueAt(index);
        keys[index] = null;
        values[index] = null;
        states[index] = DELETED;
        size--;

        if (size == 0) {
            Arrays.fill(states, EMPTY);
            usedSlots = 0;
        }
        return previousValue;
    }

    public int size() {
        return size;
    }

    public List<K> keys() {
        List<K> result = new ArrayList<>(size);
        for (int index = 0; index < states.length; index++) {
            if (states[index] == OCCUPIED) {
                result.add(keyAt(index));
            }
        }
        return result;
    }

    public List<V> values() {
        List<V> result = new ArrayList<>(size);
        for (int index = 0; index < states.length; index++) {
            if (states[index] == OCCUPIED) {
                result.add(valueAt(index));
            }
        }
        return result;
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        Arrays.fill(states, EMPTY);
        size = 0;
        usedSlots = 0;
    }

    private int findKey(Object key) {
        int index = initialIndex(key);
        while (states[index] != EMPTY) {
            if (states[index] == OCCUPIED && key.equals(keys[index])) {
                return index;
            }
            index = nextIndex(index);
        }
        return -1;
    }

    private int findInsertionSlot(Object key) {
        int index = initialIndex(key);
        int firstDeleted = -1;
        while (states[index] != EMPTY) {
            if (firstDeleted < 0 && states[index] == DELETED) {
                firstDeleted = index;
            }
            index = nextIndex(index);
        }
        return firstDeleted >= 0 ? firstDeleted : index;
    }

    private int initialIndex(Object key) {
        int hash = key.hashCode();
        hash ^= hash >>> 16;
        return hash & (keys.length - 1);
    }

    private int nextIndex(int index) {
        return (index + 1) & (keys.length - 1);
    }

    private int grownCapacity() {
        if (keys.length == MAXIMUM_CAPACITY) {
            throw new IllegalStateException("Maximum map capacity reached");
        }
        return keys.length << 1;
    }

    private void resize(int newCapacity) {
        Object[] previousKeys = keys;
        Object[] previousValues = values;
        byte[] previousStates = states;

        keys = new Object[newCapacity];
        values = new Object[newCapacity];
        states = new byte[newCapacity];
        usedSlots = 0;
        resizeThreshold = resizeThreshold(newCapacity);

        for (int index = 0; index < previousStates.length; index++) {
            if (previousStates[index] == OCCUPIED) {
                int insertionIndex = findInsertionSlot(previousKeys[index]);
                keys[insertionIndex] = previousKeys[index];
                values[insertionIndex] = previousValues[index];
                states[insertionIndex] = OCCUPIED;
                usedSlots++;
            }
        }
    }

    private static int resizeThreshold(int capacity) {
        if (capacity == MAXIMUM_CAPACITY) {
            return capacity - 1;
        }
        return capacity - capacity / 3;
    }

    private static void requireKey(Object key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
    }

    @SuppressWarnings("unchecked")
    private K keyAt(int index) {
        return (K) keys[index];
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int index) {
        return (V) values[index];
    }
}
