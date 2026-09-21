package org.jugsaxony.tdd.demo6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Null keys are rejected, null values are first-class citizens.
 *
 * <p>The interesting consequence: {@code get} returning {@code null} is ambiguous.
 * Only {@code size()} and {@code keys()} can tell "absent" from "mapped to null",
 * and these tests hold the implementation to that.
 */
@DisplayName("TDDHashMap - null handling")
class TDDHashMapNullTest
{
    private TDDHashMap<String, String> map;

    @BeforeEach
    void createMap()
    {
        map = new TDDHashMap<>();
    }

    @Nested
    @DisplayName("null keys are rejected")
    class NullKeys
    {
        @Test
        @DisplayName("get(null) throws IllegalArgumentException")
        void getRejectsNull()
        {
            assertThrows(IllegalArgumentException.class, () -> map.get(null));
        }

        @Test
        @DisplayName("put(null, value) throws IllegalArgumentException")
        void putRejectsNullKey()
        {
            assertThrows(IllegalArgumentException.class, () -> map.put(null, "1"));
        }

        @Test
        @DisplayName("put(null, null) throws IllegalArgumentException")
        void putRejectsNullKeyEvenWithNullValue()
        {
            assertThrows(IllegalArgumentException.class, () -> map.put(null, null));
        }

        @Test
        @DisplayName("remove(null) throws IllegalArgumentException")
        void removeRejectsNull()
        {
            assertThrows(IllegalArgumentException.class, () -> map.remove(null));
        }

        @Test
        @DisplayName("a rejected key leaves the map untouched")
        void rejectionIsSideEffectFree()
        {
            map.put("a", "1");

            assertThrows(IllegalArgumentException.class, () -> map.put(null, "2"));

            assertEquals(1, map.size());
            assertEquals("1", map.get("a"));
            assertEquals(List.of("a"), map.keys());
        }

        @Test
        @DisplayName("rejection happens on a populated map too, not just an empty one")
        void rejectionOnPopulatedMap()
        {
            map.put("a", "1");
            map.put("b", "2");

            assertThrows(IllegalArgumentException.class, () -> map.get(null));
            assertThrows(IllegalArgumentException.class, () -> map.remove(null));

            assertEquals(2, map.size());
        }
    }

    @Nested
    @DisplayName("null values are stored")
    class NullValues
    {
        @Test
        @DisplayName("put(key, null) reports no previous value")
        void putReturnsNull()
        {
            assertNull(map.put("a", null));
        }

        @Test
        @DisplayName("put(key, null) creates a real entry")
        void createsRealEntry()
        {
            map.put("a", null);

            assertEquals(1, map.size(), "a null value is still a mapping");
            assertEquals(List.of("a"), map.keys());
        }

        @Test
        @DisplayName("the null value comes back from get")
        void getReturnsNull()
        {
            map.put("a", null);

            assertNull(map.get("a"));
        }

        @Test
        @DisplayName("values() contains the null")
        void valuesContainsNull()
        {
            map.put("a", null);

            final List<String> values = map.values();

            assertEquals(1, values.size());
            assertNull(values.get(0));
        }

        @Test
        @DisplayName("a null value can be overwritten with a real one")
        void nullOverwrittenByValue()
        {
            map.put("a", null);

            assertNull(map.put("a", "1"), "the previous value was null");
            assertEquals("1", map.get("a"));
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("a real value can be overwritten with null")
        void valueOverwrittenByNull()
        {
            map.put("a", "1");

            assertEquals("1", map.put("a", null));
            assertNull(map.get("a"));
            assertEquals(1, map.size(), "overwriting with null must not remove the entry");
        }

        @Test
        @DisplayName("remove drops a null-valued entry and returns null")
        void removeNullValuedEntry()
        {
            map.put("a", null);

            assertNull(map.remove("a"));
            assertEquals(0, map.size());
            assertTrue(map.keys().isEmpty());
        }

        @Test
        @DisplayName("size and keys distinguish 'absent' from 'mapped to null'")
        void absentIsNotTheSameAsNullValued()
        {
            map.put("present", null);

            assertNull(map.get("present"));
            assertNull(map.get("absent"));

            assertEquals(1, map.size());
            assertTrue(map.keys().contains("present"));
            assertFalse(map.keys().contains("absent"));
        }

        @Test
        @DisplayName("many null values coexist with real ones")
        void mixedNullAndNonNull()
        {
            map.put("a", null);
            map.put("b", "2");
            map.put("c", null);
            map.put("d", "4");

            assertEquals(4, map.size());
            assertNull(map.get("a"));
            assertEquals("2", map.get("b"));
            assertNull(map.get("c"));
            assertEquals("4", map.get("d"));

            assertEquals(2, map.values().stream().filter(v -> v == null).count());
        }

        @Test
        @DisplayName("null values survive collisions")
        void nullValuesWithCollidingKeys()
        {
            final TDDHashMap<CollidingKey, String> collidingMap = new TDDHashMap<>();

            final List<CollidingKey> keys = CollidingKey.colliding(20);

            for (int i = 0; i < keys.size(); i++)
            {
                collidingMap.put(keys.get(i), i % 2 == 0 ? null : "v" + i);
            }

            assertEquals(20, collidingMap.size());

            for (int i = 0; i < keys.size(); i++)
            {
                if (i % 2 == 0)
                {
                    assertNull(collidingMap.get(keys.get(i)), "key " + i);
                }
                else
                {
                    assertEquals("v" + i, collidingMap.get(keys.get(i)), "key " + i);
                }
            }
        }

        @Test
        @DisplayName("clear removes null-valued entries too")
        void clearRemovesNullValues()
        {
            map.put("a", null);
            map.put("b", null);

            map.clear();

            assertEquals(0, map.size());
            assertTrue(map.values().isEmpty());
        }
    }
}
