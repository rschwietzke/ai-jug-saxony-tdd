package org.jugsaxony.tdd.demo6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Collisions, probe sequences and tombstones - where open addressing actually earns
 * its keep, and where it usually breaks.
 *
 * <p>Nothing here guesses at the probing scheme. The tests only demand that lookups
 * stay correct no matter how badly the keys pile up.
 */
@DisplayName("TDDHashMap - collisions")
class TDDHashMapCollisionTest
{
    private TDDHashMap<CollidingKey, String> map;

    @BeforeEach
    void createMap()
    {
        map = new TDDHashMap<>();
    }

    @Nested
    @DisplayName("keys sharing one hash code")
    class SharedHashCode
    {
        @Test
        @DisplayName("two colliding keys stay separate")
        void twoKeys()
        {
            final CollidingKey a = CollidingKey.of("a");
            final CollidingKey b = CollidingKey.of("b");

            map.put(a, "1");
            map.put(b, "2");

            assertEquals(2, map.size());
            assertEquals("1", map.get(a));
            assertEquals("2", map.get(b));
        }

        @Test
        @DisplayName("a thousand colliding keys are all retrievable")
        void aThousandKeys()
        {
            final List<CollidingKey> keys = CollidingKey.colliding(1_000);

            for (int i = 0; i < keys.size(); i++)
            {
                assertNull(map.put(keys.get(i), "v" + i), "key " + i + " should be new");
            }

            assertEquals(1_000, map.size());

            for (int i = 0; i < keys.size(); i++)
            {
                assertEquals("v" + i, map.get(keys.get(i)), "key " + i);
            }
        }

        @Test
        @DisplayName("a thousand colliding keys are all removable")
        void aThousandKeysRemoved()
        {
            final List<CollidingKey> keys = CollidingKey.colliding(1_000);

            for (int i = 0; i < keys.size(); i++)
            {
                map.put(keys.get(i), "v" + i);
            }

            for (int i = 0; i < keys.size(); i++)
            {
                assertEquals("v" + i, map.remove(keys.get(i)), "removing key " + i);
                assertEquals(999 - i, map.size());
            }

            assertEquals(0, map.size());
            assertTrue(map.keys().isEmpty());
        }

        @Test
        @DisplayName("a colliding key that is equal overwrites rather than duplicates")
        void equalKeysOverwrite()
        {
            map.put(CollidingKey.of("same"), "1");
            map.put(CollidingKey.of("same"), "2");

            assertEquals(1, map.size());
            assertEquals("2", map.get(CollidingKey.of("same")));
        }

        @Test
        @DisplayName("a miss among colliding keys returns null instead of looping forever")
        void missAmongCollisions()
        {
            final List<CollidingKey> keys = CollidingKey.colliding(100);

            for (final CollidingKey key : keys)
            {
                map.put(key, "v");
            }

            assertNull(map.get(CollidingKey.of("not-in-there")));
            assertNull(map.remove(CollidingKey.of("not-in-there")));
            assertEquals(100, map.size());
        }
    }

    @Nested
    @DisplayName("removing from a probe chain")
    class ProbeChains
    {
        @Test
        @DisplayName("removing the first key keeps the rest reachable")
        void removeHeadOfChain()
        {
            final CollidingKey a = CollidingKey.of("a");
            final CollidingKey b = CollidingKey.of("b");
            final CollidingKey c = CollidingKey.of("c");

            map.put(a, "1");
            map.put(b, "2");
            map.put(c, "3");

            map.remove(a);

            assertNull(map.get(a));
            assertEquals("2", map.get(b), "b sat behind a in the probe sequence");
            assertEquals("3", map.get(c), "c sat behind a in the probe sequence");
            assertEquals(2, map.size());
        }

        @Test
        @DisplayName("removing a middle key keeps the tail reachable")
        void removeMiddleOfChain()
        {
            final CollidingKey a = CollidingKey.of("a");
            final CollidingKey b = CollidingKey.of("b");
            final CollidingKey c = CollidingKey.of("c");

            map.put(a, "1");
            map.put(b, "2");
            map.put(c, "3");

            map.remove(b);

            assertEquals("1", map.get(a));
            assertNull(map.get(b));
            assertEquals("3", map.get(c), "the gap left by b must not hide c");
            assertEquals(2, map.size());
        }

        @Test
        @DisplayName("removing every other key of a long chain keeps the rest reachable")
        void removeAlternatingInLongChain()
        {
            final List<CollidingKey> keys = CollidingKey.colliding(200);

            for (int i = 0; i < keys.size(); i++)
            {
                map.put(keys.get(i), "v" + i);
            }

            for (int i = 0; i < keys.size(); i += 2)
            {
                map.remove(keys.get(i));
            }

            assertEquals(100, map.size());

            for (int i = 0; i < keys.size(); i++)
            {
                if (i % 2 == 0)
                {
                    assertNull(map.get(keys.get(i)), "removed key " + i);
                }
                else
                {
                    assertEquals("v" + i, map.get(keys.get(i)), "surviving key " + i);
                }
            }
        }

