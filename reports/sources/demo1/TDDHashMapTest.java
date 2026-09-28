package org.jugsaxony.tdd.demo1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TDDHashMap Test Suite")
class TDDHashMapTest {

    private TDDHashMap<String, String> map;

    @BeforeEach
    void setUp() {
        map = new TDDHashMap<>();
    }

    @Nested
    @DisplayName("1. Initial State & Basic Capacity")
    class InitialStateTests {

        @Test
        @DisplayName("Newly created map should have size 0 and be empty")
        void shouldBeEmptyUponCreation() {
            assertThat(map.size()).isZero();
            assertThat(map.get("key")).isNull();
            assertThat(map.remove("key")).isNull();
            assertThat(map.keys()).isEmpty();
            assertThat(map.values()).isEmpty();
        }

        @Test
        @DisplayName("Should validate constructor arguments")
        void shouldValidateConstructorArguments() {
            assertThatThrownBy(() -> new TDDHashMap<String, String>(0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TDDHashMap<String, String>(-5))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TDDHashMap<String, String>(16, 0.0f))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new TDDHashMap<String, String>(16, 1.0f))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("2. Basic Put and Get Operations")
    class BasicPutGetTests {

        @Test
        @DisplayName("Should put a single key-value pair and retrieve it")
        void shouldPutAndGetSingleElement() {
            String previous = map.put("key1", "value1");

            assertThat(previous).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("Should update existing key and return previous value")
        void shouldUpdateExistingKeyAndReturnOldValue() {
            map.put("key1", "value1");
            String previous = map.put("key1", "value2");

            assertThat(previous).isEqualTo("value1");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key1")).isEqualTo("value2");
        }

        @Test
        @DisplayName("Should put and retrieve multiple distinct key-value pairs")
        void shouldPutAndGetMultipleElements() {
            map.put("key1", "value1");
            map.put("key2", "value2");
            map.put("key3", "value3");

            assertThat(map.size()).isEqualTo(3);
            assertThat(map.get("key1")).isEqualTo("value1");
            assertThat(map.get("key2")).isEqualTo("value2");
            assertThat(map.get("key3")).isEqualTo("value3");
            assertThat(map.get("key4")).isNull();
        }
    }

    @Nested
    @DisplayName("3. Null Key and Null Value Semantics")
    class NullHandlingTests {

        @Test
        @DisplayName("Should allow storing null as a value")
        void shouldPermitNullValues() {
            String prev = map.put("key1", null);

            assertThat(prev).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key1")).isNull();
            assertThat(map.keys()).containsExactly("key1");
            assertThat(map.values()).containsNull();

            String prev2 = map.put("key1", "value1");
            assertThat(prev2).isNull();
            assertThat(map.get("key1")).isEqualTo("value1");

            String prev3 = map.put("key1", null);
            assertThat(prev3).isEqualTo("value1");
            assertThat(map.get("key1")).isNull();
        }

