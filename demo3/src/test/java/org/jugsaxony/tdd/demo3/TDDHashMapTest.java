package org.jugsaxony.tdd.demo3;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TDDHashMap")
class TDDHashMapTest {

    @Nested
    @DisplayName("construction and lookup")
    class ConstructionAndLookup {

        @Test
        void newMapIsEmpty() {
            TDDHashMap<String, Integer> map = new TDDHashMap<>();

            assertAll(
                    () -> assertEquals(0, map.size()),
                    () -> assertTrue(map.keys().isEmpty()),
                    () -> assertTrue(map.values().isEmpty()),
                    () -> assertNull(map.get("missing")));
        }

        @Test
        void getUsesKeyEqualityRatherThanIdentity() {
            TDDHashMap<CollisionKey, String> map = new TDDHashMap<>();
            CollisionKey storedKey = new CollisionKey("equal", 17);
            CollisionKey lookupKey = new CollisionKey("equal", 17);

            map.put(storedKey, "value");

            assertEquals("value", map.get(lookupKey));
        }
    }

    @Nested
    @DisplayName("put")
    class Put {

        @Test
        void storesNewMappingAndReturnsNull() {
            TDDHashMap<String, Payload> map = new TDDHashMap<>();
            Payload value = new Payload("first");

            assertNull(map.put("key", value));
            assertSame(value, map.get("key"));
            assertEquals(1, map.size());
        }

        @Test
        void replacesEqualKeyAndReturnsPreviousValueWithoutGrowing() {
            TDDHashMap<CollisionKey, Payload> map = new TDDHashMap<>();
            CollisionKey firstKey = new CollisionKey("key", 31);
            CollisionKey equalKey = new CollisionKey("key", 31);
            Payload firstValue = new Payload("first");
            Payload secondValue = new Payload("second");

            assertNull(map.put(firstKey, firstValue));
            assertSame(firstValue, map.put(equalKey, secondValue));

            assertAll(
                    () -> assertEquals(1, map.size()),
                    () -> assertSame(secondValue, map.get(firstKey)),
                    () -> assertSame(secondValue, map.get(equalKey)));
        }

        @Test
        void storesNullAsARealValue() {
            TDDHashMap<String, String> map = new TDDHashMap<>();

            assertNull(map.put("nullable", null));

            assertAll(
                    () -> assertEquals(1, map.size()),
                    () -> assertNull(map.get("nullable")),
                    () -> assertTrue(map.keys().contains("nullable")),
                    () -> assertTrue(map.values().contains(null)));
        }

        @Test
        void reportsPreviousValueWhenSwitchingToAndFromNull() {
            TDDHashMap<String, String> map = new TDDHashMap<>();

            assertNull(map.put("key", null));
            assertNull(map.put("key", "present"));
            assertEquals("present", map.put("key", null));

            assertAll(
                    () -> assertEquals(1, map.size()),
                    () -> assertNull(map.get("key")));
        }

        @Test
        void allKeyBasedOperationsRejectNullKeys() {
            TDDHashMap<String, String> map = new TDDHashMap<>();

            assertAll(
                    () -> assertThrows(NullPointerException.class, () -> map.get(null)),
                    () -> assertThrows(NullPointerException.class, () -> map.put(null, "value")),
                    () -> assertThrows(NullPointerException.class, () -> map.remove(null)));
            assertEquals(0, map.size());
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        void removesEqualKeyAndReturnsPreviousValue() {
            TDDHashMap<CollisionKey, Payload> map = new TDDHashMap<>();
            CollisionKey storedKey = new CollisionKey("key", 9);
            Payload value = new Payload("value");
            map.put(storedKey, value);

            Payload removed = map.remove(new CollisionKey("key", 9));

            assertAll(
                    () -> assertSame(value, removed),
                    () -> assertEquals(0, map.size()),
                    () -> assertNull(map.get(storedKey)),
                    () -> assertTrue(map.keys().isEmpty()),
                    () -> assertTrue(map.values().isEmpty()));
        }

        @Test
        void removingUnknownKeyReturnsNullAndDoesNotChangeSize() {
            TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("present", "value");

            assertNull(map.remove("missing"));

            assertAll(
                    () -> assertEquals(1, map.size()),
                    () -> assertEquals("value", map.get("present")));
        }

        @Test
        void removesMappingWhoseValueIsNull() {
            TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("nullable", null);

            assertNull(map.remove("nullable"));

            assertAll(
                    () -> assertEquals(0, map.size()),
                    () -> assertFalse(map.keys().contains("nullable")),
                    () -> assertTrue(map.values().isEmpty()));
        }
    }

    @Nested
    @DisplayName("open-addressed collisions")
    class OpenAddressedCollisions {

