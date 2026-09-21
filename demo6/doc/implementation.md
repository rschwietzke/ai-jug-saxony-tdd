# Implementation log — TDDHashMap

A record of the session that produced this project, written as it happened.

- **Date:** 2026-09-21
- **Assistant:** Claude (Cowork), model `claude-opus-5`
- **Working folder:** `demo6` (empty at the start — greenfield)
- **Result:** 101 test cases, all green, ~1.3 s

---

## 1. The brief

> Create a new implementation of an open hashing map and all required tests.
> `K` and `V` are the generics. Open hashing means no separate chaining in case of
> collisions. No wrappers for storage. Allocation free storage except for the backing
> structure.

Required signatures:

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

Constraints: not thread-safe · free choice of collision strategy · unbound capacity ·
no null keys, null values permitted · Maven · JUnit 5, Java 21, `org.jugsaxony.tdd` ·
file name `TDDHashMap.java`.

Process constraint: **write the tests first, stop for human review, wait for a "go"
before implementing.**

### A note on the name

The brief defines "open hashing" as *no separate chaining*. The literature usually
calls that **open addressing** (or closed hashing), and reserves "open hashing" for
chaining — the exact opposite. The brief's definition is unambiguous, so it won.

---

## 2. Environment check

Before anything else:

- `demo6` was empty.
- Java 21.0.10 and Maven 3.9.11 available in the session container.
- No shell on the local machine, so the build had to run in the container and the
  files be written back.

---

## 3. Four questions before writing a line

The brief was detailed but left four decisions that change what the tests assert. Each
was asked before any code was written.

| Question | Answer chosen |
|---|---|
| What do `keys()` / `values()` guarantee about ordering? | **No guarantee at all.** Lists are independent; no positional correlation. Tests compare sets and multisets, never positions. |
| What does `put(key, null)` mean? | **Stores a real entry.** `size()` counts it, `keys()` lists it. Matches `java.util.Map`. |
| How is a null key rejected? | **`IllegalArgumentException`** (rather than `NullPointerException`). |
| How deep should the suite go? | **Contract + collisions + stress**, including a randomized cross-check against `java.util.HashMap`. |

The second answer has a consequence worth stating out loud, because it drove several
tests: with null values legal and no `containsKey` in the API, `get()` returning `null`
is **ambiguous**. Only `size()` and `keys()` can separate "absent" from "mapped to
null".

---

## 4. Red phase

### The compilation problem

Java tests cannot compile against a class that does not exist, so a strict "no
production code before the tests" is impossible in this language. The resolution:
`TDDHashMap` was written with full signatures and Javadoc, every method throwing
`UnsupportedOperationException`.

The constructor deliberately does **not** throw. If it did, every test would fail at
setup and the failure would say nothing about which operation is missing.

### The suite — 101 cases across six files

| File | Holds down |
|---|---|
| `TDDHashMapContractTest` | Construction, put/get/remove return values, overwrite semantics, `size()` across mixed sequences, `clear()` and reuse. |
| `TDDHashMapNullTest` | `IllegalArgumentException` on null keys and that rejection is side-effect free; null values as first-class entries, including under collisions. |
| `TDDHashMapKeysValuesTest` | Element sets and multiplicities, snapshot-not-view semantics, cross-consistency of the two lists. |
| `TDDHashMapCollisionTest` | 1 000 keys on one hash code; removal from head and middle of a probe chain; slot reuse without duplicates; 2 000 rounds of churn; `Integer.MIN_VALUE` / `0` / `MAX_VALUE` hashes; hashes differing only above the low bits; a chain surviving the resize it triggers. |
| `TDDHashMapStressTest` | 200 000 entries, removal of half, refill after `clear()`, repeated grow/shrink, hostile insertion order; differential runs of 100 000 / 50 000 / 20 000 random operations checked step by step against `java.util.HashMap`. |
| `TDDHashMapDesignConstraintsTest` | The structural rules: no nested `Entry`/`Node` class, arrays and primitives only, no collection type anywhere, no nested arrays, capacity stable under constant-size churn, and `get`/`put`/`remove` allocating nothing. |

