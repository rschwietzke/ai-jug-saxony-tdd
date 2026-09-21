# TDDHashMap — Implementation Log

Date: 2026-09-21
Project: `demo7` (Maven, Java 21, JUnit 5, package `org.jugsaxony.tdd`)

## 1. Requirements

Create a new implementation of an open hashing map named `TDDHashMap.java` with all required tests, developed in strict TDD style (tests first, human review, then implementation on explicit "go").

Required method signatures (`K`, `V` are generics):

```java
public TDDHashMap()
public V get( final K key )
public V put( final K key, final V value )
public V remove( final K key )
public int size()
public List<K> keys()
public List<V> values()
public void clear()
```

Constraints:

- Open hashing = open addressing, **no separate chaining** on collisions
- **No wrapper objects** for storage (no `Entry`-style nodes)
- **Allocation-free storage** except for the backing structure
- Not thread-safe
- Collision strategy free to choose
- Unbounded capacity
- No null keys (rejected), null values permitted
- Maven build; JUnit 5; Java 21; `org.jugsaxony.tdd`

## 2. Process

1. **Red phase** — created the Maven project (`pom.xml`: `maven.compiler.release=21`, JUnit Jupiter 5.10.2, Surefire 3.2.5), a stub `TDDHashMap` whose methods all throw `UnsupportedOperationException`, and the full test suite `TDDHashMapTest.java` (45 tests in 9 nested groups).
   Verified with `mvn test`: all 45 tests compiled and failed with `UnsupportedOperationException` — the expected TDD red state.
2. **Human review checkpoint** — stopped and waited for approval.
3. **Green phase** — on "go", implemented `TDDHashMap` and re-ran `mvn test`: **45/45 tests pass, BUILD SUCCESS**.

## 3. Test Suite (`src/test/java/org/jugsaxony/tdd/TDDHashMapTest.java`)

45 tests organized in nested groups:

| Group | Focus |
|---|---|
| Construction | empty state, null returns on empty map, no-op clear, non-null lists |
| PutAndGet | basic put/get, previous-value return, overwrite semantics, absent keys, equals-not-identity lookup |
| NullHandling | NPE on null keys (put/get/remove), null values stored/counted/overwritten/removed/listed |
| Remove | return values, absent keys, double remove, re-insert after remove, remove-all |
| Size | tracking across put/overwrite/remove/failed-remove/clear |
| KeysAndValues | completeness, duplicates, removal reflection, snapshot semantics (mutating returned lists must not affect the map) |
| Clear | empties map, full reusability afterwards, clear after churn |
| Collisions | 100 keys with identical `hashCode` (`CollidingKey`), overwrite of colliding keys, removal from the middle of a probe chain, re-insert into freed slots, pathological hashes (0, −1, `Integer.MIN_VALUE`, `Integer.MAX_VALUE`) |
| Growth | 100,000 entries, 1,000 full-collision entries, 20,000-round randomized put/remove churn verified against `java.util.HashMap` as reference (seeded `Random`), repeated fill/clear cycles |

Helper key types in the tests:

- `CollidingKey` — every instance returns `hashCode() == 42`; equality by id. Forces worst-case probe chains.
- `HashKey` — configurable (possibly pathological) `hashCode`; equality by id.

## 4. Implementation (`src/main/java/org/jugsaxony/tdd/TDDHashMap.java`)

Design decisions:

- **Collision strategy**: linear probing with power-of-two table sizes.
- **Storage**: two parallel `Object[]` arrays (`keys`, `values`) — no chaining, no entry wrapper objects. The only allocations are the backing arrays (and their replacements on growth).
- **Empty-slot marker**: a `null` in the keys array. Safe because null keys are rejected with `NullPointerException` (`Objects.requireNonNull` in `get`/`put`/`remove`). Null values are stored normally and remain distinguishable from empty slots.
- **Hash spreading**: `h ^ (h >>> 16)`, index computed as `hash & (capacity - 1)`. Bit masking (instead of modulo/abs) is safe for every `hashCode`, including negative values and `Integer.MIN_VALUE`.
- **Deletion**: backward-shift deletion (`backwardShiftDelete`) — no tombstones. After clearing the hole, subsequent occupied slots are scanned; an entry moves back into the hole iff its natural slot does **not** lie cyclically within `(hole, j]`, i.e. its probe path crossed the hole. This keeps every probe chain contiguous, so removals never degrade later lookups and freed slots are immediately reusable.
- **Growth**: unbounded capacity. When `size` exceeds the threshold (`capacity * 0.75`), the table doubles and all live entries are rehashed into the new arrays.
- **Lookup equality**: identity short-circuit first (`k[i] == key`), then `key.equals(k[i])`, matching `java.util.HashMap` semantics.
- **`keys()` / `values()`**: build fresh `ArrayList` snapshots sized to `size()`; mutating the returned lists does not affect the map. Order is unspecified (iteration over the backing array).
- **`clear()`**: `Arrays.fill` both arrays with `null` (releases value references) and resets `size`; capacity is retained, so the map is fully reusable.
- **Thread safety**: none, as required — no synchronization or volatile fields.

## 5. Verification

```
mvn test
...
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- Red phase confirmed before implementation: 45 errors, all `UnsupportedOperationException`.
- Green phase confirmed after implementation: 45/45 passing, including the randomized churn test against `java.util.HashMap` and the 100k-entry growth test.

## 6. Files

```
demo7/
├── pom.xml
├── doc/
│   └── implementation.md          (this log)
└── src/
    ├── main/java/org/jugsaxony/tdd/
    │   └── TDDHashMap.java
    └── test/java/org/jugsaxony/tdd/
        └── TDDHashMapTest.java
```
