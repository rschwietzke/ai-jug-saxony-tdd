package org.jugsaxony.tdd.demo2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test-first specification of {@link TDDHashMap}: an open-addressing hash map
 * (probing instead of separate chaining), unbound capacity, no null keys,
 * null values permitted, not thread-safe.
 *
 * <p>Written ahead of the implementation (TDD red phase).</p>
 */
class TDDHashMapTest
{
    /**
     * A key with a forced hash code so collisions can be created
     * deterministically. Equality is based on the id only.
     */
    private static final class CollidingKey
    {
        private final String id;
        private final int hash;

        private CollidingKey(final String id, final int hash)
        {
            this.id = id;
            this.hash = hash;
        }

        @Override
        public int hashCode()
        {
            return hash;
        }

        @Override
        public boolean equals(final Object obj)
        {
            return obj instanceof CollidingKey other && id.equals(other.id);
        }

        @Override
        public String toString()
        {
            return id + "@" + hash;
        }
    }

    @Nested
    @DisplayName("a new map")
    class NewMap
    {
        @Test
        @DisplayName("is empty")
        void isEmpty()
        {
            var map = new TDDHashMap<String, Integer>();

            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("returns null for get of an unknown key")
        void getUnknownKeyReturnsNull()
        {
            var map = new TDDHashMap<String, Integer>();

            assertNull(map.get("missing"));
        }

        @Test
        @DisplayName("returns null for remove of an unknown key")
        void removeUnknownKeyReturnsNull()
        {
            var map = new TDDHashMap<String, Integer>();

            assertNull(map.remove("missing"));
        }

        @Test
        @DisplayName("has no keys and no values")
        void keysAndValuesAreEmpty()
        {
            var map = new TDDHashMap<String, Integer>();

            assertTrue(map.keys().isEmpty());
            assertTrue(map.values().isEmpty());
        }
    }

    @Nested
    @DisplayName("put and get")
    class PutAndGet
    {
        @Test
        @DisplayName("put returns null when the key is new")
        void putNewKeyReturnsNull()
        {
            var map = new TDDHashMap<String, Integer>();

            assertNull(map.put("one", 1));
        }

        @Test
        @DisplayName("a stored value can be read back")
        void storedValueCanBeReadBack()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("one", 1);

            assertEquals(Integer.valueOf(1), map.get("one"));
        }

        @Test
        @DisplayName("several entries can be stored and read back")
        void severalEntries()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("one", 1);
            map.put("two", 2);
            map.put("three", 3);

            assertEquals(3, map.size());
            assertEquals(Integer.valueOf(1), map.get("one"));
            assertEquals(Integer.valueOf(2), map.get("two"));
            assertEquals(Integer.valueOf(3), map.get("three"));
        }

        @Test
        @DisplayName("put replaces the value of an existing key and returns the old value")
        void putReplacesValue()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("key", 1);

            assertEquals(Integer.valueOf(1), map.put("key", 2));
            assertEquals(Integer.valueOf(2), map.get("key"));
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("keys that are equal but not identical address the same entry")
        void equalKeysAddressSameEntry()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put(new String("key"), 1); // NOPMD - intentionally a distinct instance

            assertEquals(Integer.valueOf(1), map.get("key"));
            assertEquals(Integer.valueOf(1), map.put("key", 2));
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("null values are permitted")
        void nullValuesArePermitted()
        {
            var map = new TDDHashMap<String, Integer>();

            assertNull(map.put("key", null));
            assertEquals(1, map.size());
            assertNull(map.get("key"));
            assertTrue(map.keys().contains("key"));
            assertEquals(1, map.values().stream().filter(Objects::isNull).count());
        }

        @Test
        @DisplayName("a null value can be replaced by a non-null value")
        void nullValueCanBeReplaced()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("key", null);

