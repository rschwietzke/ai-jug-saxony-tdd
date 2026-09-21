package org.jugsaxony.tdd.demo5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link TDDHashMap}, an open hashing map
 * (collisions resolved by probing, no chaining, no wrappers).
 */
@DisplayName("TDDHashMap")
class TDDHashMapTest {

    /**
     * Keys that all hash to the same bucket, forcing heavy probing.
     */
    private static final class CollisionKey {
        private final int id;

        CollisionKey(final int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof CollisionKey other && other.id == this.id;
        }

        @Override
        public String toString() {
            return "CollisionKey(" + id + ")";
        }
    }

    @Nested
    @DisplayName("on an empty map")
    class EmptyMap {

        @Test
        @DisplayName("size() is 0")
        void sizeIsZero() {
            assertEquals(0, new TDDHashMap<String, String>().size());
        }

        @Test
        @DisplayName("get() returns null")
        void getReturnsNull() {
            assertNull(new TDDHashMap<String, String>().get("missing"));
        }

        @Test
        @DisplayName("remove() returns null")
        void removeReturnsNull() {
            assertNull(new TDDHashMap<String, String>().remove("missing"));
        }

        @Test
        @DisplayName("keys() is empty")
        void keysIsEmpty() {
            assertTrue(new TDDHashMap<String, String>().keys().isEmpty());
        }

        @Test
        @DisplayName("values() is empty")
        void valuesIsEmpty() {
            assertTrue(new TDDHashMap<String, String>().values().isEmpty());
        }

