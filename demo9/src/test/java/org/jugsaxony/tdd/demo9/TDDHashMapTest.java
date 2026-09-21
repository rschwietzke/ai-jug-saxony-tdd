package org.jugsaxony.tdd.demo9;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TDDHashMapTest {

    private TDDHashMap<String, String> map;

    @BeforeEach
    void setUp() {
        map = new TDDHashMap<>();
    }

    @Nested
    @DisplayName("Initial State & Empty Map Tests")
    class InitialStateTests {

        @Test
        @DisplayName("Newly constructed map should have size 0")
        void newMapHasSizeZero() {
            assertThat(map.size()).isZero();
        }

        @Test
        @DisplayName("get on empty map should return null")
        void getOnEmptyMapReturnsNull() {
            assertThat(map.get("key")).isNull();
        }

        @Test
        @DisplayName("remove on empty map should return null and maintain size 0")
        void removeOnEmptyMapReturnsNull() {
            assertThat(map.remove("key")).isNull();
            assertThat(map.size()).isZero();
        }

        @Test
        @DisplayName("keys on empty map should return an empty list")
        void keysOnEmptyMapReturnsEmptyList() {
            List<String> keys = map.keys();
            assertThat(keys).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("values on empty map should return an empty list")
        void valuesOnEmptyMapReturnsEmptyList() {
            List<String> values = map.values();
            assertThat(values).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Basic Put & Get Tests")
    class BasicPutAndGetTests {

        @Test
        @DisplayName("put single key-value pair returns null (no previous value) and updates size")
        void putSingleEntry() {
            String previous = map.put("key1", "value1");

            assertThat(previous).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("put multiple distinct entries and retrieve all of them")
        void putMultipleEntries() {
            map.put("k1", "v1");
            map.put("k2", "v2");
            map.put("k3", "v3");

            assertThat(map.size()).isEqualTo(3);
            assertThat(map.get("k1")).isEqualTo("v1");
            assertThat(map.get("k2")).isEqualTo("v2");
            assertThat(map.get("k3")).isEqualTo("v3");
        }

        @Test
        @DisplayName("get on non-existent key returns null when map has entries")
        void getNonExistentKeyReturnsNull() {
            map.put("k1", "v1");
            assertThat(map.get("nonExistent")).isNull();
        }

        @Test
        @DisplayName("put with existing key updates value, returns previous value, and does not change size")
        void putExistingKeyUpdatesValue() {
            map.put("key", "initial");
            String previous = map.put("key", "updated");

            assertThat(previous).isEqualTo("initial");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key")).isEqualTo("updated");
        }

        @Test
        @DisplayName("put with equal key objects (different instances) updates value")
        void putEqualKeyObjectsUpdatesValue() {
            String key1 = new String("duplicateKey");
            String key2 = new String("duplicateKey");

            map.put(key1, "val1");
            String previous = map.put(key2, "val2");

            assertThat(previous).isEqualTo("val1");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get(key1)).isEqualTo("val2");
            assertThat(map.get(key2)).isEqualTo("val2");
        }
    }

    @Nested
    @DisplayName("Null Key & Null Value Handling")
    class NullHandlingTests {

        @Test
        @DisplayName("put with null key should throw IllegalArgumentException")
        void putNullKeyThrowsException() {
            assertThatThrownBy(() -> map.put(null, "value"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("get with null key should throw IllegalArgumentException")
        void getNullKeyThrowsException() {
            assertThatThrownBy(() -> map.get(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("remove with null key should throw IllegalArgumentException")
        void removeNullKeyThrowsException() {
            assertThatThrownBy(() -> map.remove(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("put with null value is permitted")
        void putNullValuePermitted() {
            String previous = map.put("keyWithNull", null);

            assertThat(previous).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("keyWithNull")).isNull();
        }

        @Test
        @DisplayName("update existing non-null value to null returns old value")
        void updateValueToNullReturnsOldValue() {
            map.put("key", "existingValue");
            String previous = map.put("key", null);

            assertThat(previous).isEqualTo("existingValue");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key")).isNull();
        }

        @Test
        @DisplayName("update existing null value to non-null returns null")
        void updateNullValueToNonNullReturnsNull() {
            map.put("key", null);
            String previous = map.put("key", "newValue");

            assertThat(previous).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key")).isEqualTo("newValue");
        }

        @Test
        @DisplayName("remove key that has null value returns null and decrements size")
        void removeKeyWithNullValue() {
            map.put("key", null);
            String removed = map.remove("key");

            assertThat(removed).isNull();
            assertThat(map.size()).isZero();
            assertThat(map.get("key")).isNull();
        }

        @Test
        @DisplayName("values list includes null value when mapped")
        void valuesListContainsNull() {
            map.put("k1", null);
            map.put("k2", "v2");

            List<String> values = map.values();
            assertThat(values).containsExactlyInAnyOrder(null, "v2");
        }
    }

    @Nested
    @DisplayName("Collision Handling (Open Hashing / Addressing)")
    class CollisionTests {

        record FixedHashKey(String id, int fixedHash) {
            @Override
            public int hashCode() {
                return fixedHash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (obj == null || getClass() != obj.getClass()) return false;
                FixedHashKey that = (FixedHashKey) obj;
                return Objects.equals(id, that.id);
            }
        }

        @Test
        @DisplayName("Handle multiple keys with identical hash code")
        void handleCollidingKeys() {
            TDDHashMap<FixedHashKey, String> collisionMap = new TDDHashMap<>();
            FixedHashKey k1 = new FixedHashKey("k1", 42);
            FixedHashKey k2 = new FixedHashKey("k2", 42);
            FixedHashKey k3 = new FixedHashKey("k3", 42);

            collisionMap.put(k1, "v1");
            collisionMap.put(k2, "v2");
            collisionMap.put(k3, "v3");

            assertThat(collisionMap.size()).isEqualTo(3);
            assertThat(collisionMap.get(k1)).isEqualTo("v1");
            assertThat(collisionMap.get(k2)).isEqualTo("v2");
            assertThat(collisionMap.get(k3)).isEqualTo("v3");
        }

        @Test
        @DisplayName("Update value of a colliding key without affecting other colliding keys")
        void updateCollidingKey() {
            TDDHashMap<FixedHashKey, String> collisionMap = new TDDHashMap<>();
            FixedHashKey k1 = new FixedHashKey("k1", 100);
            FixedHashKey k2 = new FixedHashKey("k2", 100);

            collisionMap.put(k1, "v1");
            collisionMap.put(k2, "v2");

            String previous = collisionMap.put(k1, "v1_updated");

            assertThat(previous).isEqualTo("v1");
            assertThat(collisionMap.size()).isEqualTo(2);
            assertThat(collisionMap.get(k1)).isEqualTo("v1_updated");
            assertThat(collisionMap.get(k2)).isEqualTo("v2");
        }

        @Test
        @DisplayName("Handle negative hash codes including Integer.MIN_VALUE")
        void handleNegativeHashCodes() {
            TDDHashMap<FixedHashKey, String> negHashMap = new TDDHashMap<>();
            FixedHashKey minKey = new FixedHashKey("min", Integer.MIN_VALUE);
            FixedHashKey negKey = new FixedHashKey("neg", -123456);
            FixedHashKey zeroKey = new FixedHashKey("zero", 0);

            negHashMap.put(minKey, "minVal");
            negHashMap.put(negKey, "negVal");
            negHashMap.put(zeroKey, "zeroVal");

            assertThat(negHashMap.size()).isEqualTo(3);
            assertThat(negHashMap.get(minKey)).isEqualTo("minVal");
            assertThat(negHashMap.get(negKey)).isEqualTo("negVal");
            assertThat(negHashMap.get(zeroKey)).isEqualTo("zeroVal");
        }
    }

    @Nested
    @DisplayName("Remove & Probe Chain Integrity Tests")
    class RemoveTests {

        record FixedHashKey(String id, int fixedHash) {
            @Override
            public int hashCode() {
                return fixedHash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (obj == null || getClass() != obj.getClass()) return false;
                FixedHashKey that = (FixedHashKey) obj;
                return Objects.equals(id, that.id);
            }
        }

        @Test
        @DisplayName("remove existing single key returns value and decrements size")
        void removeExistingKey() {
            map.put("k1", "v1");
            String removed = map.remove("k1");

            assertThat(removed).isEqualTo("v1");
            assertThat(map.size()).isZero();
            assertThat(map.get("k1")).isNull();
        }

        @Test
        @DisplayName("remove non-existing key returns null and keeps size unchanged")
        void removeNonExistingKey() {
            map.put("k1", "v1");
            String removed = map.remove("k2");

            assertThat(removed).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isEqualTo("v1");
        }

        @Test
        @DisplayName("repeated remove returns null on second attempt")
        void repeatedRemove() {
            map.put("k1", "v1");
            assertThat(map.remove("k1")).isEqualTo("v1");
            assertThat(map.remove("k1")).isNull();
            assertThat(map.size()).isZero();
        }

        @Test
        @DisplayName("remove middle element from collision chain does not break probe chain for subsequent elements")
        void removeMiddleOfCollisionChainPreservesReachability() {
            TDDHashMap<FixedHashKey, String> collisionMap = new TDDHashMap<>();
            FixedHashKey k1 = new FixedHashKey("k1", 7);
            FixedHashKey k2 = new FixedHashKey("k2", 7);
            FixedHashKey k3 = new FixedHashKey("k3", 7);

            collisionMap.put(k1, "v1");
            collisionMap.put(k2, "v2");
            collisionMap.put(k3, "v3");

            // Remove k2 (the middle colliding element)
            String removed = collisionMap.remove(k2);
            assertThat(removed).isEqualTo("v2");
            assertThat(collisionMap.size()).isEqualTo(2);

            // k1 and k3 must still be reachable!
            assertThat(collisionMap.get(k1)).isEqualTo("v1");
            assertThat(collisionMap.get(k3)).isEqualTo("v3");
            assertThat(collisionMap.get(k2)).isNull();
        }

        @Test
        @DisplayName("remove first element of collision chain preserves reachability of others")
        void removeHeadOfCollisionChain() {
            TDDHashMap<FixedHashKey, String> collisionMap = new TDDHashMap<>();
            FixedHashKey k1 = new FixedHashKey("k1", 7);
            FixedHashKey k2 = new FixedHashKey("k2", 7);
            FixedHashKey k3 = new FixedHashKey("k3", 7);

            collisionMap.put(k1, "v1");
            collisionMap.put(k2, "v2");
            collisionMap.put(k3, "v3");

            // Remove k1 (head of cluster)
            assertThat(collisionMap.remove(k1)).isEqualTo("v1");
            assertThat(collisionMap.size()).isEqualTo(2);

            assertThat(collisionMap.get(k1)).isNull();
            assertThat(collisionMap.get(k2)).isEqualTo("v2");
            assertThat(collisionMap.get(k3)).isEqualTo("v3");
        }

        @Test
        @DisplayName("re-inserting a removed key in a collision chain works properly")
        void reinsertRemovedKeyInCollisionChain() {
            TDDHashMap<FixedHashKey, String> collisionMap = new TDDHashMap<>();
            FixedHashKey k1 = new FixedHashKey("k1", 7);
            FixedHashKey k2 = new FixedHashKey("k2", 7);

            collisionMap.put(k1, "v1");
            collisionMap.put(k2, "v2");

            collisionMap.remove(k1);
            assertThat(collisionMap.get(k1)).isNull();

            // Re-insert k1
            collisionMap.put(k1, "v1_new");
            assertThat(collisionMap.size()).isEqualTo(2);
            assertThat(collisionMap.get(k1)).isEqualTo("v1_new");
            assertThat(collisionMap.get(k2)).isEqualTo("v2");
        }
    }

    @Nested
    @DisplayName("Clear Tests")
    class ClearTests {

        @Test
        @DisplayName("clear on empty map does nothing")
        void clearEmptyMap() {
            map.clear();
            assertThat(map.size()).isZero();
            assertThat(map.keys()).isEmpty();
            assertThat(map.values()).isEmpty();
        }

        @Test
        @DisplayName("clear resets size and removes all elements")
        void clearPopulatedMap() {
            map.put("k1", "v1");
            map.put("k2", "v2");
            map.put("k3", "v3");

            map.clear();

            assertThat(map.size()).isZero();
            assertThat(map.get("k1")).isNull();
            assertThat(map.get("k2")).isNull();
            assertThat(map.get("k3")).isNull();
            assertThat(map.keys()).isEmpty();
            assertThat(map.values()).isEmpty();
        }

        @Test
        @DisplayName("map is reusable after clear")
        void mapReusableAfterClear() {
            map.put("oldKey", "oldVal");
            map.clear();

            map.put("newKey", "newVal");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("newKey")).isEqualTo("newVal");
            assertThat(map.get("oldKey")).isNull();
        }
    }

    @Nested
    @DisplayName("Keys & Values Collection Tests")
    class CollectionViewTests {

        @Test
        @DisplayName("keys returns all keys present in the map")
        void keysReturnsAllKeys() {
            map.put("a", "1");
            map.put("b", "2");
            map.put("c", "3");

            List<String> keys = map.keys();

            assertThat(keys).hasSize(3);
            assertThat(keys).containsExactlyInAnyOrder("a", "b", "c");
        }

        @Test
        @DisplayName("values returns all values present in the map including duplicates")
        void valuesReturnsAllValues() {
            map.put("a", "same");
            map.put("b", "same");
            map.put("c", "other");

            List<String> values = map.values();

            assertThat(values).hasSize(3);
            assertThat(values).containsExactlyInAnyOrder("same", "same", "other");
        }

        @Test
        @DisplayName("keys and values reflect removals")
        void keysAndValuesReflectRemovals() {
            map.put("a", "1");
            map.put("b", "2");
            map.remove("a");

            assertThat(map.keys()).containsExactly("b");
            assertThat(map.values()).containsExactly("2");
        }
    }

    @Nested
    @DisplayName("Unbound Capacity & Dynamic Resizing Tests")
    class ResizingTests {

        @ParameterizedTest
        @ValueSource(ints = {50, 500, 5000})
        @DisplayName("Map handles growth across multiple resize thresholds")
        void handlesLargeNumberOfElements(int count) {
            TDDHashMap<Integer, String> intMap = new TDDHashMap<>();

            for (int i = 0; i < count; i++) {
                intMap.put(i, "val" + i);
            }

            assertThat(intMap.size()).isEqualTo(count);

            for (int i = 0; i < count; i++) {
                assertThat(intMap.get(i)).isEqualTo("val" + i);
            }

            // Remove every even key
            for (int i = 0; i < count; i += 2) {
                String removed = intMap.remove(i);
                assertThat(removed).isEqualTo("val" + i);
            }

            assertThat(intMap.size()).isEqualTo(count / 2);

            // Odd keys remain intact, even keys return null
            for (int i = 0; i < count; i++) {
                if (i % 2 == 0) {
                    assertThat(intMap.get(i)).isNull();
                } else {
                    assertThat(intMap.get(i)).isEqualTo("val" + i);
                }
            }
        }

        @Test
        @DisplayName("Colliding keys are preserved during resize and rehashing")
        void collidingKeysPreservedDuringResize() {
            record FixedHashKey(String id, int fixedHash) {
                @Override
                public int hashCode() {
                    return fixedHash;
                }

                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    FixedHashKey that = (FixedHashKey) obj;
                    return Objects.equals(id, that.id);
                }
            }

            TDDHashMap<FixedHashKey, Integer> collisionMap = new TDDHashMap<>();
            int count = 100;

            // Insert 100 keys all colliding on hashCode = 13
            for (int i = 0; i < count; i++) {
                collisionMap.put(new FixedHashKey("key" + i, 13), i);
            }

            assertThat(collisionMap.size()).isEqualTo(count);

            for (int i = 0; i < count; i++) {
                assertThat(collisionMap.get(new FixedHashKey("key" + i, 13))).isEqualTo(i);
            }
        }

        @Test
        @DisplayName("Wrap-around collision cluster with removals across array boundary")
        void wrapAroundCollisionClusterWithRemovals() {
            record FixedHashKey(String id, int fixedHash) {
                @Override
                public int hashCode() {
                    return fixedHash;
                }

                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    FixedHashKey that = (FixedHashKey) obj;
                    return Objects.equals(id, that.id);
                }
            }

            TDDHashMap<FixedHashKey, String> map = new TDDHashMap<>();
            // Capacity starts at 16, mask is 15. Hash 14 places keys right at the end of the array,
            // wrapping around to indices 15, 0, 1, 2...
            FixedHashKey k1 = new FixedHashKey("k1", 14);
            FixedHashKey k2 = new FixedHashKey("k2", 14);
            FixedHashKey k3 = new FixedHashKey("k3", 14);
            FixedHashKey k4 = new FixedHashKey("k4", 14);

            map.put(k1, "v1");
            map.put(k2, "v2");
            map.put(k3, "v3");
            map.put(k4, "v4");

            assertThat(map.size()).isEqualTo(4);

            // Remove k2 (causing wrap-around shift)
            assertThat(map.remove(k2)).isEqualTo("v2");
            assertThat(map.size()).isEqualTo(3);
            assertThat(map.get(k1)).isEqualTo("v1");
            assertThat(map.get(k2)).isNull();
            assertThat(map.get(k3)).isEqualTo("v3");
            assertThat(map.get(k4)).isEqualTo("v4");

            // Remove head k1
            assertThat(map.remove(k1)).isEqualTo("v1");
            assertThat(map.size()).isEqualTo(2);
            assertThat(map.get(k1)).isNull();
            assertThat(map.get(k3)).isEqualTo("v3");
            assertThat(map.get(k4)).isEqualTo("v4");
        }

        @Test
        @DisplayName("Stress test: 50,000 sequential insertions and lookups")
        void stressTestFiftyThousandEntries() {
            TDDHashMap<Integer, Integer> stressMap = new TDDHashMap<>();
            int total = 50_000;

            for (int i = 0; i < total; i++) {
                stressMap.put(i, i * 2);
            }

            assertThat(stressMap.size()).isEqualTo(total);

            for (int i = 0; i < total; i++) {
                assertThat(stressMap.get(i)).isEqualTo(i * 2);
            }
        }
    }
}
