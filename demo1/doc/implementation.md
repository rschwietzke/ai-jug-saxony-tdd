# Implementation Log: Open Hashing Map (TDDHashMap)

## Overview

This document logs the design, implementation, and test-driven development (TDD) process for `TDDHashMap`, an open-addressing (open hashing) hash map in Java 21, built under the `org.jugsaxony.tdd` package with JUnit 5.

---

## 1. Requirements & Constraints

- **Class Name**: `org.jugsaxony.tdd.TDDHashMap<K, V>`
- **Method Signatures**:
  - `public TDDHashMap()`
  - `public V get(final K key)`
  - `public V put(final K key, final V value)`
  - `public V remove(final K key)`
  - `public int size()`
  - `public List<K> keys()`
  - `public List<V> values()`
  - `public void clear()`
- **Architecture**:
  - **Open Hashing / Open Addressing**: No separate chaining (no linked list or tree buckets).
  - **Wrapper-free**: No `Map.Entry` or wrapper objects allocated during insertions/lookups.
  - **Allocation-free storage**: Parallel arrays `Object[] keys` and `Object[] values` for backing storage.
  - **Thread safety**: Not thread-safe.
  - **Capacity**: Unbounded capacity via dynamic resizing (power-of-two growth).
  - **Null Handling**: Disallow `null` keys (`NullPointerException`), allow `null` values.
  - **Stack**: Java 21, Maven, JUnit 5, AssertJ.

---

## 2. Development Process (TDD Workflow)

### Phase 1: Test & Skeleton Definition (RED)

1. **Build Configuration (`pom.xml`)**:
   - Configured `maven.compiler.release = 21`.
   - Added `junit-jupiter` (v5.11.4) and `assertj-core` (v3.27.3).
   - Configured `maven-surefire-plugin` (v3.5.2).

2. **Skeleton Creation (`TDDHashMap.java`)**:
   - Defined generic class `TDDHashMap<K, V>`.
   - Created all required method stubs throwing `UnsupportedOperationException`.

3. **Test Suite Design (`TDDHashMapTest.java`)**:
   Created nested test structure covering:
   - **Initial State**: Empty map verification (`size == 0`, `get == null`, empty lists).
   - **Basic Put/Get**: Single insertion, updates returning old values, multiple distinct keys.
   - **Null Handling**: Rejection of null keys on `put`, `get`, `remove`; full support for storing/retrieving `null` values.
   - **Collision Handling**: Dedicated `CollidingKey` class with fixed `hashCode()` to force collisions; verifying open addressing probing and isolated updates.
   - **Deletion & Probe Chain Continuity**: Removing elements and verifying that removing an item in the middle or head of a collision cluster does not break subsequent lookups.
   - **Resizing**: Inserting elements beyond default capacity to trigger dynamic array growth.
   - **Key/Value Views**: `keys()` and `values()` views reflecting all active entries (including `null` values).
   - **Clear & Reuse**: Clearing all entries and reusing the map.

4. **Test Run Verification**:
   - Executed `mvn test`.
   - Verified that the compilation succeeded and 18 initial tests failed as expected with `UnsupportedOperationException`.

---

### Phase 2: Concrete Implementation (GREEN)

1. **Storage Structure**:
   - Parallel arrays `Object[] keys` and `Object[] values` sized to powers of two.
   - Bitwise mask (`mask = capacity - 1`) for fast modulo indexing.
   - Hash perturbation (`h ^ (h >>> 16)`) to spread high-order bits into lower table positions.

2. **Collision Resolution**:
   - Linear probing (`slot = (slot + 1) & mask`).

3. **Tombstone-Free Deletion (Knuth's Algorithm R)**:
   - Instead of marking deleted slots with sentinel tombstones, implemented backward-shift deletion (`shiftDelete`).
   - When slot `emptySlot` is vacated, subsequent non-empty slots `j` in the cluster are inspected.
   - If the key at `j` can be shifted back to `emptySlot` (determined by cyclic distance comparison `distCurrentToEmpty <= distCurrentToIdeal`), it is moved back, and `emptySlot` becomes `j`.
   - This keeps the probe chain intact with 0 tombstones and 0 garbage collector pressure.

4. **Dynamic Resizing**:
   - Automatically doubles table capacity when `size >= threshold` (`capacity * loadFactor`).
   - Rehashes existing keys into the newly allocated arrays.

---

### Phase 3: Stress Testing & Differential Fuzzing

Added additional tests to `TDDHashMapTest.java`:
- **Massive Collision Cluster**: 30 keys sharing identical hash codes, removing odd-indexed keys, verifying even-indexed keys remain accessible, then removing all remaining keys.
- **Differential Fuzz Test**: 20,000 randomized operations (`put`, `remove`, `get`) against `java.util.HashMap` as a reference oracle, verifying exact matching return values and map states.
- **Constructor Validation**: Testing invalid initial capacity (`<= 0`) and load factors (`<= 0` or `>= 1.0`).

---

## 3. Test Verification Results

All 21 test scenarios executed and passed with `mvn test`:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest$ClearTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.142 s
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest$KeysAndValuesTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest$ResizingTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.092 s
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest$RemoveTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest$CollisionTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest$NullHandlingTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.028 s
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest$BasicPutGetTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest$InitialStateTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