        @Test
        @DisplayName("clear() is a no-op")
        void clearIsNoOp() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.clear();
            assertEquals(0, map.size());
        }
    }

    @Nested
    @DisplayName("put")
    class Put {

        @Test
        @DisplayName("returns null for a new key")
        void putNewKeyReturnsNull() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            assertNull(map.put("key", "value"));
        }

        @Test
        @DisplayName("stores the value so get() returns it")
        void putThenGet() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("key", "value");
            assertEquals("value", map.get("key"));
        }

        @Test
        @DisplayName("overwrite returns the previous value")
        void putOverwriteReturnsOldValue() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("key", "old");
            assertEquals("old", map.put("key", "new"));
        }

        @Test
        @DisplayName("overwrite replaces the value")
        void putOverwriteReplacesValue() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("key", "old");
            map.put("key", "new");
            assertEquals("new", map.get("key"));
        }

        @Test
        @DisplayName("does not grow size on overwrite")
        void putOverwriteKeepsSize() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("key", "old");
            map.put("key", "new");
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("rejects null keys")
        void putNullKeyThrows() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            assertThrows(NullPointerException.class, () -> map.put(null, "value"));
        }

        @Test
        @DisplayName("permits null values")
        void putNullValueAllowed() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("key", null);
            assertEquals(1, map.size());
            assertNull(map.get("key"));
            assertEquals(List.of("key"), map.keys());
            assertEquals(java.util.Collections.singletonList(null), map.values());
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns null for a missing key")
        void getMissingReturnsNull() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("present", "value");
            assertNull(map.get("absent"));
        }

        @Test
        @DisplayName("rejects null keys")
        void getNullKeyThrows() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            assertThrows(NullPointerException.class, () -> map.get(null));
        }

        @Test
        @DisplayName("distinguishes absent key from present key with null value")
        void getDistinguishesNullValueFromAbsent() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("present", null);
            assertNull(map.get("present"));
            assertNull(map.get("absent"));
            assertEquals(1, map.size());
            assertEquals(List.of("present"), map.keys());
        }

        @Test
        @DisplayName("works for many distinct keys")
        void getManyKeys() {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();
            for (int i = 0; i < 1_000; i++) {
                map.put(i, "v" + i);
            }
            for (int i = 0; i < 1_000; i++) {
                assertEquals("v" + i, map.get(i));
            }
            assertNull(map.get(-1));
            assertNull(map.get(1_000));
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("returns the removed value")
        void removeReturnsValue() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("key", "value");
            assertEquals("value", map.remove("key"));
        }

        @Test
        @DisplayName("removes the key from the map")
        void removeDeletesKey() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("key", "value");
            map.remove("key");
            assertNull(map.get("key"));
            assertTrue(map.keys().isEmpty());
        }

        @Test
        @DisplayName("decrements size")
        void removeDecrementsSize() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("a", "1");
            map.put("b", "2");
            map.remove("a");
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("returns null for a missing key")
        void removeMissingReturnsNull() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("present", "value");
            assertNull(map.remove("absent"));
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("rejects null keys")
        void removeNullKeyThrows() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            assertThrows(NullPointerException.class, () -> map.remove(null));
        }

        @Test
        @DisplayName("allows re-insertion after removal")
        void reinsertAfterRemove() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("key", "value");
            map.remove("key");
            map.put("key", "again");
            assertEquals("again", map.get("key"));
            assertEquals(1, map.size());
        }
    }

    @Nested
    @DisplayName("keys")
    class Keys {

        @Test
        @DisplayName("returns every key exactly once")
        void keysContainsAllKeys() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("a", "1");
            map.put("b", "2");
            map.put("c", "3");
            assertEquals(Set.of("a", "b", "c"), new HashSet<>(map.keys()));
        }

        @Test
        @DisplayName("size matches map size")
        void keysSizeMatchesMapSize() {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();
            for (int i = 0; i < 100; i++) {
                map.put(i, "v" + i);
            }
            assertEquals(map.size(), map.keys().size());
        }

        @Test
        @DisplayName("reflects removals")
        void keysReflectRemovals() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("a", "1");
            map.put("b", "2");
            map.remove("a");
            assertEquals(List.of("b"), map.keys());
        }
    }

    @Nested
    @DisplayName("values")
    class Values {

        @Test
        @DisplayName("returns every value and permits duplicates")
        void valuesContainsAllValues() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("a", "same");
            map.put("b", "same");
            map.put("c", "different");
            final List<String> values = map.values();
            assertEquals(3, values.size());
            assertEquals(List.of("same", "same", "different"), values);
        }

        @Test
        @DisplayName("includes null values")
        void valuesIncludeNull() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("a", null);
            map.put("b", "x");
            final List<String> values = map.values();
            assertEquals(2, values.size());
            assertTrue(values.contains(null));
            assertTrue(values.contains("x"));
        }

        @Test
        @DisplayName("size matches map size")
        void valuesSizeMatchesMapSize() {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();
            for (int i = 0; i < 100; i++) {
                map.put(i, "v" + i);
            }
            assertEquals(map.size(), map.values().size());
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("empties a populated map")
        void clearEmptiesMap() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("a", "1");
            map.put("b", "2");
            map.clear();
            assertEquals(0, map.size());
            assertTrue(map.keys().isEmpty());
            assertTrue(map.values().isEmpty());
            assertNull(map.get("a"));
        }

        @Test
        @DisplayName("map remains usable after clear")
        void mapUsableAfterClear() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("a", "1");
            map.clear();
            map.put("b", "2");
            assertEquals("2", map.get("b"));
            assertEquals(1, map.size());
        }
    }

    @Nested
    @DisplayName("collision handling (open hashing, no chaining)")
    class Collisions {

        @Test
        @DisplayName("keys with identical hash codes still work")
        void identicalHashCodes() {
            final TDDHashMap<CollisionKey, String> map = new TDDHashMap<>();
            for (int i = 0; i < 500; i++) {
                map.put(new CollisionKey(i), "v" + i);
            }
            for (int i = 0; i < 500; i++) {
                assertEquals("v" + i, map.get(new CollisionKey(i)));
            }
            assertEquals(500, map.size());
        }

        @Test
        @DisplayName("removal under heavy collision stays correct")
        void removeUnderCollisions() {
            final TDDHashMap<CollisionKey, String> map = new TDDHashMap<>();
            for (int i = 0; i < 300; i++) {
                map.put(new CollisionKey(i), "v" + i);
            }
            for (int i = 0; i < 300; i += 2) {
                assertEquals("v" + i, map.remove(new CollisionKey(i)));
            }
            for (int i = 0; i < 300; i++) {
                if (i % 2 == 0) {
                    assertNull(map.get(new CollisionKey(i)));
                } else {
                    assertEquals("v" + i, map.get(new CollisionKey(i)));
                }
            }
        }

        @Test
        @DisplayName("colliding and non-colliding keys coexist")
        void mixedCollidingKeys() {
            final TDDHashMap<Object, String> map = new TDDHashMap<>();
            final CollisionKey a = new CollisionKey(1);
            final CollisionKey b = new CollisionKey(2);
            map.put(a, "collision");
            map.put(b, "collision2");
            map.put("plain", "normal");
            map.put(42, "integer");
            assertEquals("collision", map.get(a));
            assertEquals("collision2", map.get(b));
            assertEquals("normal", map.get("plain"));
            assertEquals("integer", map.get(42));
            assertEquals(4, map.size());
        }
    }

    @Nested
    @DisplayName("capacity and growth")
    class Capacity {

        @Test
        @DisplayName("supports far more entries than the initial bucket count")
        void unboundCapacity() {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();
            final int count = 10_000;
            for (int i = 0; i < count; i++) {
                map.put(i, "value-" + i);
            }
            assertEquals(count, map.size());
            for (int i = 0; i < count; i++) {
                assertEquals("value-" + i, map.get(i));
            }
            assertEquals(count, map.keys().size());
            assertEquals(count, map.values().size());
        }

        @Test
        @DisplayName("grows, then shrinks back to empty correctly")
        void growThenEmpty() {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();
            for (int i = 0; i < 5_000; i++) {
                map.put(i, "v" + i);
            }
            for (int i = 0; i < 5_000; i++) {
                map.remove(i);
            }
            assertEquals(0, map.size());
            assertTrue(map.keys().isEmpty());
            assertNull(map.get(0));
        }

        @Test
        @DisplayName("randomized mixed operations stay consistent")
        void randomizedOperations() {
            final TDDHashMap<Integer, String> map = new TDDHashMap<>();
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            final List<Integer> expectedKeys = new ArrayList<>();
            for (int i = 0; i < 5_000; i++) {
                final int key = random.nextInt(2_000);
                switch (random.nextInt(3)) {
                    case 0 -> {
                        final String old = map.put(key, "v" + key + "-" + i);
                        if (old == null && !expectedKeys.contains(key)) {
                            expectedKeys.add(key);
                        }
                    }
                    case 1 -> {
                        final String removed = map.remove(key);
                        if (removed != null) {
                            expectedKeys.remove((Integer) key);
                        }
                    }
                    default -> map.get(key);
                }
            }
            assertEquals(expectedKeys.size(), map.size());
            assertEquals(expectedKeys.size(), map.keys().size());
            for (final Integer key : expectedKeys) {
                assertNotNull(map.get(key));
            }
        }
    }

    @Nested
    @DisplayName("equality semantics")
    class Equality {

        @Test
        @DisplayName("keys are compared by equals(), not identity")
        void equalsNotIdentity() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put(new String("key"), "value");
            assertEquals("value", map.get(new String("key")));
        }

        @Test
        @DisplayName("distinct keys that compare equal collapse to one entry")
        void equalKeysCollapse() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put(new String("key"), "first");
            map.put(new String("key"), "second");
            assertEquals(1, map.size());
            assertEquals("second", map.get("key"));
        }

        @Test
        @DisplayName("returned lists are independent of the map")
        void listsAreIndependent() {
            final TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("a", "1");
            final List<String> keys = map.keys();
            final List<String> values = map.values();
            map.clear();
            assertEquals(List.of("a"), keys);
            assertEquals(List.of("1"), values);
            assertNotEquals(List.of("a"), map.keys());
        }
    }
}
