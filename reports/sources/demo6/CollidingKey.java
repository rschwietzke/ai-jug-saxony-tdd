package org.jugsaxony.tdd.demo6;

import java.util.ArrayList;
import java.util.List;

/**
 * A key with a hash code we control, so collision behaviour can be tested on purpose
 * instead of by luck.
 *
 * <p>Identity comes from {@code id} alone. Two keys can therefore share a hash code
 * and still be different keys, which is exactly the situation an open-addressing map
 * has to survive.
 */
final class CollidingKey
{
    /** The hash code every {@link #of(String)} key reports. Arbitrary, just constant. */
    static final int SHARED_HASH = 42;

    private final String id;

    private final int hash;

    private CollidingKey(final String id, final int hash)
    {
        this.id = id;
        this.hash = hash;
    }

    /**
     * @return a key that collides with every other key built by this method
     */
    static CollidingKey of(final String id)
    {
        return new CollidingKey(id, SHARED_HASH);
    }

    /**
     * @return a key with a hash code of your choosing
     */
    static CollidingKey of(final String id, final int hash)
    {
        return new CollidingKey(id, hash);
    }

    /**
     * @return {@code count} mutually colliding keys named {@code k0..k(count-1)}
     */
    static List<CollidingKey> colliding(final int count)
    {
        final List<CollidingKey> keys = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            keys.add(of("k" + i));
        }

        return keys;
    }

    @Override
    public int hashCode()
    {
        return hash;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof CollidingKey that))
        {
            return false;
        }

        return id.equals(that.id);
    }

    @Override
    public String toString()
    {
        return "CollidingKey[" + id + ", hash=" + hash + "]";
    }
}
