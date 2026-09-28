package org.jugsaxony.tdd.demo5;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Open hashing map: collisions are resolved by linear probing, not chaining,
 * and no wrapper objects are stored. Keys and values live in two parallel
 * arrays that grow on demand. A single sentinel marks deleted slots
 * (tombstones) so probes survive removals.
 *
 * <p>Not thread-safe. Null keys are rejected with {@link NullPointerException},
 * null values are allowed.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class TDDHashMap<K, V> {

    private static final Object TOMBSTONE = new Object();
    private static final int INITIAL_CAPACITY = 16;

    private Object[] keys;
    private Object[] values;
    private int size;
    private int tombstones;

    public TDDHashMap() {
        this.keys = new Object[INITIAL_CAPACITY];
        this.values = new Object[INITIAL_CAPACITY];
    }

    @SuppressWarnings("unchecked")
    public V get(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        final int mask = keys.length - 1;
        int index = spread(key.hashCode()) & mask;
        while (keys[index] != null) {
            final Object candidate = keys[index];
            if (candidate != TOMBSTONE && key.equals(candidate)) {
                return (V) values[index];
            }
            index = (index + 1) & mask;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public V put(final K key, final V value) {
        Objects.requireNonNull(key, "key must not be null");
        ensureCapacity();
        final int mask = keys.length - 1;
        int index = spread(key.hashCode()) & mask;
        int firstTombstone = -1;
        while (keys[index] != null) {
            final Object candidate = keys[index];
            if (candidate == TOMBSTONE) {
                if (firstTombstone < 0) {
                    firstTombstone = index;
                }
            } else if (key.equals(candidate)) {
                final Object old = values[index];
                values[index] = value;
                return (V) old;
            }
            index = (index + 1) & mask;
        }
        final int slot = firstTombstone >= 0 ? firstTombstone : index;
        if (firstTombstone >= 0) {
            tombstones--;
        }
        keys[slot] = key;
        values[slot] = value;
        size++;
        return null;
    }

    @SuppressWarnings("unchecked")
    public V remove(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        final int mask = keys.length - 1;
        int index = spread(key.hashCode()) & mask;
        while (keys[index] != null) {
            final Object candidate = keys[index];
            if (candidate != TOMBSTONE && key.equals(candidate)) {
                final Object old = values[index];
                keys[index] = TOMBSTONE;
                values[index] = null;
                size--;
                tombstones++;
                return (V) old;
            }
            index = (index + 1) & mask;
        }
        return null;
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    public List<K> keys() {
        final List<K> result = new ArrayList<>(size);
        for (final Object key : keys) {
            if (key != null && key != TOMBSTONE) {
                result.add((K) key);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<V> values() {
        final List<V> result = new ArrayList<>(size);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i] != TOMBSTONE) {
                result.add((V) values[i]);
            }
        }
        return result;
    }

    public void clear() {
        this.keys = new Object[INITIAL_CAPACITY];
        this.values = new Object[INITIAL_CAPACITY];
        this.size = 0;
        this.tombstones = 0;
    }

    private static int spread(final int hash) {
        return hash ^ (hash >>> 16);
    }

    private void ensureCapacity() {
        if ((size + tombstones) * 2 >= keys.length) {
            resize();
        }
    }

    private void resize() {
        final Object[] oldKeys = keys;
        final Object[] oldValues = values;
        this.keys = new Object[oldKeys.length * 2];
        this.values = new Object[oldValues.length * 2];
        this.size = 0;
        this.tombstones = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            final Object key = oldKeys[i];
            if (key != null && key != TOMBSTONE) {
                insertRaw(key, oldValues[i]);
                size++;
            }
        }
    }

    private void insertRaw(final Object key, final Object value) {
        final int mask = keys.length - 1;
        int index = spread(key.hashCode()) & mask;
        while (keys[index] != null) {
            index = (index + 1) & mask;
        }
        keys[index] = key;
        values[index] = value;
    }
}
