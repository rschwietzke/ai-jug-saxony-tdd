package org.jugsaxony.tdd.demo6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code keys()} and {@code values()}.
 *
 * <p>Per the agreed contract both are snapshots in <em>unspecified</em> order with
 * <em>no</em> positional correlation, so nothing here asserts a position. What is
 * asserted: the right elements, the right multiplicities, and snapshot - not view -
 * semantics.
 */
@DisplayName("TDDHashMap - keys() and values()")
class TDDHashMapKeysValuesTest
{
    private TDDHashMap<String, String> map;

    @BeforeEach
    void createMap()
    {
        map = new TDDHashMap<>();
    }

    @Test
    @DisplayName("keys() holds every key exactly once")
    void keysHoldsEveryKeyOnce()
    {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        final List<String> keys = map.keys();

        assertEquals(3, keys.size(), "no duplicates");
        assertEquals(Set.of("a", "b", "c"), new HashSet<>(keys));
    }

    @Test
    @DisplayName("keys() size always matches size()")
    void keysSizeMatchesMapSize()
    {
        for (int i = 0; i < 50; i++)
        {
            map.put("k" + i, "v" + i);
        }

        map.remove("k7");
        map.remove("k13");
        map.put("k7", "again");

        assertEquals(map.size(), map.keys().size());
    }

    @Test
    @DisplayName("values() holds one element per mapping, duplicates included")
    void valuesKeepsDuplicates()
    {
        map.put("a", "same");
        map.put("b", "same");
        map.put("c", "other");

        final List<String> values = map.values();

        assertEquals(3, values.size());
        assertEquals(bagOf("same", "same", "other"), bag(values));
    }

    @Test
    @DisplayName("values() size always matches size()")
    void valuesSizeMatchesMapSize()
    {
        for (int i = 0; i < 50; i++)
        {
            map.put("k" + i, "v" + (i % 5));
        }

        map.remove("k7");

        assertEquals(map.size(), map.values().size());
    }

    @Test
    @DisplayName("an overwritten value appears only once, in its new form")
    void overwriteReplacesInValues()
    {
        map.put("a", "old");
        map.put("a", "new");

        assertEquals(List.of("new"), map.values());
        assertEquals(List.of("a"), map.keys());
    }

    @Test
    @DisplayName("a removed key disappears from both lists")
    void removedKeyDisappears()
    {
        map.put("a", "1");
        map.put("b", "2");

        map.remove("a");

        assertEquals(List.of("b"), map.keys());
        assertEquals(List.of("2"), map.values());
    }

    @Test
    @DisplayName("every key in keys() resolves through get()")
    void keysAreResolvable()
    {
        for (int i = 0; i < 100; i++)
        {
            map.put("k" + i, "v" + i);
        }

        for (final String key : map.keys())
        {
            assertEquals("v" + key.substring(1), map.get(key), key);
        }
    }

    @Test
    @DisplayName("the values collected via keys() match values()")
    void listsDescribeTheSameMappings()
    {
        for (int i = 0; i < 100; i++)
        {
            map.put("k" + i, "v" + (i % 7));
        }

        final List<String> viaKeys = new ArrayList<>();

        for (final String key : map.keys())
        {
            viaKeys.add(map.get(key));
        }

        assertEquals(bag(viaKeys), bag(map.values()));
    }

    @Test
    @DisplayName("keys() is a snapshot - later changes do not show up in it")
    void keysIsASnapshotOfThePast()
    {
        map.put("a", "1");

        final List<String> keys = map.keys();

        map.put("b", "2");
        map.remove("a");

        assertEquals(List.of("a"), keys, "the returned list must not track the map");
    }

    @Test
    @DisplayName("values() is a snapshot - later changes do not show up in it")
    void valuesIsASnapshotOfThePast()
    {
        map.put("a", "1");

        final List<String> values = map.values();

        map.put("a", "changed");
        map.put("b", "2");

        assertEquals(List.of("1"), values, "the returned list must not track the map");
    }

    @Test
    @DisplayName("mutating the returned key list does not touch the map")
    void mutatingKeysDoesNotTouchTheMap()
    {
        map.put("a", "1");
        map.put("b", "2");

        final List<String> keys = map.keys();

        try
        {
            keys.clear();
        }
        catch (final UnsupportedOperationException immutableIsFineToo)
        {
            // An immutable snapshot satisfies the contract just as well.
        }

        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
    }

    @Test
    @DisplayName("mutating the returned value list does not touch the map")
    void mutatingValuesDoesNotTouchTheMap()
    {
        map.put("a", "1");

        final List<String> values = map.values();

        try
        {
            values.clear();
        }
        catch (final UnsupportedOperationException immutableIsFineToo)
        {
            // An immutable snapshot satisfies the contract just as well.
        }

        assertEquals(1, map.size());
        assertEquals("1", map.get("a"));
    }

    @Test
    @DisplayName("calling keys() twice returns equal content")
    void repeatableCalls()
    {
        map.put("a", "1");
        map.put("b", "2");

        assertEquals(new HashSet<>(map.keys()), new HashSet<>(map.keys()));
        assertEquals(bag(map.values()), bag(map.values()));
    }

    @Test
    @DisplayName("both lists are empty, never null, on an empty map")
    void emptyMapYieldsEmptyLists()
    {
        assertNotNull(map.keys());
        assertNotNull(map.values());
        assertTrue(map.keys().isEmpty());
        assertTrue(map.values().isEmpty());
    }

    @Test
    @DisplayName("both lists stay correct across collisions")
    void listsSurviveCollisions()
    {
        final TDDHashMap<CollidingKey, String> collidingMap = new TDDHashMap<>();

        final List<CollidingKey> keys = CollidingKey.colliding(64);

        for (int i = 0; i < keys.size(); i++)
        {
            collidingMap.put(keys.get(i), "v" + i);
        }

        collidingMap.remove(keys.get(10));
        collidingMap.remove(keys.get(20));

        assertEquals(62, collidingMap.keys().size());
        assertEquals(62, collidingMap.values().size());

        final Set<CollidingKey> expected = new HashSet<>(keys);
        expected.remove(keys.get(10));
        expected.remove(keys.get(20));

        assertEquals(expected, new HashSet<>(collidingMap.keys()));
    }

    /**
     * Counts occurrences, so two collections can be compared as multisets without
     * caring about order. Tolerates {@code null} elements.
     */
    private static <T> Map<T, Integer> bag(final Collection<T> items)
    {
        final Map<T, Integer> counts = new HashMap<>();

        for (final T item : items)
        {
            counts.merge(item, 1, Integer::sum);
        }

        return counts;
    }

    @SafeVarargs
    private static <T> Map<T, Integer> bagOf(final T... items)
    {
        final Map<T, Integer> counts = new HashMap<>();

        for (final T item : items)
        {
            counts.merge(item, 1, Integer::sum);
        }

        return counts;
    }
}
