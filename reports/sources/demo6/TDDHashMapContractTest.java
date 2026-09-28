package org.jugsaxony.tdd.demo6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The core behaviour every caller relies on: construction, put, get, remove, size
 * and clear.
 */
@DisplayName("TDDHashMap - core contract")
class TDDHashMapContractTest
{
    private TDDHashMap<String, String> map;

    @BeforeEach
    void createMap()
    {
        map = new TDDHashMap<>();
    }

    @Nested
    @DisplayName("a fresh map")
    class FreshMap
    {
        @Test
        @DisplayName("is empty")
        void isEmpty()
        {
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("returns null for any lookup")
        void lookupReturnsNull()
        {
            assertNull(map.get("absent"));
        }

        @Test
        @DisplayName("returns null when removing anything")
        void removeReturnsNull()
        {
            assertNull(map.remove("absent"));
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("has empty, non-null key and value lists")
        void listsAreEmpty()
        {
            assertTrue(map.keys().isEmpty(), "keys()");
            assertTrue(map.values().isEmpty(), "values()");
        }
    }

    @Nested
    @DisplayName("put")
    class Put
    {
        @Test
        @DisplayName("returns null for a key that was not mapped")
        void returnsNullForNewKey()
        {
            assertNull(map.put("a", "1"));
        }

        @Test
        @DisplayName("makes the value retrievable")
        void storesTheValue()
        {
            map.put("a", "1");

            assertEquals("1", map.get("a"));
        }

        @Test
        @DisplayName("grows the size by one per new key")
        void growsSize()
        {
            map.put("a", "1");
            assertEquals(1, map.size());

            map.put("b", "2");
            assertEquals(2, map.size());
        }

        @Test
        @DisplayName("returns the previous value when overwriting")
        void returnsPreviousValue()
        {
            map.put("a", "1");

            assertEquals("1", map.put("a", "2"));
        }

        @Test
        @DisplayName("replaces the value when overwriting")
        void overwritesTheValue()
        {
            map.put("a", "1");
            map.put("a", "2");

            assertEquals("2", map.get("a"));
        }

        @Test
        @DisplayName("does not change the size when overwriting")
        void overwriteKeepsSize()
        {
            map.put("a", "1");
            map.put("a", "2");
            map.put("a", "3");

            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("keeps distinct keys apart")
        void keepsKeysApart()
        {
            map.put("a", "1");
            map.put("b", "2");
            map.put("c", "3");

            assertEquals("1", map.get("a"));
            assertEquals("2", map.get("b"));
            assertEquals("3", map.get("c"));
            assertEquals(3, map.size());
        }

        @Test
        @DisplayName("returns the value instance that was stored, not a copy")
        void returnsStoredInstance()
        {
            final String value = new String("shared");
            map.put("a", value);

            assertSame(value, map.get("a"));
        }
    }

    @Nested
    @DisplayName("get")
    class Get
    {
        @Test
        @DisplayName("matches keys by equals, not by identity")
        void matchesByEquals()
        {
            map.put(new String("key"), "1");

            assertEquals("1", map.get(new String("key")));
        }

        @Test
        @DisplayName("returns null for an unmapped key")
        void unmappedKeyReturnsNull()
        {
            map.put("a", "1");

            assertNull(map.get("b"));
        }

        @Test
        @DisplayName("does not modify the map")
        void isSideEffectFree()
        {
            map.put("a", "1");

            map.get("a");
            map.get("absent");

            assertEquals(1, map.size());
            assertEquals("1", map.get("a"));
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove
    {
        @Test
        @DisplayName("returns the value that was mapped")
        void returnsRemovedValue()
        {
            map.put("a", "1");

            assertEquals("1", map.remove("a"));
        }

        @Test
        @DisplayName("makes the key unreachable")
        void keyIsGone()
        {
            map.put("a", "1");
            map.remove("a");

            assertNull(map.get("a"));
        }

        @Test
        @DisplayName("shrinks the size by one")
        void shrinksSize()
        {
            map.put("a", "1");
            map.put("b", "2");

            map.remove("a");

            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("returns null and keeps the size for an unmapped key")
        void unmappedKeyIsNoOp()
        {
            map.put("a", "1");

            assertNull(map.remove("b"));
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("is idempotent - removing twice returns null the second time")
        void removingTwice()
        {
            map.put("a", "1");

            assertEquals("1", map.remove("a"));
            assertNull(map.remove("a"));
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("leaves the other keys reachable")
        void leavesOthersAlone()
        {
            map.put("a", "1");
            map.put("b", "2");
            map.put("c", "3");

            map.remove("b");

            assertEquals("1", map.get("a"));
            assertNull(map.get("b"));
            assertEquals("3", map.get("c"));
            assertEquals(2, map.size());
        }

        @Test
        @DisplayName("allows the key to be put again afterwards")
        void keyCanComeBack()
        {
            map.put("a", "1");
            map.remove("a");

            assertNull(map.put("a", "2"), "the key was absent, so put must report no previous value");
            assertEquals("2", map.get("a"));
            assertEquals(1, map.size());
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear
    {
        @Test
        @DisplayName("drops every mapping")
        void dropsEverything()
        {
            map.put("a", "1");
            map.put("b", "2");

            map.clear();

            assertEquals(0, map.size());
            assertNull(map.get("a"));
            assertNull(map.get("b"));
            assertTrue(map.keys().isEmpty(), "keys()");
            assertTrue(map.values().isEmpty(), "values()");
        }

        @Test
        @DisplayName("works on an empty map")
        void onEmptyMap()
        {
            map.clear();

            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("is repeatable")
        void isRepeatable()
        {
            map.put("a", "1");

            map.clear();
            map.clear();

            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("leaves the map usable")
        void mapStaysUsable()
        {
            map.put("a", "1");
            map.clear();

            assertNull(map.put("a", "2"));
            assertEquals("2", map.get("a"));
            assertEquals(1, map.size());
        }
    }

    @Nested
    @DisplayName("size")
    class Size
    {
        @Test
        @DisplayName("tracks a mixed sequence of operations")
        void tracksMixedOperations()
        {
            assertEquals(0, map.size());

            map.put("a", "1");
            assertEquals(1, map.size());

            map.put("b", "2");
            assertEquals(2, map.size());

            map.put("a", "overwrite");
            assertEquals(2, map.size());

            map.remove("nope");
            assertEquals(2, map.size());

            map.remove("a");
            assertEquals(1, map.size());

            map.remove("b");
            assertEquals(0, map.size());

            map.put("c", "3");
            assertEquals(1, map.size());

            map.clear();
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("never goes negative when removing from an empty map")
        void neverNegative()
        {
            map.remove("a");
            map.remove("b");

            assertEquals(0, map.size());
        }
    }
}