        @Test
        @DisplayName("a freed slot gets reused without creating a duplicate entry")
        void freedSlotIsReusedNotDuplicated()
        {
            final CollidingKey a = CollidingKey.of("a");
            final CollidingKey b = CollidingKey.of("b");

            map.put(a, "1");
            map.put(b, "2");

            map.remove(a);

            // b must be found in its existing slot, not written a second time into
            // the gap that a left behind. An implementation that duplicates b would
            // report no previous value here and end up with a size of 2.
            assertEquals("2", map.put(b, "2-updated"), "put must report b's previous value");
            assertEquals(1, map.size(), "b must exist exactly once");
            assertEquals("2-updated", map.get(b));

            map.remove(b);

            assertEquals(0, map.size());
            assertNull(map.get(b), "no stale copy of b may survive");
        }

        @Test
        @DisplayName("a removed key can be re-inserted into the gap it left")
        void reinsertIntoOwnGap()
        {
            final CollidingKey a = CollidingKey.of("a");
            final CollidingKey b = CollidingKey.of("b");

            map.put(a, "1");
            map.put(b, "2");
            map.remove(a);

            assertNull(map.put(a, "1-again"));

            assertEquals(2, map.size());
            assertEquals("1-again", map.get(a));
            assertEquals("2", map.get(b));
        }

        @Test
        @DisplayName("heavy churn on one chain stays consistent")
        void churnOnOneChain()
        {
            final List<CollidingKey> keys = CollidingKey.colliding(16);

            for (int round = 0; round < 2_000; round++)
            {
                final CollidingKey key = keys.get(round % keys.size());

                map.put(key, "round" + round);
                assertEquals("round" + round, map.get(key));

                map.remove(key);
                assertNull(map.get(key));
            }

            assertEquals(0, map.size());
        }
    }

    @Nested
    @DisplayName("awkward hash codes")
    class AwkwardHashCodes
    {
        @ParameterizedTest(name = "hashCode = {0}")
        @ValueSource(ints =
        {
            Integer.MIN_VALUE, -1_000_003, -17, -1, 0, 1, 17, 1_000_003, Integer.MAX_VALUE
        })
        @DisplayName("a single key with an extreme hash code round-trips")
        void extremeHashCode(final int hash)
        {
            final CollidingKey key = CollidingKey.of("k", hash);

            assertNull(map.put(key, "v"));
            assertEquals("v", map.get(key));
            assertEquals(1, map.size());
            assertEquals("v", map.remove(key));
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("Integer.MIN_VALUE does not break index computation")
        void minValueHash()
        {
            // Math.abs(Integer.MIN_VALUE) is still Integer.MIN_VALUE - the classic
            // source of a negative array index.
            for (int i = 0; i < 50; i++)
            {
                map.put(CollidingKey.of("k" + i, Integer.MIN_VALUE), "v" + i);
            }

            assertEquals(50, map.size());

            for (int i = 0; i < 50; i++)
            {
                assertEquals("v" + i, map.get(CollidingKey.of("k" + i, Integer.MIN_VALUE)));
            }
        }

        @Test
        @DisplayName("keys whose hashes differ only above the low bits still separate")
        void hashesDifferingOnlyInHighBits()
        {
            // With a power-of-two table and no bit spreading these all land in the
            // same bucket. Correctness must not depend on that.
            for (int i = 0; i < 64; i++)
            {
                map.put(CollidingKey.of("k" + i, i * 1_024), "v" + i);
            }

            assertEquals(64, map.size());

            for (int i = 0; i < 64; i++)
            {
                assertEquals("v" + i, map.get(CollidingKey.of("k" + i, i * 1_024)), "key " + i);
            }
        }

        @Test
        @DisplayName("mixed positive and negative hashes coexist")
        void mixedSigns()
        {
            for (int i = -100; i <= 100; i++)
            {
                map.put(CollidingKey.of("k" + i, i), "v" + i);
            }

            assertEquals(201, map.size());

            for (int i = -100; i <= 100; i++)
            {
                assertEquals("v" + i, map.get(CollidingKey.of("k" + i, i)), "key " + i);
            }
        }
    }

    @Test
    @DisplayName("a collision chain survives the growth that it triggers")
    void chainSurvivesResize()
    {
        final List<CollidingKey> keys = CollidingKey.colliding(5_000);

        for (int i = 0; i < keys.size(); i++)
        {
            map.put(keys.get(i), "v" + i);

            // Check an older key on every step - a rehash that drops or shadows an
            // entry shows up immediately.
            assertEquals("v0", map.get(keys.get(0)), "after inserting key " + i);
        }

        assertEquals(5_000, map.size());
        assertEquals(new HashSet<>(keys), new HashSet<>(map.keys()));
    }
}
