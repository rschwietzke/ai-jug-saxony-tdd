package org.jugsaxony.tdd.demo6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A hash map using open addressing (the task calls it "open hashing": collisions are
 * resolved inside the backing arrays, never by separate chaining).
 *
 * <p><b>Layout.</b> Two parallel arrays, one for keys and one for values. Slot
 * {@code i} holds a mapping exactly when {@code keys[i] != null}, which is why
 * {@code null} keys are rejected: {@code null} is the empty marker. Values are free to
 * be {@code null} because they are never consulted to decide whether a slot is taken.
 * There is no per-entry object, so nothing is allocated per mapping - only the two
 * arrays themselves.
 *
 * <p><b>Probing.</b> Linear probing over a power-of-two capacity. The table is kept at
 * most half full, so a probe always terminates on an empty slot.
 *
 * <p><b>Deletion.</b> Backward-shift deletion (Knuth, TAOCP vol. 3, algorithm 6.4R)
 * rather than tombstones. On removal the rest of the cluster is pulled back into the
 * gap, so the table is left exactly as if the removed key had never been inserted.
 * That buys two properties tombstones cannot: a lookup may stop at the first empty
 * slot, and repeated insert/remove at a constant size never degrades the table or
 * forces it to grow.
 *
 * <p><b>Contract.</b>
 * <ul>
 *   <li>Not thread-safe.</li>
 *   <li>Unbound capacity - the map grows as needed.</li>
 *   <li>{@code null} keys are rejected with {@link IllegalArgumentException}.</li>
 *   <li>{@code null} values are permitted and stored as real entries. A {@code null}
 *       from {@link #get(Object)} therefore does not mean the key is absent - only
 *       {@link #size()} and {@link #keys()} can tell those apart.</li>
 *   <li>{@link #keys()} and {@link #values()} return snapshots in unspecified order,
 *       with no positional correlation between the two lists.</li>
 * </ul>
 *
 * @param <K> key type, never {@code null}
 * @param <V> value type, may be {@code null}
 */
public class TDDHashMap<K, V>
{
    /** Power of two. Small enough to stay cheap for the many tiny maps in practice. */
    private static final int INITIAL_CAPACITY = 16;

    /** Largest power of two an {@code int}-indexed array can reach. */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * Occupied slots, or {@code null} where a slot is free. Length is always a power
     * of two.
     */
    private Object[] keys;

    /** Values, positionally aligned with {@link #keys}. May legitimately hold nulls. */
    private Object[] values;

    /** Number of mappings; also the number of non-null entries in {@link #keys}. */
    private int size;

    /**
     * Creates an empty map.
     */
    public TDDHashMap()
    {
        keys = new Object[INITIAL_CAPACITY];
        values = new Object[INITIAL_CAPACITY];
        size = 0;
    }

    /**
     * Returns the value mapped to {@code key}, or {@code null} when there is no
     * mapping. Because {@code null} values are legal, a {@code null} result does not
     * by itself mean the key is absent.
     *
     * @param key the key to look up, never {@code null}
     * @return the mapped value, possibly {@code null}
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public V get(final K key)
    {
        requireKey(key);

        final int slot = slotOf(key);

        return slot < 0 ? null : (V) values[slot];
    }

    /**
     * Associates {@code value} with {@code key}, replacing any previous mapping.
     *
     * @param key   the key, never {@code null}
     * @param value the value, may be {@code null}
     * @return the previously mapped value, or {@code null} if the key was absent
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public V put(final K key, final V value)
    {
        requireKey(key);

        final int hash = spread(key.hashCode());
        final int slot = slotOf(key, hash);

        if (slot >= 0)
        {
            final V previous = (V) values[slot];
            values[slot] = value;

            return previous;
        }

        // Not present. slotOf encoded the free slot it stopped on as ~slot, which is
        // a valid insertion point precisely because deletion leaves no tombstones.
        final int free;

        if (size + 1 > keys.length >> 1)
        {
            grow();
            free = freeSlotFor(hash);
        }
        else
        {
            free = ~slot;
        }

        keys[free] = key;
        values[free] = value;
        size++;

        return null;
    }

    /**
     * Removes the mapping for {@code key}, if any.
     *
     * @param key the key to remove, never {@code null}
     * @return the removed value, or {@code null} if the key was absent
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public V remove(final K key)
    {
        requireKey(key);

        final int slot = slotOf(key);

        if (slot < 0)
        {
            return null;
        }

        final V previous = (V) values[slot];

        closeGap(slot);
        size--;

        return previous;
    }

    /**
     * @return the number of mappings currently held
     */
    public int size()
    {
        return size;
    }

    /**
     * @return a snapshot of all keys, in unspecified order, each key exactly once
     */
    @SuppressWarnings("unchecked")
    public List<K> keys()
    {
        final List<K> snapshot = new ArrayList<>(size);

        for (int slot = 0; slot < keys.length; slot++)
        {
            if (keys[slot] != null)
            {
                snapshot.add((K) keys[slot]);
            }
        }

        return snapshot;
    }

    /**
     * @return a snapshot of all values, in unspecified order, one element per
     *         mapping - duplicates and {@code null} included
     */
    @SuppressWarnings("unchecked")
    public List<V> values()
    {
        final List<V> snapshot = new ArrayList<>(size);

        for (int slot = 0; slot < keys.length; slot++)
        {
            if (keys[slot] != null)
            {
                snapshot.add((V) values[slot]);
            }
        }

        return snapshot;
    }

    /**
     * Removes all mappings. The map stays usable afterwards and keeps the capacity it
     * has grown to.
     */
    public void clear()
    {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        size = 0;
    }

    /**
     * Convenience overload that computes the hash itself.
     *
     * @see #slotOf(Object, int)
     */
    private int slotOf(final K key)
    {
        return slotOf(key, spread(key.hashCode()));
    }

    /**
     * Probes for {@code key}.
     *
     * <p>Returns the slot holding the key, or - when the key is absent - the bitwise
     * complement of the free slot the probe stopped on, so a caller can insert there
     * without probing a second time. The complement is always negative, because slot
     * indices are not.
     *
     * @return {@code slot} if found, otherwise {@code ~freeSlot}
     */
    private int slotOf(final K key, final int hash)
    {
        final Object[] currentKeys = keys;
        final int mask = currentKeys.length - 1;

        int slot = hash & mask;

        while (true)
        {
            final Object candidate = currentKeys[slot];

            if (candidate == null)
            {
                return ~slot;
            }

            if (candidate == key || key.equals(candidate))
            {
                return slot;
            }

            slot = slot + 1 & mask;
        }
    }

    /**
     * Finds the first free slot on the probe path of {@code hash}. Only safe for a key
     * known to be absent, which is why it is used after {@link #grow()} alone.
     */
    private int freeSlotFor(final int hash)
    {
        final Object[] currentKeys = keys;
        final int mask = currentKeys.length - 1;

        int slot = hash & mask;

        while (currentKeys[slot] != null)
        {
            slot = slot + 1 & mask;
        }

        return slot;
    }

    /**
     * Removes the entry at {@code gap} and repairs the cluster behind it by shifting
     * back every following entry that the gap would otherwise cut off from its home
     * slot. Knuth's algorithm 6.4R.
     *
     * <p>This is what makes tombstones unnecessary: afterwards the table is
     * indistinguishable from one the removed key never entered.
     */
    private void closeGap(final int removed)
    {
        final Object[] currentKeys = keys;
        final Object[] currentValues = values;
        final int mask = currentKeys.length - 1;

        int gap = removed;

        while (true)
        {
            currentKeys[gap] = null;
            currentValues[gap] = null;

            int probe = gap;

            while (true)
            {
                probe = probe + 1 & mask;

                final Object candidate = currentKeys[probe];

                if (candidate == null)
                {
                    return;
                }

                final int home = spread(candidate.hashCode()) & mask;

                // Keep the candidate where it is while its home still lies in the
                // stretch (gap, probe] - the gap is then behind it and harmless.
                // Otherwise the gap sits between the candidate and its home, and a
                // lookup would stop short of it, so it has to move up.
                final boolean staysPut = gap <= probe
                                         ? home > gap && home <= probe
                                         : home > gap || home <= probe;

                if (!staysPut)
                {
                    break;
                }
            }

            currentKeys[gap] = currentKeys[probe];
            currentValues[gap] = currentValues[probe];

            gap = probe;
        }
    }

    /**
     * Doubles the capacity and reinserts every mapping. No duplicate check is needed
     * during the rebuild - the keys are already known to be distinct.
     */
    private void grow()
    {
        final Object[] oldKeys = keys;
        final Object[] oldValues = values;

        final int capacity = oldKeys.length << 1;

        if (capacity <= 0 || capacity > MAXIMUM_CAPACITY)
        {
            throw new IllegalStateException("TDDHashMap cannot grow beyond " + MAXIMUM_CAPACITY + " slots");
        }

        final Object[] newKeys = new Object[capacity];
        final Object[] newValues = new Object[capacity];
        final int mask = capacity - 1;

        for (int slot = 0; slot < oldKeys.length; slot++)
        {
            final Object key = oldKeys[slot];

            if (key == null)
            {
                continue;
            }

            int target = spread(key.hashCode()) & mask;

            while (newKeys[target] != null)
            {
                target = target + 1 & mask;
            }

            newKeys[target] = key;
            newValues[target] = oldValues[slot];
        }

        keys = newKeys;
        values = newValues;
    }

    /**
     * Scatters a hash code across the whole int range (the murmur3 32-bit finalizer).
     *
     * <p>Linear probing needs considerably more than {@link java.util.HashMap}'s
     * {@code h ^ (h >>> 16)}. That shift-xor is the identity for small integers, so
     * sequential keys would occupy sequential slots and merge into one enormous
     * cluster - harmless with separate chaining, but here it turns both probing and
     * backward-shift deletion into linear scans of the whole table. A full avalanche
     * keeps clusters short, which is what makes every operation constant time in
     * practice.
     */
    private static int spread(final int hash)
    {
        int mixed = hash;

        mixed ^= mixed >>> 16;
        mixed *= 0x85ebca6b;
        mixed ^= mixed >>> 13;
        mixed *= 0xc2b2ae35;
        mixed ^= mixed >>> 16;

        return mixed;
    }

    private static void requireKey(final Object key)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("null keys are not supported");
        }
    }
}
