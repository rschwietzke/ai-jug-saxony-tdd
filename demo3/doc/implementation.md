# TDDHashMap Implementation Log

Date: 2026-09-21

## Initial Request

The user requested a new generic open-addressed hash map and its complete test suite. The requested API was:

```java
public TDDHashMap()
public V get(final K key)
public V put(final K key, final V value)
public V remove(final K key)
public int size()
public List<K> keys()
public List<V> values()
public void clear()
```

The implementation requirements were:

- Java 21 and Maven.
- JUnit 5.
- Package `org.jugsaxony.tdd`.
- Production source file `TDDHashMap.java`.
- Open hashing without separate chaining.
- No wrapper objects for stored entries.
- No storage allocations other than backing structures.
- Dynamic, automatically growing capacity.
- Null keys are prohibited.
- Null values are permitted.
- The map is not thread-safe.
- The collision strategy could be selected by the implementer.

The user explicitly required a test-first process. Work had to stop after creating and running the tests so a human could review them. Production code could only be written after the user responded with `go`.

## Test-First Phase

The project directory was initially empty. A Maven project was created with Java 21, JUnit Jupiter 5.11.4, Maven Compiler Plugin 3.13.0, and Maven Surefire Plugin 3.5.2.

The test suite was added at:

```text
src/test/java/org/jugsaxony/tdd/TDDHashMapTest.java
```

The 22 tests cover:

- Empty-map construction and missing-key lookup.
- Key equality rather than object identity.
- New insertion and previous-value return semantics.
- Updating existing mappings without increasing size.
- Null values, including transitions to and from null.
- Null-key rejection by `get`, `put`, and `remove`.
- Removal of existing, missing, and null-valued mappings.
- Multiple distinct keys with identical hash codes.
- Updating one key within a collision sequence.
- Removal from the middle of a probe sequence.
- Zero, negative, minimum, and maximum hash codes.
- Growth through multiple capacity changes.
- Collision-heavy growth, removal, and reinsertion.
- Unordered key snapshots with no duplicate keys.
- Value snapshots containing duplicates and nulls.
- Clearing and reusing the map.
- Idempotent clearing of an empty map.
- Array-and-primitive-only instance storage.
- Direct key/value storage without per-entry wrappers.

### Naming Correction

The original method list named the constructor `FastHashMap()`, while the requested filename was `TDDHashMap.java`. The first test draft interpreted `FastHashMap` as the production type and `TDDHashMap.java` as the test filename.

The user clarified:

> TDDHashMap.java is the target name, FastHashMap was a typo

The test was renamed to `TDDHashMapTest.java`, all specifications were retargeted to `TDDHashMap<K,V>`, and the Maven artifact was named `tdd-hash-map`.

### Red State

The production source tree was deliberately left empty. Running:

```bash
mvn test
```

failed during test compilation with `cannot find symbol: class TDDHashMap`. This was the expected initial red state. Implementation work stopped at this point for human review.

## Implementation Approval

The user reviewed the test-first state and responded:

> go

This explicitly approved creation of the production implementation.

## Implementation Phase

The implementation was added at:

```text
src/main/java/org/jugsaxony/tdd/TDDHashMap.java
```

The selected collision strategy is linear probing. Storage consists only of:

- An `Object[]` for keys.
- An `Object[]` for values.
- A `byte[]` containing `EMPTY`, `OCCUPIED`, or `DELETED` slot states.
- Primitive counters and thresholds.

No entry, node, bucket, or other per-mapping wrapper objects are allocated. Null values are stored directly in the value array. The separate state array distinguishes a stored null value from an unused slot.

Deleted slots are marked with tombstones so lookup can continue through an existing probe sequence. Key and value references are cleared immediately during removal to avoid retaining removed objects.

The table maintains a load threshold of approximately two thirds. It doubles in size when the live-entry threshold is reached. If tombstones consume the threshold while sufficient live capacity remains, the map rehashes at the current capacity to purge them. Capacity is bounded only by practical JVM array limits, with a maximum power-of-two capacity of `1 << 30`.

`keys()` and `values()` return newly allocated unordered snapshots. These result allocations are required by the public list-returning API and are not part of the map's internal storage.

## Green State

After the implementation was created, the complete test suite passed:

```text
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

A clean verification build was then run:

```bash
mvn clean verify
```

It also completed successfully and produced:

```text
target/tdd-hash-map-1.0-SNAPSHOT.jar
```

## Final Files

```text
pom.xml
src/main/java/org/jugsaxony/tdd/TDDHashMap.java
src/test/java/org/jugsaxony/tdd/TDDHashMapTest.java
doc/implementation.md
```
