# Conversation Log: TDD Implementation of Open Hashing Map (`TDDHashMap`)

**Date:** 2026-09-21  
**Project:** AI JUG Saxony TDD Demo 9 (`demo9`)  
**Package:** `org.jugsaxony.tdd`  
**Target Class:** `TDDHashMap.java`  
**Test Class:** `TDDHashMapTest.java`  
**Environment:** Java 21, JUnit Jupiter 5.11.4, AssertJ 3.27.3, Apache Maven 3.9.0

---

## 1. Initial User Request & Requirements

The user requested the creation of a new open hashing map and all required unit tests using a strict Test-Driven Development (TDD) workflow.

### Functional Requirements
- **Generic Types:** `K` (keys) and `V` (values).
- **Required Method Signatures:**
  - `public TDDHashMap()`
  - `public V get( final K key )`
  - `public V put( final K key, final V value )`
  - `public V remove( final K key )`
  - `public int size()`
  - `public List<K> keys()`
  - `public List<V> values()`
  - `public void clear()`
- **Storage & Collision Constraints:**
  - **Open Hashing:** No separate chaining in case of collisions.
  - **No Storage Wrappers:** No `Entry` or `Node` objects.
  - **Allocation-Free Storage:** Storage operations (`get`, `put`, `remove`) must not allocate heap objects, except when expanding the backing array structure.
  - **Thread-Safety:** Not thread-safe.
  - **Collision Strategy:** Free choice of open addressing strategy (linear probing selected).
  - **Capacity:** Unbound (resizes dynamically).
  - **Null Semantics:** No null keys permitted (`IllegalArgumentException`); null values are permitted.
- **Workflow Constraint:**
  - Implement tests first (TDD Red phase).
  - Stop after test suite creation for human review.
  - Wait for an explicit `"go"` before implementing the concrete code.
  - Strictly stay inside `demo9`.

---

## 2. Phase 1: Project Setup & Test Suite Creation (TDD Red Phase)

### Project Configuration (`pom.xml`)
Created [pom.xml](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo9/pom.xml) configured for Java 21 release, JUnit Jupiter 5.11.4, AssertJ 3.27.3, and Maven Surefire 3.5.2 configured with `useManifestOnlyJar=false` and `useSystemClassLoader=false` for compatibility with JDK runtimes.

### Skeleton Class (`TDDHashMap.java`)
Created the initial stub [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo9/src/main/java/org/jugsaxony/tdd/TDDHashMap.java) containing all required method signatures throwing `UnsupportedOperationException("Not implemented yet")` to allow compilation while asserting failure in the Red phase.

### Test Suite (`TDDHashMapTest.java`)
Implemented comprehensive JUnit 5 tests covering 37 initial test scenarios organized into nested test classes:

1. **`InitialStateTests` (5 tests):**
   - New map size is 0.
   - `get` on empty map returns `null`.
   - `remove` on empty map returns `null` and preserves size 0.
   - `keys()` and `values()` return non-null, empty lists.
2. **`BasicPutAndGetTests` (5 tests):**
   - Single entry insertion, size tracking, and retrieval.
   - Multiple distinct entries insertion and lookup.
   - Lookup for non-existent keys returns `null`.
   - Key overwrite returns previous value and preserves size.
   - Key equality based on `.equals()` with distinct instances.
3. **`NullHandlingTests` (8 tests):**
   - `put(null, val)`, `get(null)`, `remove(null)` throw `IllegalArgumentException`.
   - `put(key, null)` permitted with correct size tracking.
   - Updating non-null to null and null to non-null returns correct previous value.
   - Removing key with null value decrements size and returns null.
   - `values()` list includes mapped null entries.
4. **`CollisionTests` (3 tests):**
   - Uses `FixedHashKey` to force hash collisions.
   - Multiple colliding keys stored and retrieved correctly via open addressing.
   - Updating one colliding key does not affect other colliding keys.
   - Handling negative hash codes including `Integer.MIN_VALUE`.
