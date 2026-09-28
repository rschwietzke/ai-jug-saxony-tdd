package org.jugsaxony.tdd.demo4;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TDDHashMapTest {

    private TDDHashMap<String, Integer> map;

    @BeforeEach
    void setUp() {
        map = new TDDHashMap<>();
    }

    @Test
    void testPutAndGet() {
        assertNull(map.put("key1", 1));
        assertEquals(1, map.get("key1"));
    }

    @Test
    void testPutUpdate() {
        map.put("key1", 1);
        assertEquals(1, map.put("key1", 2));
        assertEquals(2, map.get("key1"));
    }

    @Test
    void testGetNonExistent() {
        assertNull(map.get("nonexistent"));
    }

    @Test
    void testRemove() {
        map.put("key1", 1);
        assertEquals(1, map.remove("key1"));
        assertNull(map.get("key1"));
    }

    @Test
    void testRemoveNonExistent() {
        assertNull(map.remove("nonexistent"));
    }

    @Test
    void testSize() {
        assertEquals(0, map.size());
        map.put("k1", 1);
        map.put("k2", 2);
        assertEquals(2, map.size());
        map.remove("k1");
        assertEquals(1, map.size());
    }

    @Test
    void testKeys() {
        map.put("k1", 1);
        map.put("k2", 2);
        List<String> keys = map.keys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));
    }

    @Test
    void testValues() {
        map.put("k1", 1);
        map.put("k2", 2);
        List<Integer> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    void testClear() {
        map.put("k1", 1);
        map.put("k2", 2);
        map.clear();
        assertEquals(0, map.size());
        assertNull(map.get("k1"));
    }

    @Test
    void testNullValues() {
        map.put("k1", null);
        assertNull(map.get("k1"));
        assertEquals(1, map.size());
    }

    @Test
    void testNullKeyThrows() {
        assertThrows(Exception.class, () -> map.put(null, 1));
    }

    @Test
    void testCollisionHandling() {
        // Using keys that are likely to collide depending on implementation
        // Since we don't have the implementation yet, we just use many keys
        for (int i = 0; i < 100; i++) {
            map.put("key" + i, i);
        }
        assertEquals(100, map.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i, map.get("key" + i));
        }
    }

    @Test
    void testUnboundCapacity() {
        int largeSize = 10000;
        for (int i = 0; i < largeSize; i++) {
            map.put("k" + i, i);
        }
        assertEquals(largeSize, map.size());
        assertEquals(5000, map.get("k5000"));
    }
}
