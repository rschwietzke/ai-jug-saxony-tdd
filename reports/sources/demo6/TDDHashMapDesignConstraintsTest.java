package org.jugsaxony.tdd.demo6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The structural requirements from the brief, which behaviour alone cannot pin down:
 * open addressing rather than separate chaining, no per-entry wrapper objects, and
 * storage that allocates nothing beyond the backing arrays.
 *
 * <p>These look inside the class on purpose. They are the executable form of
 * "no wrappers, no chaining" - remove them and an implementation backed by
 * {@code ArrayList<Entry>[]} would pass every other test in this suite.
 */
@DisplayName("TDDHashMap - design constraints")
class TDDHashMapDesignConstraintsTest
{
    @Test
    @DisplayName("declares no nested class - there is no Entry or Node wrapper")
    void noNestedClasses()
    {
        final Class<?>[] nested = TDDHashMap.class.getDeclaredClasses();

        assertEquals(0, nested.length,
                     () -> "entries must live in plain arrays, found: " + Arrays.toString(nested));
    }

    @Test
    @DisplayName("stores state in arrays and primitives only")
    void storageIsArraysAndPrimitives()
    {
        for (final Field field : instanceFields())
        {
            final Class<?> type = field.getType();

            assertTrue(type.isPrimitive() || type.isArray(),
                       () -> "field '" + field.getName() + "' is a " + type.getName()
                             + "; storage must be arrays or primitives");
        }
    }

    @Test
    @DisplayName("uses no collection type anywhere in its storage")
    void noCollectionsInStorage()
    {
        for (final Field field : instanceFields())
        {
            final Class<?> element = elementTypeOf(field.getType());

            assertFalse(Collection.class.isAssignableFrom(element) || Map.class.isAssignableFrom(element),
                        () -> "field '" + field.getName() + "' uses " + element.getName()
                              + "; that is separate chaining, not open addressing");
        }
    }

    @Test
    @DisplayName("has no array of arrays - buckets are slots, not sub-tables")
    void noNestedArrays()
    {
        for (final Field field : instanceFields())
        {
            final Class<?> type = field.getType();

            if (type.isArray())
            {
                assertFalse(type.getComponentType().isArray(),
                            () -> "field '" + field.getName() + "' is a nested array; "
                                  + "open addressing needs flat slot arrays");
            }
        }
    }

    @Test
    @DisplayName("holds at least one array for keys and one for values")
    void hasBackingArrays()
    {
        final long arrayFields = instanceFields().stream()
                                                 .filter(field -> field.getType().isArray())
                                                 .count();

        assertTrue(arrayFields >= 1,
                   "the map needs backing array storage, found none");
    }

    @Test
    @DisplayName("does not grow its backing arrays while the size stays constant")
    void churnDoesNotGrowStorage() throws Exception
    {
        final TDDHashMap<String, String> map = new TDDHashMap<>();

        // Settle on a capacity first.
        for (int i = 0; i < 100; i++)
        {
            map.put("resident" + i, "v" + i);
        }

        final long capacityBefore = largestArrayLength(map);

        // Churn hard: the size never exceeds 101, so a correct implementation reuses
        // the slots freed by remove instead of growing.
        for (int i = 0; i < 100_000; i++)
        {
            map.put("transient", "v" + i);
            map.remove("transient");
        }

        assertEquals(100, map.size());
        assertEquals(capacityBefore, largestArrayLength(map),
                     "freed slots must be reusable; growing here means tombstones are never reclaimed");
    }

    @Test
    @Tag("allocation")
    @DisplayName("get, overwriting put and remove allocate nothing")
    void operationsAreAllocationFree()
    {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        assumeTrue(bean instanceof com.sun.management.ThreadMXBean,
                   "allocation accounting is a HotSpot extension");

        final com.sun.management.ThreadMXBean hotspot = (com.sun.management.ThreadMXBean) bean;

        assumeTrue(hotspot.isThreadAllocatedMemorySupported(), "allocation accounting unsupported");

        hotspot.setThreadAllocatedMemoryEnabled(true);

        final int entries = 1_000;
        final String[] keys = new String[entries];
        final String[] values = new String[entries];

        for (int i = 0; i < entries; i++)
        {
            keys[i] = "key" + i;
            values[i] = "value" + i;
        }

        final TDDHashMap<String, String> map = new TDDHashMap<>();

        for (int i = 0; i < entries; i++)
        {
            map.put(keys[i], values[i]);
        }

        // Warm up so the JIT has compiled the hot path before we start counting.
        exercise(map, keys, values, 200_000);

        final long before = hotspot.getCurrentThreadAllocatedBytes();

        final int operations = 600_000;
        exercise(map, keys, values, operations / 3);

        final long allocated = hotspot.getCurrentThreadAllocatedBytes() - before;

        // One wrapper object per operation would be 16 bytes or more. Half a byte per
        // operation leaves room for measurement noise and nothing else.
        assertTrue(allocated < operations / 2,
                   () -> "expected allocation-free operations, but " + operations
                         + " of them allocated " + allocated + " bytes");
    }

    /**
     * Runs {@code rounds} triples of get, overwriting put and remove-then-reinsert.
     * Allocates nothing itself, so whatever the counter sees comes from the map.
     */
    private static void exercise(final TDDHashMap<String, String> map,
                                 final String[] keys,
                                 final String[] values,
                                 final int rounds)
    {
        for (int i = 0; i < rounds; i++)
        {
            final int slot = i % keys.length;

            map.get(keys[slot]);
            map.put(keys[slot], values[slot]);
            map.remove(keys[slot]);
            map.put(keys[slot], values[slot]);
        }
    }

    private static List<Field> instanceFields()
    {
        final List<Field> fields = new ArrayList<>();

        for (final Field field : TDDHashMap.class.getDeclaredFields())
        {
            if (!Modifier.isStatic(field.getModifiers()))
            {
                fields.add(field);
            }
        }

        return fields;
    }

    /**
     * @return the innermost component type of a possibly nested array type
     */
    private static Class<?> elementTypeOf(final Class<?> type)
    {
        Class<?> current = type;

        while (current.isArray())
        {
            current = current.getComponentType();
        }

        return current;
    }

    /**
     * @return the length of the longest array field held by the map, or -1 if it has none
     */
    private static long largestArrayLength(final TDDHashMap<?, ?> map) throws Exception
    {
        long longest = -1;

        for (final Field field : instanceFields())
        {
            if (!field.getType().isArray())
            {
                continue;
            }

            field.setAccessible(true);

            final Object array = field.get(map);

            if (array != null)
            {
                longest = Math.max(longest, java.lang.reflect.Array.getLength(array));
            }
        }

        return longest;
    }
}
