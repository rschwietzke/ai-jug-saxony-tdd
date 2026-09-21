package org.jugsaxony.tdd.demo6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Volume and randomness: growth well past any sensible default capacity, and a
 * differential test against {@link java.util.HashMap} as the reference
 * implementation.
 */
@DisplayName("TDDHashMap - growth and randomized behaviour")
class TDDHashMapStressTest
{
    private static final int MANY = 200_000;

    @Nested
    @DisplayName("unbound capacity")
    class Growth
    {
        @Test
        @DisplayName("holds 200k entries and finds every one of them")
        void holdsManyEntries()
        {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();

            for (int i = 0; i < MANY; i++)
            {
                map.put(i, "v" + i);
            }

            assertEquals(MANY, map.size());

            for (int i = 0; i < MANY; i++)
            {
                assertEquals("v" + i, map.get(i), "key " + i);
            }
        }

        @Test
        @DisplayName("reports no false hits after growing")
        void noFalseHitsAfterGrowth()
        {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();

            for (int i = 0; i < 100_000; i++)
            {
                map.put(i, "v" + i);
            }

            for (int i = 100_000; i < 100_500; i++)
            {
                assertNull(map.get(i), "key " + i + " was never inserted");
            }
        }

        @Test
        @DisplayName("removes half of a large map and keeps the other half intact")
        void removesHalf()
        {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();

            for (int i = 0; i < MANY; i++)
            {
                map.put(i, "v" + i);
            }

            for (int i = 0; i < MANY; i += 2)
            {
                assertEquals("v" + i, map.remove(i), "removing key " + i);
            }

            assertEquals(MANY / 2, map.size());

            for (int i = 0; i < MANY; i++)
            {
                if (i % 2 == 0)
                {
                    assertNull(map.get(i), "removed key " + i);
                }
                else
                {
                    assertEquals("v" + i, map.get(i), "surviving key " + i);
                }
            }
        }

        @Test
        @DisplayName("keys() and values() stay complete at scale")
        void listsStayCompleteAtScale()
        {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();

            for (int i = 0; i < 50_000; i++)
            {
                map.put(i, "v" + i);
            }

            final List<Integer> keys = map.keys();

            assertEquals(50_000, keys.size(), "no duplicates, no losses");
            assertEquals(50_000, new HashSet<>(keys).size(), "every key distinct");
            assertEquals(50_000, map.values().size());
        }

        @Test
        @DisplayName("can be filled, cleared and filled again")
        void refillAfterClear()
        {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();

            for (int i = 0; i < 50_000; i++)
            {
                map.put(i, "first" + i);
            }

            map.clear();

            assertEquals(0, map.size());
            assertNull(map.get(17));

            for (int i = 0; i < 50_000; i++)
            {
                assertNull(map.put(i, "second" + i), "key " + i + " must look new after clear");
            }

            assertEquals(50_000, map.size());
            assertEquals("second17", map.get(17));
        }

        @Test
        @DisplayName("grows and shrinks repeatedly without losing entries")
        void repeatedGrowAndShrink()
        {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();

            for (int round = 0; round < 5; round++)
            {
                for (int i = 0; i < 20_000; i++)
                {
                    map.put(i, "r" + round + "-v" + i);
                }

                assertEquals(20_000, map.size(), "round " + round);

                for (int i = 0; i < 20_000; i++)
                {
                    map.remove(i);
                }

                assertEquals(0, map.size(), "round " + round);
            }
        }
    }

