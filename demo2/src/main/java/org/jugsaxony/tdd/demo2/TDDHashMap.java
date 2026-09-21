package org.jugsaxony.tdd.demo2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A hash map based on open addressing: collisions are resolved by linear
 * probing inside one flat backing structure, not by separate chaining.
 *
 * <p>Keys and values live in two parallel plain {@code Object} arrays; no
 * per-entry wrapper objects are allocated, so storing entries is
 * allocation-free beyond the backing structure itself.</p>
 *
 * <p>The map grows automatically (unbound capacity, power-of-two sizing,
 * load factor 0.75). Deletions use backward-shift compaction, keeping probe
 * clusters intact without tombstones. Null keys are rejected with
 * {@link NullPointerException}; null values are permitted. This
 * implementation is not thread-safe.</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@SuppressWarnings("unchecked")
public class TDDHashMap<K, V>
{
    /** Initial capacity of the backing arrays, always a power of two. */
    private static final int INITIAL_CAPACITY = 16;

    /** Maximum fill level before the backing arrays are doubled. */
    private static final float LOAD_FACTOR = 0.75f;

    private Object[] keys;
    private Object[] values;
    private int size;
    private int mask;
    private int threshold;

    /**
     * Creates an empty map.
     */
    public TDDHashMap()
    {
        keys = new Object[INITIAL_CAPACITY];
        values = new Object[INITIAL_CAPACITY];
        mask = INITIAL_CAPACITY - 1;
        threshold = (int) (INITIAL_CAPACITY * LOAD_FACTOR);
    }

    /**
     * Returns the value stored for the given key, or {@code null} if the key
     * is not present (a stored {@code null} value is indistinguishable from an
     * absent key through this method).
     *
     * @param key the key, must not be {@code null}
     * @return the stored value or {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public V get(final K key)
    {
        Objects.requireNonNull(key, "key must not be null");
        final var index = indexOf(key);
        return index < 0 ? null : (V) values[index];
    }

    /**
     * Stores the given value under the given key, replacing any existing
     * value.
     *
     * @param key the key, must not be {@code null}
     * @param value the value, may be {@code null}
     * @return the previously stored value, or {@code null} if the key was not present
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public V put(final K key, final V value)
    {
        Objects.requireNonNull(key, "key must not be null");
        if (size >= threshold)
        {
            grow();
        }

        var index = spread(key.hashCode()) & mask;
        while (true)
        {
            final var k = keys[index];
            if (k == null)
            {
                keys[index] = key;
                values[index] = value;
                size++;
                return null;
            }
            if (k.equals(key))
            {
                final var old = (V) values[index];
                values[index] = value;
                return old;
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * Removes the given key.
     *
     * @param key the key, must not be {@code null}
     * @return the removed value, or {@code null} if the key was not present
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public V remove(final K key)
    {
        Objects.requireNonNull(key, "key must not be null");
        final var index = indexOf(key);
        if (index < 0)
        {
            return null;
        }

        final var old = (V) values[index];
        shiftBackward(index);
        size--;
        return old;
    }

    /**
     * Returns the number of entries in this map.
     *
     * @return the entry count
     */
    public int size()
    {
        return size;
    }

    /**
     * Returns a snapshot of all keys. Order is unspecified.
     *
     * @return the keys
     */
    public List<K> keys()
    {
        final var result = new ArrayList<K>(size);
        for (final var k : keys)
        {
            if (k != null)
            {
                result.add((K) k);
            }
        }
        return result;
    }

    /**
     * Returns a snapshot of all values, including duplicates and nulls.
     * Order is unspecified.
     *
     * @return the values
     */
    public List<V> values()
    {
        final var result = new ArrayList<V>(size);
        for (var i = 0; i < keys.length; i++)
        {
            if (keys[i] != null)
            {
                result.add((V) values[i]);
            }
        }
        return result;
    }

    /**
     * Removes all entries. The map stays fully usable afterwards; the backing
     * structure is reused.
     */
    public void clear()
    {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        size = 0;
    }

    /**
     * Finds the slot holding the given key.
     *
     * @param key the key, non-null
     * @return the slot index, or {@code -1} if the key is not present
     */
    private int indexOf(final Object key)
    {
        var index = spread(key.hashCode()) & mask;
        while (true)
        {
            final var k = keys[index];
            if (k == null)
            {
                return -1;
            }
            if (k.equals(key))
            {
                return index;
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * Removes the entry at {@code removed} by shifting every following entry
     * of the same probe cluster one step back, until the cluster ends. This
     * keeps all other entries reachable without tombstones.
     *
     * @param removed the slot to clear
     */
    private void shiftBackward(final int removed)
    {
        var gap = removed;
        var index = removed;
        while (true)
        {
            index = (index + 1) & mask;
            final var k = keys[index];
            if (k == null)
            {
                keys[gap] = null;
                values[gap] = null;
                return;
            }
            // the entry probed past the gap if its home slot is not in the
            // cyclic range (gap, index]; then it has to move into the gap
            if (!inRange(gap, index, spread(k.hashCode()) & mask))
            {
                keys[gap] = k;
                values[gap] = values[index];
                gap = index;
            }
        }
    }

    /**
     * Tells whether {@code home} lies within the cyclic slot range
     * {@code (gap, index]}, i.e. the probe range that ends at {@code index}.
     */
    private static boolean inRange(final int gap, final int index, final int home)
    {
        return gap < index ? (gap < home && home <= index)
                           : (gap < home || home <= index);
    }

    /**
     * Doubles the backing arrays and reinserts all entries.
     */
    private void grow()
    {
        final var oldKeys = keys;
        final var oldValues = values;
        final var capacity = oldKeys.length << 1;

        keys = new Object[capacity];
        values = new Object[capacity];
        mask = capacity - 1;
        threshold = (int) (capacity * LOAD_FACTOR);

        for (var i = 0; i < oldKeys.length; i++)
        {
            final var k = oldKeys[i];
            if (k != null)
            {
                var index = spread(k.hashCode()) & mask;
                while (keys[index] != null)
                {
                    index = (index + 1) & mask;
                }
                keys[index] = k;
                values[index] = oldValues[i];
            }
        }
    }

    /**
     * Spreads the higher bits of a hash code into the lower bits so that
     * power-of-two masking stays well distributed.
     */
    private static int spread(final int hashCode)
    {
        return hashCode ^ (hashCode >>> 16);
    }
}