        @Test
        void storesDistinctKeysWithIdenticalHashes() {
            TDDHashMap<CollisionKey, String> map = new TDDHashMap<>();
            CollisionKey first = new CollisionKey("first", 42);
            CollisionKey second = new CollisionKey("second", 42);
            CollisionKey third = new CollisionKey("third", 42);

            map.put(first, "one");
            map.put(second, "two");
            map.put(third, "three");

            assertAll(
                    () -> assertEquals(3, map.size()),
                    () -> assertEquals("one", map.get(first)),
                    () -> assertEquals("two", map.get(second)),
                    () -> assertEquals("three", map.get(third)));
        }

        @Test
        void replacingOneCollidingKeyLeavesOtherMappingsUntouched() {
            TDDHashMap<CollisionKey, String> map = new TDDHashMap<>();
            CollisionKey first = new CollisionKey("first", 7);
            CollisionKey second = new CollisionKey("second", 7);
            map.put(first, "one");
            map.put(second, "two");

            assertEquals("two", map.put(new CollisionKey("second", 7), "updated"));

            assertAll(
                    () -> assertEquals(2, map.size()),
                    () -> assertEquals("one", map.get(first)),
                    () -> assertEquals("updated", map.get(second)));
        }

        @Test
        void deletionInsideProbeSequenceDoesNotHideLaterKeys() {
            TDDHashMap<CollisionKey, String> map = new TDDHashMap<>();
            CollisionKey first = new CollisionKey("first", 5);
            CollisionKey middle = new CollisionKey("middle", 5);
            CollisionKey last = new CollisionKey("last", 5);
            CollisionKey replacement = new CollisionKey("replacement", 5);
            map.put(first, "one");
            map.put(middle, "two");
            map.put(last, "three");

            assertEquals("two", map.remove(middle));
            assertEquals("three", map.get(last));
            assertNull(map.put(replacement, "four"));

            assertAll(
                    () -> assertEquals(3, map.size()),
                    () -> assertEquals("one", map.get(first)),
                    () -> assertNull(map.get(middle)),
                    () -> assertEquals("three", map.get(last)),
                    () -> assertEquals("four", map.get(replacement)));
        }

        @Test
        void acceptsZeroNegativeAndExtremeHashCodes() {
            TDDHashMap<CollisionKey, Integer> map = new TDDHashMap<>();
            List<CollisionKey> keys = List.of(
                    new CollisionKey("zero", 0),
                    new CollisionKey("negative", -37),
                    new CollisionKey("minimum", Integer.MIN_VALUE),
                    new CollisionKey("maximum", Integer.MAX_VALUE));

            for (int index = 0; index < keys.size(); index++) {
                map.put(keys.get(index), index);
            }

            for (int index = 0; index < keys.size(); index++) {
                assertEquals(index, map.get(keys.get(index)));
            }
        }

        @Test
        void growsAcrossManyCapacityChanges() {
            TDDHashMap<Integer, Integer> map = new TDDHashMap<>();
            int entryCount = 10_000;

            for (int key = 0; key < entryCount; key++) {
                assertNull(map.put(key, key * 3));
            }

            assertEquals(entryCount, map.size());
            for (int key = 0; key < entryCount; key++) {
                assertEquals(key * 3, map.get(key));
            }
        }

        @Test
        void growsAndReusesCapacityUnderCollisionHeavyChurn() {
            TDDHashMap<CollisionKey, Integer> map = new TDDHashMap<>();
            List<CollisionKey> originalKeys = collisionKeys("original", 512, 1);

            for (int index = 0; index < originalKeys.size(); index++) {
                map.put(originalKeys.get(index), index);
            }
            for (int index = 0; index < originalKeys.size(); index += 2) {
                assertEquals(index, map.remove(originalKeys.get(index)));
            }

            List<CollisionKey> addedKeys = collisionKeys("added", 512, 1);
            for (int index = 0; index < addedKeys.size(); index++) {
                map.put(addedKeys.get(index), 10_000 + index);
            }

            assertEquals(768, map.size());
            for (int index = 1; index < originalKeys.size(); index += 2) {
                assertEquals(index, map.get(originalKeys.get(index)));
            }
            for (int index = 0; index < addedKeys.size(); index++) {
                assertEquals(10_000 + index, map.get(addedKeys.get(index)));
            }
        }
    }

    @Nested
    @DisplayName("keys and values")
    class KeysAndValues {

        @Test
        void keysReturnsEveryCurrentKeyExactlyOnceWithoutDefiningOrder() {
            TDDHashMap<String, Integer> map = new TDDHashMap<>();
            map.put("alpha", 1);
            map.put("beta", 2);
            map.put("gamma", 3);
            map.put("beta", 20);
            map.remove("gamma");

            List<String> keys = map.keys();

            assertAll(
                    () -> assertEquals(2, keys.size()),
                    () -> assertEquals(Set.of("alpha", "beta"), new HashSet<>(keys)));
        }

