# TDD Open Addressing Hash Map Implementation Log

## Overview & Requirements

The goal was to implement a custom open addressing hash map (`TDDHashMap<K, V>`) following a Test-Driven Development (TDD) workflow.

### Functional & Design Requirements:
- **Package**: `org.jugsaxony.tdd`
- **Class Name**: `TDDHashMap.java`
- **Environment**: Java 21, JUnit 5, AssertJ, Maven
- **Required API Signatures**:
  - `public TDDHashMap()`
  - `public V get(final K key)`
  - `public V put(final K key, final V value)`
  - `public V remove(final K key)`
  - `public int size()`
  - `public List<K> keys()`
  - `public List<V> values()`
  - `public void clear()`
- **Constraints**:
  - Open hashing / open addressing: no separate chaining for collisions.
  - No wrapper objects for storage (`Map.Entry` or similar); allocation-free storage except for backing arrays.
  - Unbound capacity with automatic dynamic resizing.
  - Disallow `null` keys (`NullPointerException` / `IllegalArgumentException`).
  - Allow `null` values.
  - Not thread-safe.
  - TDD process: implement tests first, verify compilation and failure against stubs, wait for human review and 'go' approval before implementing the concrete map.

---

## Phase 1: Project Setup & Test Suite Creation

### 1. Maven Configuration (`pom.xml`)
Created `pom.xml` targeting Java 21 with dependencies for JUnit Jupiter (5.11.4) and AssertJ (3.27.3), plus `maven-compiler-plugin` and `maven-surefire-plugin`.

### 2. Method Stubs Skeleton (`TDDHashMap.java`)
Created the initial skeleton class with all requested method signatures throwing `UnsupportedOperationException("Not implemented yet")`.

### 3. Test Suite (`TDDHashMapTest.java`)
Implemented 33 test cases structured across nested test classes:
- **InitialStateTests**:
  - Size is 0 on empty map.
  - `get` / `remove` on empty map returns `null`.
  - `keys()` and `values()` return empty lists.
- **BasicPutGetTests**:
  - `put` new key-value pair returns `null` and increments size.
  - `put` multiple distinct key-value pairs.
  - `put` existing key overwrites value and returns old value without increasing size.
  - `get` non-existent key returns `null`.
- **NullHandlingTests**:
  - `put`, `get`, and `remove` with `null` key throws a runtime exception.
  - `put` with `null` value is permitted and size increments.
  - Overwriting non-null value with `null` returns the previous value.
  - Overwriting `null` value with non-null value returns `null`.
- **RemoveTests**:
  - `remove` existing key returns value and decrements size.
  - `remove` non-existent key returns `null` and preserves size.
  - `remove` key associated with a `null` value returns `null` and decrements size.
  - Re-inserting a removed key functions correctly.
- **ClearTests**:
  - `clear()` resets size to 0 and empties keys/values views.
  - Inserting after `clear()` works properly.
- **KeysAndValuesTests**:
  - `keys()` returns all present keys and excludes removed keys.
  - `values()` returns all present values (including duplicates and nulls) and excludes removed values.
- **HashCollisionTests**:
  - `CollidingKey` helper forcing identical hash codes.
  - Multiple colliding keys can all be inserted and retrieved via linear probing.
  - Updating a colliding key does not affect other colliding keys in the cluster.
  - Removing a colliding key preserves probe continuity for subsequent colliding keys.
  - Re-inserting into a slot vacated by a deleted colliding key.
- **ResizingAndScaleTests**:
  - Map grows beyond initial capacity (10,000 items) across multiple resizing cycles while preserving all entries.
  - Interleaved `put` and `remove` operations under continuous growth maintain correct state and size.
- **GenericsTests**:
  - Custom object / record types as keys and values.

### 4. Test Verification (Red Phase)
Ran `mvn test` to verify that all 33 tests compiled and failed as expected on the unimplemented stubs (30 errors on stubs, 3 null-key exception tests passed immediately).

---

## Phase 2: Human Review & Go Signal

The test suite was presented for review. The user provided the `go` confirmation to proceed with the implementation.

---

## Phase 3: Implementation (`TDDHashMap.java`)

Implemented `TDDHashMap` using open addressing:
- **Storage**: Two parallel `Object[]` arrays (`keys` and `values`) with power-of-two sizing, avoiding node/entry object wrapper allocations.
- **Hashing**: Spread hash function `h ^ (h >>> 16)` masked with `(length - 1)` for power-of-two table indexing.
- **Collision Resolution**: Linear probing (`idx = (idx + 1) & mask`).
- **Deletion Strategy**: Backward-shift deletion algorithm (`deleteAndShift`), shifting subsequent entries backward into vacated slots when needed, avoiding tombstone markers and preventing cluster degradation.
- **Capacity Management**: Automatic resizing when `size >= threshold` (default load factor 0.75), doubling table capacity and re-hashing active keys.
- **Null Safety**: `Objects.requireNonNull(key)` check on `get`, `put`, and `remove`.

---

## Phase 4: Final Verification (Green Phase)

Ran `mvn test`:
```
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
All 33 test cases passed.
