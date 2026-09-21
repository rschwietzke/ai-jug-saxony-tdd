# Implementation Log — TDDHashMap

Session date: 2026-09-21 · Project: `org.jugsaxony:tdd-hashmap` · Java 21 · JUnit 5 · Maven

## Task

Implement an open-addressing hash map (`TDDHashMap.java`, package `org.jugsaxony.tdd`) with a strict
test-first (TDD) process: write the tests first, stop for human review, and only implement after a
human 'go'.

### Required API

```java
public TDDHashMap()
public V get(K key)
public V put(K key, V value)
public V remove(K key)
public int size()
public List<K> keys()
public List<V> values()
public void clear()
```

### Constraints

- Open hashing: probing in case of collisions, **no separate chaining**
- No per-entry wrappers; allocation-free storage except the backing structure
- Not thread-safe
- Collision strategy free to choose
- Unbound capacity
- No null keys; null values permitted
- Maven, JUnit 5, Java 21

## Phase 1 — Tests first (RED)

The working directory started empty. Scaffolded:

- `pom.xml` — Java 21 (`maven.compiler.release=21`), JUnit Jupiter 5.11.4, Surefire 3.5.2
- `src/main/java/org/jugsaxony/tdd/TDDHashMap.java` — compilable stub, all methods throw
  `UnsupportedOperationException`
- `src/test/java/org/jugsaxony/tdd/TDDHashMapTest.java` — 37 tests in 7 `@Nested` groups

Verified red state with `mvn test`: **37 run, 37 fail** (34 `UnsupportedOperationException` errors,
3 assertion failures expecting `NullPointerException`).

### Test coverage written

| Group           | Tests | Specification covered |
|-----------------|-------|-----------------------|
| `NewMap`        | 4     | empty size, unknown-key `get`/`remove` → `null`, empty `keys()`/`values()` |
| `PutAndGet`     | 8     | put/get roundtrip, put return values, replace semantics, equal-but-not-identical keys, null values permitted, multiple type parameters |
| `NullKeys`      | 4     | `get`/`put`/`remove(null)` → `NullPointerException`, also on populated map |
| `Remove`        | 5     | return value, key gone, double-remove, selectivity, reinsertion |
| `KeysAndValues` | 4     | exact contents (duplicates in `values()`), reflect removals, snapshot semantics |
| `Collisions`    | 6     | forced-hash clusters, replace within cluster, head/middle cluster removal, negative hashes incl. `Integer.MIN_VALUE`, 100-key interleaved put/remove/reinsert |
| `Growth`        | 3     | 10,000-entry growth, growth preserving collision clusters, 20,000-op deterministic mixed-load comparison vs `java.util.HashMap` (seeded `Random(42)`) |
| `Clear`         | 3     | empties map, no-op when empty, reusable afterwards |

### Decisions encoded in the tests (reviewed before implementation)

1. **Null keys → `NullPointerException`** (`Objects.requireNonNull` semantics).
2. **`keys()`/`values()` return independent snapshots**; iteration order unspecified (tests compare
   as sets/sorted copies).
3. **`get` returning `null` is ambiguous** for stored null values (no `containsKey` in the API);
   tested via `size()`/`keys()`/`values()` instead.
4. **Naming:** the task text mentioned `FastHashMap()` but required file name `TDDHashMap.java`;
   Java constructors must match the class name, so the class is `TDDHashMap` with constructor
   `public TDDHashMap()`.

Work stopped here for human review, per instructions.

## Phase 2 — Human review

User answered: **"go"** — approval to implement.

## Phase 3 — Implementation (GREEN)

Implemented `src/main/java/org/jugsaxony/tdd/TDDHashMap.java`:

- **Open addressing with linear probing** in one flat structure — no separate chaining.
- **Allocation-free storage:** two parallel plain `Object[]` arrays (`keys`, `values`); no
  `Map.Entry`/node wrappers. Allocation only when the backing structure is created or doubled.
- **Unbound capacity:** power-of-two sizing, initial capacity 16, load factor 0.75, doubling with
  full rehash.
- **Deletion:** backward-shift deletion — the remainder of the probe cluster is compacted one slot
  back, keeping all entries reachable without tombstones.
- **Hash spreading:** `h ^ (h >>> 16)` protects the power-of-two mask; `Integer.MIN_VALUE` safe.
- **Null handling:** `Objects.requireNonNull` for keys in `get`/`put`/`remove`; null values allowed.
- **`keys()`/`values()`:** independent `ArrayList` snapshots; **`clear()`** nulls arrays in place
  and resets size (backing structure reused).
- **Not thread-safe:** no synchronization, per spec.

### Verification

`mvn test` → **Tests run: 37, Failures: 0, Errors: 0 — BUILD SUCCESS.**

## Final project layout

```
demo2/
├── pom.xml
├── doc/
│   └── implementation.md          (this log)
└── src/
    ├── main/java/org/jugsaxony/tdd/TDDHashMap.java
    └── test/java/org/jugsaxony/tdd/TDDHashMapTest.java
```