        @Test
        @DisplayName("Should reject null key on put with exception")
        void shouldRejectNullKeyOnPut() {
            assertThatThrownBy(() -> map.put(null, "value"))
                    .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject null key on get with exception")
        void shouldRejectNullKeyOnGet() {
            assertThatThrownBy(() -> map.get(null))
                    .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject null key on remove with exception")
        void shouldRejectNullKeyOnRemove() {
            assertThatThrownBy(() -> map.remove(null))
                    .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("4. Collision Handling (Open Addressing)")
    class CollisionTests {

        /**
         * Key class with fixed hash code to force collisions.
         */
        static final class CollidingKey {
            private final String name;
            private final int fixedHash;

            CollidingKey(String name, int fixedHash) {
                this.name = name;
                this.fixedHash = fixedHash;
            }

            @Override
            public int hashCode() {
                return fixedHash;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                CollidingKey that = (CollidingKey) o;
                return Objects.equals(name, that.name);
            }

            @Override
            public String toString() {
                return "Key[" + name + ", hash=" + fixedHash + "]";
            }
        }

        @Test
        @DisplayName("Should store and retrieve keys that collide on hash code")
        void shouldHandleCollidingKeys() {
            TDDHashMap<CollidingKey, String> collisionMap = new TDDHashMap<>();
            CollidingKey k1 = new CollidingKey("A", 42);
            CollidingKey k2 = new CollidingKey("B", 42);
            CollidingKey k3 = new CollidingKey("C", 42);

            collisionMap.put(k1, "valA");
            collisionMap.put(k2, "valB");
            collisionMap.put(k3, "valC");

            assertThat(collisionMap.size()).isEqualTo(3);
            assertThat(collisionMap.get(k1)).isEqualTo("valA");
            assertThat(collisionMap.get(k2)).isEqualTo("valB");
            assertThat(collisionMap.get(k3)).isEqualTo("valC");
        }

        @Test
        @DisplayName("Should update colliding key without affecting other colliding keys")
        void shouldUpdateCollidingKey() {
            TDDHashMap<CollidingKey, String> collisionMap = new TDDHashMap<>();
            CollidingKey k1 = new CollidingKey("A", 42);
            CollidingKey k2 = new CollidingKey("B", 42);

            collisionMap.put(k1, "valA1");
            collisionMap.put(k2, "valB");
            String oldVal = collisionMap.put(k1, "valA2");

            assertThat(oldVal).isEqualTo("valA1");
            assertThat(collisionMap.size()).isEqualTo(2);
            assertThat(collisionMap.get(k1)).isEqualTo("valA2");
            assertThat(collisionMap.get(k2)).isEqualTo("valB");
        }

        @Test
        @DisplayName("Should handle massive collision cluster and sequential removals")
        void shouldHandleMassiveCollisionCluster() {
            TDDHashMap<CollidingKey, Integer> collisionMap = new TDDHashMap<>(64);
            int count = 30;
            CollidingKey[] keys = new CollidingKey[count];
            for (int i = 0; i < count; i++) {
                keys[i] = new CollidingKey("key_" + i, 7);
                collisionMap.put(keys[i], i);
            }

            assertThat(collisionMap.size()).isEqualTo(count);

            for (int i = 0; i < count; i++) {
                assertThat(collisionMap.get(keys[i])).isEqualTo(i);
            }

            // Remove all odd-indexed keys
            for (int i = 1; i < count; i += 2) {
                assertThat(collisionMap.remove(keys[i])).isEqualTo(i);
            }

            assertThat(collisionMap.size()).isEqualTo(count / 2);

            // Verify even-indexed keys are still reachable
            for (int i = 0; i < count; i += 2) {
                assertThat(collisionMap.get(keys[i])).isEqualTo(i);
            }

            // Remove remaining even-indexed keys
            for (int i = 0; i < count; i += 2) {
                assertThat(collisionMap.remove(keys[i])).isEqualTo(i);
            }

            assertThat(collisionMap.size()).isZero();
            for (int i = 0; i < count; i++) {
                assertThat(collisionMap.get(keys[i])).isNull();
            }
        }
    }

    @Nested
    @DisplayName("5. Remove Operations & Probe Chain Continuity")
    class RemoveTests {

        @Test
        @DisplayName("Should remove an existing key and return its value")
        void shouldRemoveExistingKey() {
            map.put("key1", "val1");
            map.put("key2", "val2");

            String removed = map.remove("key1");

            assertThat(removed).isEqualTo("val1");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key1")).isNull();
            assertThat(map.get("key2")).isEqualTo("val2");
        }

        @Test
        @DisplayName("Should return null when removing non-existing key")
        void shouldReturnNullWhenRemovingNonExistingKey() {
            map.put("key1", "val1");

            String removed = map.remove("nonExisting");

            assertThat(removed).isNull();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("key1")).isEqualTo("val1");
        }

        @Test
        @DisplayName("Should maintain probe chain when removing middle colliding key")
        void shouldMaintainProbeChainWhenRemovingCollidingElement() {
            TDDHashMap<CollisionTests.CollidingKey, String> collisionMap = new TDDHashMap<>();
            CollisionTests.CollidingKey k1 = new CollisionTests.CollidingKey("A", 100);
            CollisionTests.CollidingKey k2 = new CollisionTests.CollidingKey("B", 100);
            CollisionTests.CollidingKey k3 = new CollisionTests.CollidingKey("C", 100);

            collisionMap.put(k1, "valA");
            collisionMap.put(k2, "valB");
            collisionMap.put(k3, "valC");

            // Remove the middle key
            String removed = collisionMap.remove(k2);
            assertThat(removed).isEqualTo("valB");
            assertThat(collisionMap.size()).isEqualTo(2);

            // Verify k1 and k3 can still be found even though k2 was in between
            assertThat(collisionMap.get(k1)).isEqualTo("valA");
            assertThat(collisionMap.get(k3)).isEqualTo("valC");
            assertThat(collisionMap.get(k2)).isNull();

            // Remove the head key
            assertThat(collisionMap.remove(k1)).isEqualTo("valA");
            assertThat(collisionMap.size()).isEqualTo(1);
            assertThat(collisionMap.get(k3)).isEqualTo("valC");
        }
    }

    @Nested
    @DisplayName("6. Dynamic Resizing and Growth")
    class ResizingTests {

        @Test
        @DisplayName("Should grow dynamically when many elements are inserted")
        void shouldGrowDynamicallyWithManyElements() {
            int count = 2000;
            for (int i = 0; i < count; i++) {
                map.put("key_" + i, "val_" + i);
            }

            assertThat(map.size()).isEqualTo(count);

            for (int i = 0; i < count; i++) {
                assertThat(map.get("key_" + i)).isEqualTo("val_" + i);
            }

            // Remove half of them
            for (int i = 0; i < count / 2; i++) {
                assertThat(map.remove("key_" + i)).isEqualTo("val_" + i);
            }

            assertThat(map.size()).isEqualTo(count / 2);

            for (int i = count / 2; i < count; i++) {
                assertThat(map.get("key_" + i)).isEqualTo("val_" + i);
            }
        }

        @Test
        @DisplayName("Should match reference HashMap behavior across large randomized operations")
        void shouldMatchReferenceHashMapOnRandomOperations() {
            TDDHashMap<Integer, String> testMap = new TDDHashMap<>();
            Map<Integer, String> referenceMap = new HashMap<>();
            Random random = new Random(42);

            for (int op = 0; op < 20000; op++) {
                int action = random.nextInt(3);
                int key = random.nextInt(500);

                if (action == 0 || action == 1) { // 66% put
                    String val = "v_" + random.nextInt(1000);
                    String refPrev = referenceMap.put(key, val);
                    String actualPrev = testMap.put(key, val);
                    assertThat(actualPrev).isEqualTo(refPrev);
                } else { // 33% remove
                    String refRemoved = referenceMap.remove(key);
                    String actualRemoved = testMap.remove(key);
                    assertThat(actualRemoved).isEqualTo(refRemoved);
                }
            }

            assertThat(testMap.size()).isEqualTo(referenceMap.size());
            for (Map.Entry<Integer, String> entry : referenceMap.entrySet()) {
                assertThat(testMap.get(entry.getKey())).isEqualTo(entry.getValue());
            }
        }
    }

    @Nested
    @DisplayName("7. Keys and Values Collection Views")
    class KeysAndValuesTests {

        @Test
        @DisplayName("Should return list of all keys")
        void shouldReturnAllKeys() {
            map.put("a", "1");
            map.put("b", "2");
            map.put("c", "3");

            List<String> keys = map.keys();

            assertThat(keys).containsExactlyInAnyOrder("a", "b", "c");
            assertThat(keys).hasSize(3);
        }

        @Test
        @DisplayName("Should return list of all values including nulls")
        void shouldReturnAllValues() {
            map.put("a", "1");
            map.put("b", null);
            map.put("c", "3");

            List<String> values = map.values();

            assertThat(values).containsExactlyInAnyOrder("1", null, "3");
            assertThat(values).hasSize(3);
        }

        @Test
        @DisplayName("Keys and values should reflect removals")
        void shouldReflectRemovalsInKeysAndValues() {
            map.put("a", "1");
            map.put("b", "2");
            map.remove("a");

            assertThat(map.keys()).containsExactly("b");
            assertThat(map.values()).containsExactly("2");
        }
    }

    @Nested
    @DisplayName("8. Clear and Reuse")
    class ClearTests {

        @Test
        @DisplayName("Should clear all entries and allow re-insertion")
        void shouldClearAndBeReusable() {
            map.put("k1", "v1");
            map.put("k2", "v2");

            map.clear();

            assertThat(map.size()).isZero();
            assertThat(map.get("k1")).isNull();
            assertThat(map.get("k2")).isNull();
            assertThat(map.keys()).isEmpty();
            assertThat(map.values()).isEmpty();

            // Re-insert
            map.put("k3", "v3");
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("k3")).isEqualTo("v3");
        }
    }
}