`CollidingKey` is the test helper that makes hash codes controllable: identity comes
from an id, the hash code is whatever the test says it is.

### Why the structural tests exist

Delete `TDDHashMapDesignConstraintsTest` and an implementation backed by
`ArrayList<Entry>[]` passes every other test in the suite. Those checks are the
executable form of *"no wrappers, no chaining, allocation free"* — the three
requirements that are invisible to behaviour.

They are also the only tests that reach inside the class, via reflection. Four of the
five passed against the empty skeleton, because a class with no fields trivially has no
collections and no nested arrays. `hasBackingArrays` was the one that failed red.

### Maven Central was blocked

`mvn test` could not run: the session's egress policy returns `403` for
`repo.maven.apache.org`, so JUnit could not be resolved. Per the proxy's own
documentation, a policy denial is to be reported rather than routed around.

Rather than hand over an unverified suite, the red phase was confirmed locally:

1. compiled with `javac --release 21 -Xlint:all` — clean, no warnings;
2. executed against a minimal local stand-in for the JUnit 5 API (annotations,
   `Assertions`, `Assumptions`, `@ParameterizedTest`/`@ValueSource`) plus a small
   reflective runner handling `@Nested` and `@BeforeEach`.

This proves the test code is type-correct against the real API shapes and that every
test actually runs. It does **not** prove dependency resolution — that still needs one
local `mvn test`.

### Red result

```
tests run : 101
passed    :   4   (structural checks that hold vacuously on an empty class)
failed    :  97
skipped   :   0
```

Of the 97, 92 failed with `UnsupportedOperationException` and 5 with `AssertionError` —
the four null-key tests (where `assertThrows` saw `UnsupportedOperationException`
instead of `IllegalArgumentException`) and `hasBackingArrays`. All correct for red.

**Stopped here for review.**

---

## 5. Green phase

Approved with "go".

### Design

**Two parallel arrays** — `Object[] keys`, `Object[] values` — plus an `int size`. Slot
`i` holds a mapping exactly when `keys[i] != null`. That is why null keys are rejected:
`null` is the empty marker. Values are never consulted to decide occupancy, so null
values cost nothing special. No `Entry` object exists at any point, which is what
"no wrappers" means in practice.

**Linear probing** over a power-of-two capacity, kept at most half full so a probe
always terminates on an empty slot. Index is `hash & mask` — never `%`, never
`Math.abs`, both of which break on `Integer.MIN_VALUE`.

**Backward-shift deletion** (Knuth, TAOCP vol. 3, algorithm 6.4R) instead of
tombstones. On removal the rest of the cluster is pulled back into the gap, leaving the
table exactly as if the removed key had never been inserted. This was the central
decision, and it pays twice:

- a lookup may stop at the first empty slot, because no slot ever lies about being
  empty. That in turn lets `slotOf` return `~slot` — the free slot it stopped on — so
  `put` never probes twice;
- repeated insert/remove at constant size never degrades the table or forces it to
  grow, which is what `churnDoesNotGrowStorage` demands. With tombstones that test
  would require a separate reclamation threshold; here it holds by construction.

### First run: 100 / 101

One failure, and it was in the **test**, not the implementation:

```java
assertNull(map.put(b, "2-updated"), "put must report b's previous value");
```

The assertion contradicts its own message. `b` is still mapped at that point, so `put`
must return `"2"` — which is what the implementation returned. Corrected to:

```java
assertEquals("2", map.put(b, "2-updated"), "put must report b's previous value");
```

That is also the stronger assertion: an implementation that duplicates `b` into the
freed slot is precisely the one that returns `null` there.

**The red phase could not have caught this.** The test failed red like every other, with
`UnsupportedOperationException`, for reasons that had nothing to do with what it
asserted. Red proves a test *fails*; only green proves it fails *for the right reason*.
This is the one case in the session where the safety net had a hole in it, and it is
worth keeping in the talk.

### Second run: 101 / 101 — but 35 seconds

Green, but `Growth.removesHalf` alone took **16.4 seconds**. That is a finding, not a
slow test.

