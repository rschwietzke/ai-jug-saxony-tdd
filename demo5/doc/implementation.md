# TDDHashMap — Implementation Log

Date: 2026-09-21
Project: `ai-jug-saxony-tdd/demo5`
Package: `org.jugsaxony.tdd`
Files:
- `pom.xml`
- `src/main/java/org/jugsaxony/tdd/TDDHashMap.java`
- `src/test/java/org/jugsaxony/tdd/TDDHashMapTest.java`

## Requirements

- Open hashing map: collisions resolved by probing, **no** separate chaining
- No wrapper objects for storage; allocation free except the backing structure
- API: `TDDHashMap()`, `get(K)`, `put(K,V)`, `remove(K)`, `size()`, `keys()`, `values()`, `clear()`
- Not thread-safe
- Unbound capacity
- No null keys (`NullPointerException`), null values permitted
- Maven, JUnit 5, Java 21, `org.jugsaxony.tdd`
- TDD process: tests first, human review gate, then implementation

## Process (TDD)

### Red phase
- Created `pom.xml` (Java 21 via `maven.compiler.release`, JUnit Jupiter 5.10.2, Surefire 3.2.5)
- Wrote 40 tests in `TDDHashMapTest.java` covering all required behavior
- Created a skeleton `TDDHashMap.java` (all methods threw `UnsupportedOperationException`) so tests would compile and fail
- Result: `Tests run: 40, Failures: 3, Errors: 37` — confirmed red
- Handed over for human review; got "go"

### Green phase
- Replaced the skeleton with a real implementation
- Fixed one invalid test assertion: `List.of((String) null)` throws `NullPointerException` because `List.of` forbids null elements — replaced with `Collections.singletonList(null)`
- Result: `Tests run: 40, Failures: 0, Errors: 0` — `BUILD SUCCESS`

## Implementation

Collision strategy: **linear probing** (open hashing).

- Two parallel `Object[]` arrays (`keys`, `values`) — the only backing structure; no `Entry`-style wrappers
- Slot states in `keys`:
  - `null` → empty
  - shared `TOMBSTONE` sentinel (`new Object()`) → deleted; probes continue past it
  - otherwise → live key
- Hash spread: `hash ^ (hash >>> 16)`, mask `keys.length - 1` (capacity always a power of two)
- `put` probes linearly, replaces an existing key (returns old value), else stores at the first tombstone or first empty slot; returns `null` for a new key
- `remove` probes, tombstoning the slot, clearing the value, decrementing `size`
- Growth: when `(size + tombstones) * 2 >= keys.length` (load factor 0.5) the arrays double and live entries are reinserted, dropping tombstones
- `clear()` allocates fresh arrays back to the initial capacity (backing structure only)
- `keys()` / `values()` build fresh `ArrayList`s from live slots — returned lists are independent of the map
- Null keys rejected with `Objects.requireNonNull` → `NullPointerException`; null values legal, liveness decided by the key slot

### Notes
- Tombstones prevent probe chains from breaking after removals; they are reclaimed on resize.
- Heavy-collision correctness (e.g. all keys with `hashCode()` 42) is exercised by the test suite via a `CollisionKey` helper class.
- With all-colliding keys linear probing degrades to O(n) per op; acceptable per requirements (correctness-focused), resizing keeps it bounded.

## Test suite (40 tests)

| Group | Coverage |
|---|---|
| EmptyMap (6) | size 0, get/remove null, keys/values empty, clear no-op |
| Put (7) | returns null for new key, stores value, overwrite returns old + replaces, size stable, null key NPE, null value allowed |
| Get (4) | missing returns null, null key NPE, absent vs present-with-null-value, 1000 distinct keys |
| Remove (6) | returns old value, deletes key, decrements size, missing returns null, null key NPE, re-insert after remove |
| Keys (3) | all keys once (set), size match, reflects removals |
| Values (3) | all values incl. duplicates, null values included, size match |
| Clear (2) | empties map, usable after clear |
| Collisions (3) | identical hash codes (500), removal under collisions, mixed colliding keys |
| Capacity (3) | 10k entries, grow-then-shrink to empty, 5000 randomized ops vs reference |
| Equality (3) | equals() not identity, equal keys collapse, returned lists independent |

## Decisions
- Null key exception type: `NullPointerException` (matches `java.util.Map` contract)
- Collision strategy free choice: linear probing chosen
- `keys()` / `values()` order unspecified — tests use sets / `containsAll`, never index assumptions