5. **`RemoveTests` (6 tests):**
   - Removal returns previous value, decrements size, and causes subsequent `get` to return `null`.
   - Removing non-existent key returns `null` without altering size.
   - Repeated removal returns `null`.
   - **Cluster Integrity:** Removing the head or middle element of a collision cluster preserves reachability for subsequent colliding elements.
   - Re-inserting removed keys reuses available slots cleanly.
6. **`ClearTests` (3 tests):**
   - `clear()` on empty and populated maps resets size and empties views.
   - Map is fully reusable after `clear()`.
7. **`CollectionViewTests` (3 tests):**
   - `keys()` and `values()` return all elements with correct multiplicities and sizes.
   - Removals are immediately reflected in returned collections.
8. **`ResizingTests` (4 tests):**
   - Parameterized test over 50, 500, and 5,000 entries with interleaved removals.
   - Collision clusters preserved during array resize and rehashing.

### Red Phase Test Execution
Ran `mvn test`:
- **Result:** 37 tests executed, 3 failures (null checks expecting `IllegalArgumentException`), 34 errors (`UnsupportedOperationException`).
- **Conclusion:** Red phase established.

---

## 3. Human Review Gate

As requested by the user prompt, execution was stopped after the test suite creation. An implementation plan artifact (`implementation_plan.md`) was produced outlining the open addressing architecture (parallel arrays, Knuth backward-shift deletion, dynamic resizing) and awaiting the user's review.

**User Input:**
```
go
```

---

## 4. Phase 2: Concrete Implementation (TDD Green Phase)

### Architectural Design in `TDDHashMap.java`
1. **Parallel Array Backing Store:**
   - `Object[] keys`: Holds references to keys (`null` indicates an unoccupied slot).
   - `Object[] values`: Holds references to mapped values (supports `null` values natively without confusion with empty slots).
   - No `Map.Entry` or wrapper objects allocated on put/get/remove.
2. **Hash Distribution:**
   - Supplemental bit-mixer: `hash(key) = (h ^ (h >>> 16)) & 0x7fffffff` ensuring all hash values are non-negative and well-distributed.
   - Table size is always a power of two; indexing uses bitwise AND: `hash & (capacity - 1)`.
3. **Collision Resolution: Linear Probing:**
   - Sequential probing: `idx = (idx + 1) & mask`.
4. **Knuth Backward-Shift Deletion (`shiftDelete`):**
   - Avoids tombstones / dead markers entirely.
   - When key at slot `hole` is removed, scans forward through the contiguous cluster.
   - If an element at slot `j` has an ideal hash `k` such that moving to `hole` is valid cyclically:
     `((hole - k) & mask) < ((j - k) & mask)`
     the entry is shifted backward into `hole`, and `hole` moves to `j`.
   - Clears the final empty slot to allow garbage collection of keys/values.
5. **Dynamic Capacity (Unbound):**
   - Initial capacity: 16.
   - Load factor: 0.75 (`threshold = capacity * 0.75`).
   - Doubles table size and rehashes all active entries when `size >= threshold`.

### Additional Tests Added
Added cluster wrap-around deletion tests across array boundaries and a 50,000-entry stress test:
- `wrapAroundCollisionClusterWithRemovals`: Tests probe sequence wrapping around index `0` from the end of the array.
- `stressTestFiftyThousandEntries`: Validates high-volume dynamic growth and lookups.

### Green Phase Test Execution
Ran `mvn test`:
```
[INFO] Running org.jugsaxony.tdd.TDDHashMapTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.114 s -- BasicPutAndGetTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- ClearTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.030 s -- CollectionViewTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- CollisionTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- InitialStateTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.034 s -- NullHandlingTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.026 s -- RemoveTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.206 s -- ResizingTests
[INFO] 
[INFO] Results:
[INFO] Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (Total time: 2.179 s)
```

---

## 5. Artifacts and Final Structure

```
demo9/
├── pom.xml
├── doc/
│   └── implementation.md
└── src/
    ├── main/java/org/jugsaxony/tdd/
    │   └── TDDHashMap.java
    └── test/java/org/jugsaxony/tdd/
        └── TDDHashMapTest.java
```