        @Test
        void valuesReturnsDuplicatesAndNullsAndReflectsUpdates() {
            TDDHashMap<String, String> map = new TDDHashMap<>();
            map.put("first", "duplicate");
            map.put("second", "duplicate");
            map.put("nullable", null);
            map.put("updated", "old");
            map.put("updated", "new");

            List<String> values = map.values();

            assertAll(
                    () -> assertEquals(4, values.size()),
                    () -> assertEquals(2, Collections.frequency(values, "duplicate")),
                    () -> assertEquals(1, Collections.frequency(values, "new")),
                    () -> assertEquals(1, Collections.frequency(values, null)),
                    () -> assertFalse(values.contains("old")));
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        void clearRemovesAllMappingsAndAllowsReuse() {
            TDDHashMap<CollisionKey, String> map = new TDDHashMap<>();
            CollisionKey first = new CollisionKey("first", 3);
            CollisionKey second = new CollisionKey("second", 3);
            map.put(first, "one");
            map.put(second, null);

            map.clear();

            assertAll(
                    () -> assertEquals(0, map.size()),
                    () -> assertNull(map.get(first)),
                    () -> assertNull(map.get(second)),
                    () -> assertTrue(map.keys().isEmpty()),
                    () -> assertTrue(map.values().isEmpty()));

            assertNull(map.put(new CollisionKey("after-clear", 3), "reused"));
            assertEquals("reused", map.get(new CollisionKey("after-clear", 3)));
            assertEquals(1, map.size());
        }

        @Test
        void clearingAnEmptyMapIsIdempotent() {
            TDDHashMap<String, String> map = new TDDHashMap<>();

            map.clear();
            map.clear();

            assertAll(
                    () -> assertEquals(0, map.size()),
                    () -> assertTrue(map.keys().isEmpty()),
                    () -> assertTrue(map.values().isEmpty()));
        }
    }

    @Nested
    @DisplayName("storage constraints")
    class StorageConstraints {

        @Test
        void instanceStateUsesOnlyPrimitivesAndBackingArrays() {
            List<Field> instanceFields = Arrays.stream(TDDHashMap.class.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .filter(field -> !field.isSynthetic())
                    .toList();

            List<String> invalidFields = instanceFields.stream()
                    .filter(field -> !field.getType().isPrimitive() && !field.getType().isArray())
                    .map(field -> field.getName() + ": " + field.getType().getTypeName())
                    .toList();

            assertAll(
                    () -> assertTrue(
                            invalidFields.isEmpty(),
                            () -> "Instance storage must not use object wrappers: " + invalidFields),
                    () -> assertTrue(
                            instanceFields.stream().anyMatch(field -> field.getType().isArray()),
                            "At least one array-backed storage field is required"));
        }

        @Test
        void backingArraysStoreKeysAndValuesWithoutPerEntryWrappers() throws IllegalAccessException {
            TDDHashMap<CollisionKey, Payload> map = new TDDHashMap<>();
            List<CollisionKey> keys = collisionKeys("stored", 3, 11);
            List<Payload> values = List.of(
                    new Payload("one"), new Payload("two"), new Payload("three"));
            for (int index = 0; index < keys.size(); index++) {
                map.put(keys.get(index), values.get(index));
            }

            Set<Object> allowedReferences = Collections.newSetFromMap(new IdentityHashMap<>());
            allowedReferences.addAll(keys);
            allowedReferences.addAll(values);
            addClassOwnedSingletons(allowedReferences);

            boolean foundReferenceBackingArray = false;
            for (Field field : TDDHashMap.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || !field.getType().isArray()
                        || field.getType().getComponentType().isPrimitive()) {
                    continue;
                }
                assertTrue(field.trySetAccessible(), () -> "Cannot inspect backing field " + field.getName());
                Object backingArray = field.get(map);
                if (backingArray != null) {
                    foundReferenceBackingArray = true;
                    assertDirectReferences(backingArray, allowedReferences, field.getName());
                }
            }

            assertTrue(foundReferenceBackingArray, "A generic map requires a reference backing array");
        }
    }

    private static List<CollisionKey> collisionKeys(String prefix, int count, int hash) {
        List<CollisionKey> keys = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            keys.add(new CollisionKey(prefix + '-' + index, hash));
        }
        return keys;
    }

    private static void addClassOwnedSingletons(Set<Object> allowedReferences) throws IllegalAccessException {
        for (Field field : TDDHashMap.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                continue;
            }
            if (field.trySetAccessible()) {
                Object value = field.get(null);
                if (value != null) {
                    allowedReferences.add(value);
                }
            }
        }
    }

    private static void assertDirectReferences(
            Object array, Set<Object> allowedReferences, String backingFieldName) {
        for (int index = 0; index < Array.getLength(array); index++) {
            Object element = Array.get(array, index);
            if (element == null || allowedReferences.contains(element)) {
                continue;
            }
            if (element.getClass().isArray()) {
                assertDirectReferences(element, allowedReferences, backingFieldName);
                continue;
            }
            fail("Backing field '" + backingFieldName + "' contains per-entry wrapper "
                    + element.getClass().getName() + " at index " + index);
        }
    }

    private record Payload(String name) {}

    private record CollisionKey(String id, int hash) {
        @Override
        public int hashCode() {
            return hash;
        }
    }
}