            assertNull(map.put("key", 42));
            assertEquals(Integer.valueOf(42), map.get("key"));
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("works with different type parameters")
        void worksWithDifferentTypes()
        {
            var intToString = new TDDHashMap<Integer, String>();
            intToString.put(1, "one");
            assertEquals("one", intToString.get(1));

            var longToBool = new TDDHashMap<Long, Boolean>();
            longToBool.put(7L, Boolean.TRUE);
            assertEquals(Boolean.TRUE, longToBool.get(7L));
        }
    }

    @Nested
    @DisplayName("null keys are rejected")
    class NullKeys
    {
        @Test
        @DisplayName("get(null) throws NullPointerException")
        void getNull()
        {
            var map = new TDDHashMap<String, Integer>();

            assertThrows(NullPointerException.class, () -> map.get(null));
        }

        @Test
        @DisplayName("put(null, value) throws NullPointerException")
        void putNull()
        {
            var map = new TDDHashMap<String, Integer>();

            assertThrows(NullPointerException.class, () -> map.put(null, 1));
        }

        @Test
        @DisplayName("remove(null) throws NullPointerException")
        void removeNull()
        {
            var map = new TDDHashMap<String, Integer>();

            assertThrows(NullPointerException.class, () -> map.remove(null));
        }

        @Test
        @DisplayName("null keys are also rejected when the map is populated")
        void nullKeysRejectedWhenPopulated()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("key", 1);

            assertThrows(NullPointerException.class, () -> map.get(null));
            assertThrows(NullPointerException.class, () -> map.put(null, 2));
            assertThrows(NullPointerException.class, () -> map.remove(null));
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove
    {
        @Test
        @DisplayName("returns the removed value")
        void removeReturnsValue()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("key", 42);

            assertEquals(Integer.valueOf(42), map.remove("key"));
        }

        @Test
        @DisplayName("the key is gone afterwards")
        void keyIsGoneAfterRemove()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("key", 42);
            map.remove("key");

            assertNull(map.get("key"));
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("removing the same key twice returns null the second time")
        void removeTwice()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("key", 42);

            assertEquals(Integer.valueOf(42), map.remove("key"));
            assertNull(map.remove("key"));
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("only the given key is removed")
        void onlyGivenKeyIsRemoved()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);

            map.remove("b");

            assertEquals(2, map.size());
            assertEquals(Integer.valueOf(1), map.get("a"));
            assertNull(map.get("b"));
            assertEquals(Integer.valueOf(3), map.get("c"));
        }

        @Test
        @DisplayName("a key can be reinserted after removal")
        void reinsertAfterRemove()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("key", 1);
            map.remove("key");

            assertNull(map.put("key", 2));
            assertEquals(Integer.valueOf(2), map.get("key"));
            assertEquals(1, map.size());
        }
    }

    @Nested
    @DisplayName("keys() and values()")
    class KeysAndValues
    {
        @Test
        @DisplayName("keys() contains exactly the stored keys, without duplicates")
        void keysContainsStoredKeys()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);

            assertEquals(new HashSet<>(List.of("a", "b", "c")), new HashSet<>(map.keys()));
            assertEquals(3, map.keys().size());
        }

        @Test
        @DisplayName("values() contains exactly the stored values, including duplicates")
        void valuesContainsStoredValues()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 2);

            var values = new ArrayList<>(map.values());
            Collections.sort(values);
            assertEquals(List.of(1, 2, 2), values);
        }

        @Test
        @DisplayName("keys() and values() reflect removals")
        void keysAndValuesReflectRemovals()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("a", 1);
            map.put("b", 2);
            map.remove("a");

            assertEquals(List.of("b"), map.keys());
            assertEquals(List.of(2), map.values());
        }

        @Test
        @DisplayName("the returned lists are snapshots; later map changes do not leak into them")
        void returnedListsAreSnapshots()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("a", 1);

            var keys = map.keys();
            var values = map.values();

            map.put("b", 2);
            map.remove("a");

            assertEquals(List.of("a"), keys);
            assertEquals(List.of(1), values);
        }
    }

    @Nested
    @DisplayName("collision handling")
    class Collisions
    {
        @Test
        @DisplayName("keys with identical hash codes are all found")
        void collidingKeysAreAllFound()
        {
            var map = new TDDHashMap<CollidingKey, String>();
            var a = new CollidingKey("a", 42);
            var b = new CollidingKey("b", 42);
            var c = new CollidingKey("c", 42);

            map.put(a, "A");
            map.put(b, "B");
            map.put(c, "C");

            assertEquals(3, map.size());
            assertEquals("A", map.get(a));
            assertEquals("B", map.get(b));
            assertEquals("C", map.get(c));
        }

        @Test
        @DisplayName("a colliding but equal key replaces instead of adding")
        void collidingEqualKeyReplaces()
        {
            var map = new TDDHashMap<CollidingKey, String>();
            map.put(new CollidingKey("a", 42), "A");
            map.put(new CollidingKey("b", 42), "B");

            assertEquals("A", map.put(new CollidingKey("a", 42), "A2"));
            assertEquals(2, map.size());
            assertEquals("A2", map.get(new CollidingKey("a", 42)));
        }

        @Test
        @DisplayName("removing the first key of a collision cluster keeps the rest reachable")
        void removeFirstOfCluster()
        {
            var map = new TDDHashMap<CollidingKey, Integer>();
            var a = new CollidingKey("a", 42);
            var b = new CollidingKey("b", 42);
            var c = new CollidingKey("c", 42);
            map.put(a, 1);
            map.put(b, 2);
            map.put(c, 3);

            assertEquals(Integer.valueOf(1), map.remove(a));

            assertEquals(2, map.size());
            assertNull(map.get(a));
            assertEquals(Integer.valueOf(2), map.get(b));
            assertEquals(Integer.valueOf(3), map.get(c));
        }

        @Test
        @DisplayName("removing the middle key of a collision cluster keeps the rest reachable")
        void removeMiddleOfCluster()
        {
            var map = new TDDHashMap<CollidingKey, Integer>();
            var a = new CollidingKey("a", 42);
            var b = new CollidingKey("b", 42);
            var c = new CollidingKey("c", 42);
            map.put(a, 1);
            map.put(b, 2);
            map.put(c, 3);

            assertEquals(Integer.valueOf(2), map.remove(b));

            assertEquals(2, map.size());
            assertEquals(Integer.valueOf(1), map.get(a));
            assertNull(map.get(b));
            assertEquals(Integer.valueOf(3), map.get(c));
        }

        @Test
        @DisplayName("negative hash codes are handled")
        void negativeHashCodes()
        {
            var map = new TDDHashMap<CollidingKey, String>();
            var a = new CollidingKey("a", -1);
            var b = new CollidingKey("b", Integer.MIN_VALUE);
            map.put(a, "A");
            map.put(b, "B");

            assertEquals(2, map.size());
            assertEquals("A", map.get(a));
            assertEquals("B", map.get(b));
        }

        @Test
        @DisplayName("many colliding keys with interleaved puts and removes stay consistent")
        void manyCollidingKeysStayConsistent()
        {
            var map = new TDDHashMap<CollidingKey, Integer>();
            var keys = new ArrayList<CollidingKey>();
            for (int i = 0; i < 100; i++)
            {
                keys.add(new CollidingKey("key-" + i, 7));
                map.put(keys.get(i), i);
            }
            assertEquals(100, map.size());

            // remove every second key, leaving a cluster full of gaps
            for (int i = 0; i < 100; i += 2)
            {
                assertEquals(Integer.valueOf(i), map.remove(keys.get(i)));
            }
            assertEquals(50, map.size());

            // removed keys are gone, the rest is intact
            for (int i = 0; i < 100; i++)
            {
                if (i % 2 == 0)
                {
                    assertNull(map.get(keys.get(i)));
                }
                else
                {
                    assertEquals(Integer.valueOf(i), map.get(keys.get(i)));
                }
            }

            // reinsertion into the broken-up cluster works
            for (int i = 0; i < 100; i += 2)
            {
                assertNull(map.put(keys.get(i), i * 10));
            }
            assertEquals(100, map.size());
            for (int i = 0; i < 100; i++)
            {
                assertEquals(Integer.valueOf(i % 2 == 0 ? i * 10 : i), map.get(keys.get(i)));
            }
        }
    }

    @Nested
    @DisplayName("unbound capacity")
    class Growth
    {
        @Test
        @DisplayName("grows transparently: 10_000 entries stay retrievable")
        void growsTransparently()
        {
            var map = new TDDHashMap<Integer, String>();
            for (int i = 0; i < 10_000; i++)
            {
                map.put(i, "v" + i);
            }

            assertEquals(10_000, map.size());
            for (int i = 0; i < 10_000; i++)
            {
                assertEquals("v" + i, map.get(i));
            }
        }

        @Test
        @DisplayName("growing preserves entries with colliding keys")
        void growingPreservesCollidingEntries()
        {
            var map = new TDDHashMap<CollidingKey, Integer>();
            var a = new CollidingKey("a", 13);
            var b = new CollidingKey("b", 13);
            var c = new CollidingKey("c", 13);
            map.put(a, 1);
            map.put(b, 2);
            map.put(c, 3);

            // flood the map with spread-out hashes to force several resize rounds
            for (int i = 0; i < 1_000; i++)
            {
                map.put(new CollidingKey("flood-" + i, i), -i);
            }

            assertEquals(1_003, map.size());
            assertEquals(Integer.valueOf(1), map.get(a));
            assertEquals(Integer.valueOf(2), map.get(b));
            assertEquals(Integer.valueOf(3), map.get(c));
        }

        @Test
        @DisplayName("behaves like a reference map under deterministic mixed load")
        void behavesLikeReferenceMap()
        {
            var reference = new HashMap<Integer, String>();
            var map = new TDDHashMap<Integer, String>();
            var random = new Random(42); // fixed seed -> deterministic

            for (int i = 0; i < 20_000; i++)
            {
                var key = random.nextInt(2_000);
                switch (random.nextInt(3))
                {
                    case 0, 1 ->
                    {
                        var value = "value-" + i;
                        assertEquals(reference.put(key, value), map.put(key, value));
                    }
                    case 2 ->
                    {
                        if (random.nextBoolean())
                        {
                            assertEquals(reference.remove(key), map.remove(key));
                        }
                        else
                        {
                            assertEquals(reference.get(key), map.get(key));
                        }
                    }
                    default -> throw new AssertionError("unreachable");
                }
            }

            assertEquals(reference.size(), map.size());
            assertEquals(new HashSet<>(reference.keySet()), new HashSet<>(map.keys()));
            reference.forEach((key, value) -> assertEquals(value, map.get(key)));
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear
    {
        @Test
        @DisplayName("removes all entries")
        void clearRemovesAllEntries()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("a", 1);
            map.put("b", 2);

            map.clear();

            assertEquals(0, map.size());
            assertNull(map.get("a"));
            assertNull(map.get("b"));
            assertTrue(map.keys().isEmpty());
            assertTrue(map.values().isEmpty());
        }

        @Test
        @DisplayName("on an empty map is a no-op")
        void clearOnEmptyMap()
        {
            var map = new TDDHashMap<String, Integer>();

            map.clear();

            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("the map is fully usable afterwards")
        void mapUsableAfterClear()
        {
            var map = new TDDHashMap<String, Integer>();
            map.put("a", 1);
            map.clear();

            assertNull(map.put("a", 2));
            assertEquals(Integer.valueOf(2), map.get("a"));
            assertEquals(1, map.size());
        }
    }
}
