package org.jugsaxony.tdd.demo8;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TDDHashMap Tests")
class TDDHashMapTest {

    private TDDHashMap<String, String> map;

    @BeforeEach
    void setUp() {
        map = new TDDHashMap<>();
    }

    @Nested
    @DisplayName("Initial state & size")
    class InitialStateTests {

        @Test
        @DisplayName("Newly created map has size 0")
        void newMapIsEmpty() {
            assertThat(map.size()).isZero();
        }

        @Test
        @DisplayName("get on empty map returns null")
        void getOnEmptyMapReturnsNull() {
            assertThat(map.get("key")).isNull();
        }

        @Test
        @DisplayName("remove on empty map returns null")
        void removeOnEmptyMapReturnsNull() {
            assertThat(map.remove("key")).isNull();
        }

        @Test
        @DisplayName("keys on empty map returns empty list")
        void keysOnEmptyMapReturnsEmptyList() {
            assertThat(map.keys()).isEmpty();
        }

        @Test
        @DisplayName("values on empty map returns empty list")
        void valuesOnEmptyMapReturnsEmptyList() {
            assertThat(map.values()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Basic put & get operations")
    class BasicPutGetTests {

        @Test
        @DisplayName("put a new key-value pair returns null and increments size")
        void putNewEntry() {
            String previous = map.put("k1", "v1");

            assertThat(previous).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isEqualTo("v1");
        }

        @Test
        @DisplayName("put multiple distinct key-value pairs")
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
        @DisplayName("put existing key updates value and returns old value without changing size")
        void putOverwritesExistingKey() {
            map.put("k1", "v1");
            String previous = map.put("k1", "v2");

            assertThat(previous).isEqualTo("v1");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isEqualTo("v2");
        }

        @Test
        @DisplayName("get non-existent key returns null")
        void getNonExistentKey() {
            map.put("k1", "v1");

            assertThat(map.get("k2")).isNull();
        }
    }

    @Nested
    @DisplayName("Null handling")
    class NullHandlingTests {

        @Test
        @DisplayName("put with null key throws IllegalArgumentException or NullPointerException")
        void putNullKeyThrowsException() {
            assertThatThrownBy(() -> map.put(null, "value"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("get with null key throws IllegalArgumentException or NullPointerException")
        void getNullKeyThrowsException() {
            assertThatThrownBy(() -> map.get(null))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("remove with null key throws IllegalArgumentException or NullPointerException")
        void removeNullKeyThrowsException() {
            assertThatThrownBy(() -> map.remove(null))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("put with null value is permitted")
        void putNullValuePermitted() {
            String prev = map.put("k1", null);

            assertThat(prev).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isNull();
        }

        @Test
        @DisplayName("overwriting non-null value with null returns old value")
        void overwriteWithNullValue() {
            map.put("k1", "v1");
            String prev = map.put("k1", null);

            assertThat(prev).isEqualTo("v1");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isNull();
        }

        @Test
        @DisplayName("overwriting null value with new value returns null")
        void overwriteNullValueWithNonNull() {
            map.put("k1", null);
            String prev = map.put("k1", "v2");

            assertThat(prev).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isEqualTo("v2");
        }
    }

    @Nested
    @DisplayName("Remove operations")
    class RemoveTests {

        @Test
        @DisplayName("remove existing key returns value and decrements size")
        void removeExistingKey() {
            map.put("k1", "v1");
            map.put("k2", "v2");

            String removed = map.remove("k1");

            assertThat(removed).isEqualTo("v1");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isNull();
            assertThat(map.get("k2")).isEqualTo("v2");
        }

        @Test
        @DisplayName("remove non-existent key returns null and keeps size")
        void removeNonExistentKey() {
            map.put("k1", "v1");

            String removed = map.remove("k2");

            assertThat(removed).isNull();
            assertThat(map.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("remove key with null value returns null and decrements size")
        void removeKeyWithNullValue() {
            map.put("k1", null);

            String removed = map.remove("k1");

            assertThat(removed).isNull();
            assertThat(map.size()).isZero();
            assertThat(map.get("k1")).isNull();
        }

        @Test
        @DisplayName("re-inserting a removed key works correctly")
        void reinsertRemovedKey() {
            map.put("k1", "v1");
            map.remove("k1");

            String prev = map.put("k1", "v2");

            assertThat(prev).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isEqualTo("v2");
        }
    }

    @Nested
    @DisplayName("Clear operations")
    class ClearTests {

        @Test
        @DisplayName("clear on empty map keeps size 0")
        void clearEmptyMap() {
            map.clear();

            assertThat(map.size()).isZero();
            assertThat(map.keys()).isEmpty();
            assertThat(map.values()).isEmpty();
        }

        @Test
        @DisplayName("clear on populated map removes all elements and resets size")
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
        @DisplayName("can insert new elements after clear")
        void insertAfterClear() {
            map.put("k1", "v1");
            map.clear();
            map.put("k2", "v2");

            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k1")).isNull();
            assertThat(map.get("k2")).isEqualTo("v2");
        }
    }

    @Nested
    @DisplayName("Keys and Values collection views")
    class KeysAndValuesTests {

        @Test
        @DisplayName("keys() returns all inserted keys")
        void keysReturnsAllKeys() {
            map.put("k1", "v1");
            map.put("k2", "v2");
            map.put("k3", "v3");

            List<String> keys = map.keys();

            assertThat(keys).containsExactlyInAnyOrder("k1", "k2", "k3");
        }

        @Test
        @DisplayName("values() returns all inserted values including duplicates and nulls")
        void valuesReturnsAllValues() {
            map.put("k1", "v1");
            map.put("k2", "v1");
            map.put("k3", null);

            List<String> values = map.values();

            assertThat(values).containsExactlyInAnyOrder("v1", "v1", null);
        }

        @Test
        @DisplayName("keys() does not contain removed keys")
        void keysExcludesRemoved() {
            map.put("k1", "v1");
            map.put("k2", "v2");
            map.remove("k1");

            assertThat(map.keys()).containsExactly("k2");
        }

        @Test
        @DisplayName("values() does not contain removed values")
        void valuesExcludesRemoved() {
            map.put("k1", "v1");
            map.put("k2", "v2");
            map.remove("k1");

            assertThat(map.values()).containsExactly("v2");
        }
    }

    @Nested
    @DisplayName("Hash Collisions & Probing")
    class HashCollisionTests {

        /**
         * Key helper with forced identical hashCode to test collision resolution and open addressing probing.
         */
        static class CollidingKey {
            private final String name;
            private final int fixedHashCode;

            CollidingKey(String name, int fixedHashCode) {
                this.name = name;
                this.fixedHashCode = fixedHashCode;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                CollidingKey that = (CollidingKey) o;
                return Objects.equals(name, that.name);
            }

            @Override
            public int hashCode() {
                return fixedHashCode;
            }

            @Override
            public String toString() {
                return "CollidingKey{" + name + ", hash=" + fixedHashCode + "}";
            }
        }

        @Test
        @DisplayName("Multiple keys with same hashCode can all be inserted and retrieved")
        void collidingKeysPutAndGet() {
            TDDHashMap<CollidingKey, String> collisionMap = new TDDHashMap<>();
            CollidingKey k1 = new CollidingKey("a", 42);
            CollidingKey k2 = new CollidingKey("b", 42);
            CollidingKey k3 = new CollidingKey("c", 42);
            CollidingKey k4 = new CollidingKey("d", 42);

            collisionMap.put(k1, "valA");
            collisionMap.put(k2, "valB");
            collisionMap.put(k3, "valC");
            collisionMap.put(k4, "valD");

            assertThat(collisionMap.size()).isEqualTo(4);
            assertThat(collisionMap.get(k1)).isEqualTo("valA");
            assertThat(collisionMap.get(k2)).isEqualTo("valB");
            assertThat(collisionMap.get(k3)).isEqualTo("valC");
            assertThat(collisionMap.get(k4)).isEqualTo("valD");
        }

        @Test
        @DisplayName("Updating a value for one colliding key does not affect other colliding keys")
        void collidingKeysUpdate() {
            TDDHashMap<CollidingKey, String> collisionMap = new TDDHashMap<>();
            CollidingKey k1 = new CollidingKey("a", 42);
            CollidingKey k2 = new CollidingKey("b", 42);

            collisionMap.put(k1, "valA");
            collisionMap.put(k2, "valB");

            String old = collisionMap.put(k1, "valA_updated");

            assertThat(old).isEqualTo("valA");
            assertThat(collisionMap.size()).isEqualTo(2);
            assertThat(collisionMap.get(k1)).isEqualTo("valA_updated");
            assertThat(collisionMap.get(k2)).isEqualTo("valB");
        }

        @Test
        @DisplayName("Removing a colliding key does not break probe chain for subsequent colliding keys (tombstone / shift handling)")
        void removeCollidingKeyDoesNotBreakProbeChain() {
            TDDHashMap<CollidingKey, String> collisionMap = new TDDHashMap<>();
            CollidingKey k1 = new CollidingKey("a", 42);
            CollidingKey k2 = new CollidingKey("b", 42);
            CollidingKey k3 = new CollidingKey("c", 42);

            collisionMap.put(k1, "valA");
            collisionMap.put(k2, "valB");
            collisionMap.put(k3, "valC");

            // Remove the middle/first element of collision cluster
            String removed = collisionMap.remove(k1);
            assertThat(removed).isEqualTo("valA");
            assertThat(collisionMap.size()).isEqualTo(2);

            // k2 and k3 must still be reachable
            assertThat(collisionMap.get(k1)).isNull();
            assertThat(collisionMap.get(k2)).isEqualTo("valB");
            assertThat(collisionMap.get(k3)).isEqualTo("valC");

            // Also remove k2 and ensure k3 is still reachable
            collisionMap.remove(k2);
            assertThat(collisionMap.size()).isEqualTo(1);
            assertThat(collisionMap.get(k3)).isEqualTo("valC");
        }

        @Test
        @DisplayName("Re-inserting into slot of previously removed colliding key")
        void reinsertIntoRemovedCollidingSlot() {
            TDDHashMap<CollidingKey, String> collisionMap = new TDDHashMap<>();
            CollidingKey k1 = new CollidingKey("a", 42);
            CollidingKey k2 = new CollidingKey("b", 42);
            CollidingKey k3 = new CollidingKey("c", 42);

            collisionMap.put(k1, "valA");
            collisionMap.put(k2, "valB");
            collisionMap.remove(k1);

            // Put a new colliding key k3
            collisionMap.put(k3, "valC");

            assertThat(collisionMap.size()).isEqualTo(2);
            assertThat(collisionMap.get(k1)).isNull();
            assertThat(collisionMap.get(k2)).isEqualTo("valB");
            assertThat(collisionMap.get(k3)).isEqualTo("valC");
        }
    }

    @Nested
    @DisplayName("Resizing and Unbound Capacity")
    class ResizingAndScaleTests {

        @Test
        @DisplayName("Map grows automatically and retains all elements across multiple resizes")
        void growsBeyondInitialCapacity() {
            int count = 10_000;
            for (int i = 0; i < count; i++) {
                map.put("key_" + i, "val_" + i);
            }

            assertThat(map.size()).isEqualTo(count);

            for (int i = 0; i < count; i++) {
                assertThat(map.get("key_" + i)).isEqualTo("val_" + i);
            }
        }

        @Test
        @DisplayName("Interleaved put and remove under growth maintains correct state and size")
        void interleavedPutAndRemoveUnderGrowth() {
            int count = 2_000;
            for (int i = 0; i < count; i++) {
                map.put("k_" + i, "v_" + i);
            }

            // Remove even keys
            for (int i = 0; i < count; i += 2) {
                String removed = map.remove("k_" + i);
                assertThat(removed).isEqualTo("v_" + i);
            }

            assertThat(map.size()).isEqualTo(count / 2);

            // Verify odd keys present, even absent
            for (int i = 0; i < count; i++) {
                if (i % 2 == 0) {
                    assertThat(map.get("k_" + i)).isNull();
                } else {
                    assertThat(map.get("k_" + i)).isEqualTo("v_" + i);
                }
            }

            // Add new keys causing further resize
            for (int i = count; i < count * 2; i++) {
                map.put("k_" + i, "v_" + i);
            }

            assertThat(map.size()).isEqualTo(count / 2 + count);

            // Verify remaining odd keys still accessible after resize
            for (int i = 1; i < count; i += 2) {
                assertThat(map.get("k_" + i)).isEqualTo("v_" + i);
            }
            for (int i = count; i < count * 2; i++) {
                assertThat(map.get("k_" + i)).isEqualTo("v_" + i);
            }
        }
    }

    @Nested
    @DisplayName("Generics and Custom Object Types")
    class GenericsTests {

        record Person(int id, String name) {}

        @Test
        @DisplayName("Supports non-String types as keys and values")
        void customKeyAndValueTypes() {
            TDDHashMap<Person, Integer> personMap = new TDDHashMap<>();
            Person p1 = new Person(1, "Alice");
            Person p2 = new Person(2, "Bob");

            personMap.put(p1, 100);
            personMap.put(p2, 200);

            assertThat(personMap.size()).isEqualTo(2);
            assertThat(personMap.get(new Person(1, "Alice"))).isEqualTo(100);
            assertThat(personMap.get(p2)).isEqualTo(200);
            assertThat(personMap.remove(p1)).isEqualTo(100);
            assertThat(personMap.size()).isEqualTo(1);
        }
    }
}