    @Nested
    @DisplayName("against java.util.HashMap")
    class Differential
    {
        @Test
        @DisplayName("100k random operations produce identical results")
        void randomOperationsMatchReference()
        {
            final TDDHashMap<Integer, String> actual = new TDDHashMap<>();
            final Map<Integer, String> expected = new HashMap<>();

            final Random random = new Random(20260921L);

            final int keySpace = 500;

            for (int op = 0; op < 100_000; op++)
            {
                final int step = op;
                final int key = random.nextInt(keySpace);
                final int choice = random.nextInt(100);

                if (choice < 55)
                {
                    // Every twentieth put stores a null value.
                    final String value = random.nextInt(20) == 0 ? null : "v" + step;

                    assertEquals(expected.put(key, value), actual.put(key, value),
                                 () -> "put at operation " + step + " for key " + key);
                }
                else if (choice < 85)
                {
                    assertEquals(expected.get(key), actual.get(key),
                                 () -> "get at operation " + step + " for key " + key);
                }
                else
                {
                    assertEquals(expected.remove(key), actual.remove(key),
                                 () -> "remove at operation " + step + " for key " + key);
                }

                assertEquals(expected.size(), actual.size(), () -> "size after operation " + step);
            }

            assertEquals(expected.keySet(), new HashSet<>(actual.keys()));
            assertEquals(bag(new ArrayList<>(expected.values())), bag(actual.values()));
        }

        @Test
        @DisplayName("random operations on colliding keys match the reference")
        void randomOperationsOnCollidingKeys()
        {
            final TDDHashMap<CollidingKey, String> actual = new TDDHashMap<>();
            final Map<CollidingKey, String> expected = new HashMap<>();

            final List<CollidingKey> keys = CollidingKey.colliding(200);

            final Random random = new Random(4711L);

            for (int op = 0; op < 50_000; op++)
            {
                final int step = op;
                final CollidingKey key = keys.get(random.nextInt(keys.size()));
                final int choice = random.nextInt(100);

                if (choice < 50)
                {
                    final String value = "v" + step;

                    assertEquals(expected.put(key, value), actual.put(key, value),
                                 () -> "put at operation " + step);
                }
                else if (choice < 80)
                {
                    assertEquals(expected.get(key), actual.get(key),
                                 () -> "get at operation " + step);
                }
                else
                {
                    assertEquals(expected.remove(key), actual.remove(key),
                                 () -> "remove at operation " + step);
                }

                assertEquals(expected.size(), actual.size(), () -> "size after operation " + step);
            }

            assertEquals(expected.keySet(), new HashSet<>(actual.keys()));
        }

        @Test
        @DisplayName("a clear in the middle of a random run keeps both in step")
        void randomRunWithClears()
        {
            final TDDHashMap<Integer, String> actual = new TDDHashMap<>();
            final Map<Integer, String> expected = new HashMap<>();

            final Random random = new Random(1234L);

            for (int op = 0; op < 20_000; op++)
            {
                final int step = op;

                if (op % 5_000 == 4_999)
                {
                    actual.clear();
                    expected.clear();
                }
                else
                {
                    final int key = random.nextInt(300);

                    if (random.nextBoolean())
                    {
                        final String value = "v" + step;
                        assertEquals(expected.put(key, value), actual.put(key, value));
                    }
                    else
                    {
                        assertEquals(expected.remove(key), actual.remove(key));
                    }
                }

                assertEquals(expected.size(), actual.size(), () -> "size after operation " + step);
            }

            assertEquals(expected.keySet(), new HashSet<>(actual.keys()));
        }
    }

    @Test
    @DisplayName("keys inserted in a hostile order are all retrievable")
    void hostileInsertionOrder()
    {
        // Sequential integers hash to themselves, so a power-of-two table sees long
        // runs of neighbouring slots. Stepping by the table size makes it worse.
        final TDDHashMap<Integer, String> map = new TDDHashMap<>();

        final List<Integer> inserted = new ArrayList<>();

        for (int i = 0; i < 20_000; i++)
        {
            final int key = i * 1_024;

            map.put(key, "v" + key);
            inserted.add(key);
        }

        assertEquals(20_000, map.size());

        for (final Integer key : inserted)
        {
            assertEquals("v" + key, map.get(key), "key " + key);
        }

        assertTrue(map.keys().containsAll(inserted));
    }

    private static <T> Map<T, Integer> bag(final List<T> items)
    {
        final Map<T, Integer> counts = new HashMap<>();

        for (final T item : items)
        {
            counts.merge(item, 1, Integer::sum);
        }

        return counts;
    }
}