The first implementation borrowed `java.util.HashMap`'s spreader, `h ^ (h >>> 16)`. For
integers below 65 536 that expression is the **identity**. So 200 000 sequential keys
occupied 200 000 *consecutive* slots: one cluster spanning the entire populated region
of the table.

Insertion stayed fast — every key landed on its own home slot, no probing. But
backward-shift deletion walks forward to the end of the cluster, and every entry in it
sat at its own home, so nothing ever moved and the walk ran the full length. Each
removal became a 200 000-step scan; 100 000 removals gave the 16 seconds.

`HashMap` gets away with the weak mix because separate chaining does not care how keys
*cluster*, only which bucket they land in. Open addressing cares enormously. Replaced
with the murmur3 32-bit finalizer:

```java
mixed ^= mixed >>> 16;
mixed *= 0x85ebca6b;
mixed ^= mixed >>> 13;
mixed *= 0xc2b2ae35;
mixed ^= mixed >>> 16;
```

| | before | after |
|---|---|---|
| `Growth.removesHalf` | 16 448 ms | 152 ms |
| full suite | 35 s | 1.3 s |
| max cluster — 200k sequential keys, evens removed | ~200 000 | 10 |

A 108× improvement on the worst test, from five lines of arithmetic. The lesson
generalises: **a hash mix is only good relative to a collision strategy.**

---

## 6. Independent verification

Written separately from the suite, against the class rather than derived from the
tests:

| Check | Result |
|---|---|
| 12 fuzz trials vs `java.util.HashMap`, fresh random seeds, 60 000 ops each, deliberately few distinct hash codes, null values, interleaved `clear()` | all match |
| Structural invariant: every stored key reachable from its home slot without crossing a hole | holds after single-hash clusters and after 200k sequential inserts with evens removed |
| Capacity stability: 300 000 insert/remove cycles at constant size | capacity 256, unchanged |
| Allocation: 1 600 000 `get`/`put`/`remove` operations | **0 bytes** |
| Throughput: 1M put + 1M get | 244 ms vs `java.util.HashMap` 296 ms |

---

## 7. Final state

```
tests run : 101
passed    : 101
failed    :   0
skipped   :   0
```

Compiles clean under `javac --release 21 -Xlint:all -Werror`.

### Still open

`mvn test` has never been executed — Maven Central is unreachable from the session
container. Dependency resolution is the one thing not confirmed. `junit.version` is
pinned to `5.11.4`; bump freely.

### Files

```
pom.xml
README.md
doc/implementation.md
src/main/java/org/jugsaxony/tdd/TDDHashMap.java
src/test/java/org/jugsaxony/tdd/CollidingKey.java
src/test/java/org/jugsaxony/tdd/TDDHashMapContractTest.java
src/test/java/org/jugsaxony/tdd/TDDHashMapNullTest.java
src/test/java/org/jugsaxony/tdd/TDDHashMapKeysValuesTest.java
src/test/java/org/jugsaxony/tdd/TDDHashMapCollisionTest.java
src/test/java/org/jugsaxony/tdd/TDDHashMapStressTest.java
src/test/java/org/jugsaxony/tdd/TDDHashMapDesignConstraintsTest.java
```

---

## 8. What the session says about TDD

Three things worth carrying into the talk.

**Red is necessary, not sufficient.** Ninety-seven failing tests looked like a complete
safety net. One of them asserted the opposite of what its own message claimed, and red
could not tell — every test fails for the same boring reason in that phase. The
assertion is only validated when it passes for the right reason.

**Some requirements are invisible to behaviour.** "No wrappers", "no chaining" and
"allocation free" cannot be expressed as input/output pairs. They needed reflection and
an allocation counter. Without those six tests, a chaining implementation would have
been indistinguishable from a correct one.

**Correctness tests found a performance bug by accident.** `Growth.removesHalf` was
written to check that removing half a large map leaves the other half intact. It
passed — in 16 seconds. Nothing asserted a time limit; the number was simply visible,
and visible is enough. Scale in the test suite is what made a pathological hash
distribution observable at all.
