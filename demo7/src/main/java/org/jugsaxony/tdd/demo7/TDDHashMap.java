package org.jugsaxony.tdd.demo7;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Open hashing (open addressing) map with linear probing.
 *
 * Storage uses two parallel arrays (keys and values) — no separate
 * chaining and no entry wrapper objects. Aside from the backing arrays
 * (including their reallocation on growth), no allocations are made
 * for storing entries.
 *
 * Deletion uses backward-shift (no tombstones), so probe chains never
 * degrade due to removed entries. Capacity is unbounded: the table
 * doubles whenever the load factor is exceeded.
 *
 * Not thread-safe. Null keys are rejected with a
 * {@link NullPointerException}, null values are permitted. An empty
 * key slot is represented by a null in the keys array, which is safe
 * because keys can never be null.
 */
public class TDDHashMap<K, V>
{
    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private Object[] keys;
    private Object[] values;
    private int size;
    private int threshold;

    public TDDHashMap()
    {
        this.keys = new Object[INITIAL_CAPACITY];
        this.values = new Object[INITIAL_CAPACITY];
        this.threshold = (int) ( INITIAL_CAPACITY * LOAD_FACTOR );
    }

    @SuppressWarnings( "unchecked" )
    public V get( final K key )
    {
        Objects.requireNonNull( key, "key must not be null" );

        final Object[] k = this.keys;
        final Object[] v = this.values;
        final int mask = k.length - 1;

        int i = hash( key ) & mask;
        while ( k[i] != null )
        {
            if ( k[i] == key || key.equals( k[i] ) )
            {
                return (V) v[i];
            }
            i = ( i + 1 ) & mask;
        }

        return null;
    }

    @SuppressWarnings( "unchecked" )
    public V put( final K key, final V value )
    {
        Objects.requireNonNull( key, "key must not be null" );

        final Object[] k = this.keys;
        final Object[] v = this.values;
        final int mask = k.length - 1;

        int i = hash( key ) & mask;
        while ( k[i] != null )
        {
            if ( k[i] == key || key.equals( k[i] ) )
            {
                final V previous = (V) v[i];
                v[i] = value;
                return previous;
            }
            i = ( i + 1 ) & mask;
        }

        k[i] = key;
        v[i] = value;
        this.size++;

        if ( this.size > this.threshold )
        {
            grow();
        }

        return null;
    }

    @SuppressWarnings( "unchecked" )
    public V remove( final K key )
    {
        Objects.requireNonNull( key, "key must not be null" );

        final Object[] k = this.keys;
        final Object[] v = this.values;
        final int mask = k.length - 1;

        int i = hash( key ) & mask;
        while ( k[i] != null )
        {
            if ( k[i] == key || key.equals( k[i] ) )
            {
                final V previous = (V) v[i];
                backwardShiftDelete( i, mask );
                this.size--;
                return previous;
            }
            i = ( i + 1 ) & mask;
        }

        return null;
    }

    public int size()
    {
        return this.size;
    }

    @SuppressWarnings( "unchecked" )
    public List<K> keys()
    {
        final List<K> result = new ArrayList<>( this.size );

        for ( final Object key : this.keys )
        {
            if ( key != null )
            {
                result.add( (K) key );
            }
        }

        return result;
    }

    @SuppressWarnings( "unchecked" )
    public List<V> values()
    {
        final List<V> result = new ArrayList<>( this.size );

        final Object[] k = this.keys;
        final Object[] v = this.values;
        for ( int i = 0; i < k.length; i++ )
        {
            if ( k[i] != null )
            {
                result.add( (V) v[i] );
            }
        }

        return result;
    }

    public void clear()
    {
        Arrays.fill( this.keys, null );
        Arrays.fill( this.values, null );
        this.size = 0;
    }

    /**
     * Removes the entry at {@code hole} by shifting subsequent entries
     * whose probe path crossed {@code hole} back by one slot. This keeps
     * every probe chain contiguous without tombstones.
     */
    private void backwardShiftDelete( int hole, final int mask )
    {
        final Object[] k = this.keys;
        final Object[] v = this.values;

        k[hole] = null;
        v[hole] = null;

        int j = hole;
        while ( true )
        {
            j = ( j + 1 ) & mask;
            if ( k[j] == null )
            {
                return;
            }

            final int natural = hash( k[j] ) & mask;

            // distance from the hole to the scanned slot and to the
            // natural slot of the entry found there (cyclic, forward)
            final int distanceToJ = ( j - hole ) & mask;
            final int distanceToNatural = ( natural - hole ) & mask;

            // the entry at j may stay only if its natural slot lies
            // cyclically within (hole, j]; otherwise its probe path
            // crossed the hole and it must move back
            if ( !( distanceToNatural > 0 && distanceToNatural <= distanceToJ ) )
            {
                k[hole] = k[j];
                v[hole] = v[j];
                k[j] = null;
                v[j] = null;
                hole = j;
            }
        }
    }

    private void grow()
    {
        final int newCapacity = this.keys.length << 1;

        final Object[] oldKeys = this.keys;
        final Object[] oldValues = this.values;
        final Object[] newKeys = new Object[newCapacity];
        final Object[] newValues = new Object[newCapacity];
        final int mask = newCapacity - 1;

        for ( int i = 0; i < oldKeys.length; i++ )
        {
            if ( oldKeys[i] != null )
            {
                int slot = hash( oldKeys[i] ) & mask;
                while ( newKeys[slot] != null )
                {
                    slot = ( slot + 1 ) & mask;
                }
                newKeys[slot] = oldKeys[i];
                newValues[slot] = oldValues[i];
            }
        }

        this.keys = newKeys;
        this.values = newValues;
        this.threshold = (int) ( newCapacity * LOAD_FACTOR );
    }

    /**
     * Spreads the key hash to reduce clustering with power-of-two
     * table sizes. Bit masking keeps this safe for every hashCode,
     * including {@link Integer#MIN_VALUE} and negative values.
     */
    private static int hash( final Object key )
    {
        final int h = key.hashCode();
        return h ^ ( h >>> 16 );
    }
}
