# TDDHashMap

An open-addressing hash map (`org.jugsaxony.tdd.TDDHashMap<K, V>`), built test-first.

**Status: GREEN.** 101 test cases, all passing, ~1.3 s.

## The contract

| Question | Decision |
|---|---|
| Collision handling | Open addressing — collisions resolved inside the backing arrays, never by chaining. |
| `keys()` / `values()` ordering | Unspecified, and **not** correlated by index. `keys().get(i)` says nothing about `values().get(i)`. |
| `keys()` / `values()` lifetime | Snapshots, not views. |
| `values()` multiplicity | One element per mapping — duplicates and `null`s included. |
| `null` keys | Rejected with `IllegalArgumentException` from `get`, `put` and `remove`, leaving the map untouched. |
| `null` values | Stored as real entries. `get()` returning `null` is therefore ambiguous — only `size()`/`keys()` separate "absent" from "mapped to null". |
| `put` / `remove` return | Previous / removed value, or `null` if the key was absent. |
| Capacity | Unbound; grows as needed. |
| Storage | No per-entry wrapper objects, no collections inside. Allocation free apart from the backing arrays. |
| Threading | Not thread-safe. |

A note on naming: the brief's "open hashing" is defined there as *no separate
chaining*, i.e. what the literature usually calls **open addressing** / closed hashing.

## How it works

**Two parallel arrays**, `keys` and `values`, plus an `int size`. Slot `i` holds a
mapping exactly when `keys[i] != null` — which is why `null` keys are rejected: `null`
is the empty marker. Values are never consulted to decide whether a slot is taken, so
`null` values cost nothing special. No `Entry` object exists, at any point.

**Linear probing** over a power-of-two capacity, kept at most half full so a probe
always terminates.

**Backward-shift deletion** (Knuth, TAOCP vol. 3, algorithm 6.4R) instead of
tombstones. On removal the rest of the cluster is pulled back into the gap, leaving the
table exactly as if the removed key had never been inserted. Two consequences worth the
trouble:

- a lookup may stop at the first empty slot, because no slot ever lies about being
  empty;
- repeated insert/remove at constant size never degrades the table or forces it to
  grow. Verified: 300 000 insert/remove cycles leave the capacity at 256.

**Hash mixing** is the murmur3 32-bit finalizer, not `java.util.HashMap`'s
`h ^ (h >>> 16)`. See the performance note below — this one is not optional.

## Test suite — 101 test cases

| File | What it holds down |
|---|---|
| `TDDHashMapContractTest` | Construction, put/get/remove return values, overwrite semantics, `size()` across mixed sequences, `clear()` and reuse afterwards. |
| `TDDHashMapNullTest` | `IllegalArgumentException` on null keys (and that rejection is side-effect free); null values as first-class entries, including under collisions. |
| `TDDHashMapKeysValuesTest` | Element sets and multiplicities, snapshot semantics, self-consistency between the two lists, behaviour under collisions. |
| `TDDHashMapCollisionTest` | 1 000 keys on one hash code; removal from head and middle of a probe chain; slot reuse without duplicate entries; 2 000 rounds of churn; `Integer.MIN_VALUE` / `0` / `MAX_VALUE` hashes; hashes differing only above the low bits; a chain surviving the resize it triggers. |
| `TDDHashMapStressTest` | 200 000 entries, removal of half, refill after `clear()`, repeated grow/shrink, a hostile insertion order — plus differential runs of 100 000 / 50 000 / 20 000 random operations cross-checked step by step against `java.util.HashMap`. |
| `TDDHashMapDesignConstraintsTest` | The structural rules behaviour can't express: no nested `Entry`/`Node` class, storage is arrays and primitives only, no collection type anywhere, no nested arrays, capacity stable under constant-size churn, and `get`/overwriting `put`/`remove` allocate nothing (HotSpot-only probe, tagged `allocation`). |

`CollidingKey` is the test helper that makes hash codes controllable.

Drop `TDDHashMapDesignConstraintsTest` and an implementation backed by
`ArrayList<Entry>[]` passes everything else in the suite. Those checks are the
executable form of "no wrappers, no chaining, allocation free".

## Running it

```bash
mvn test
```

Requires JDK 21+. To skip the allocation probe (it needs a HotSpot JVM and a reasonably
quiet machine):

```bash
mvn test -Dsurefire.excludedGroups=allocation
```

## Two things that came out of going green

### A test that asserted the wrong thing

`freedSlotIsReusedNotDuplicated` read:

```java
assertNull(map.put(b, "2-updated"), "put must report b's previous value");
```

The assertion contradicts its own message — `b` is still mapped, so `put` must return
`"2"`. It was fixed to `assertEquals("2", ...)`, which is also the stronger assertion:
an implementation that duplicates `b` into the freed slot returns `null` there.

The red phase could not have caught this. Every test failed with
`UnsupportedOperationException`, including this one, for reasons that had nothing to do
with what it asserted. Red proves a test *fails*; only green proves it fails *for the
right reason*.

### `h ^ (h >>> 16)` is the wrong mix for linear probing

The first implementation borrowed `java.util.HashMap`'s spreader. For small integers
that expression is the identity, so 200 000 sequential keys occupied 200 000
*consecutive* slots: a single cluster spanning the whole populated region. Insertion
stayed fast — every key landed on its own slot — but backward-shift deletion walks to
the end of the cluster, so each removal became a 200 000-step scan.

`Growth.removesHalf` took **16.4 seconds**; the suite took 35.

`HashMap` gets away with the weak mix because separate chaining does not care how keys
cluster — only which bucket they land in. Open addressing cares enormously. Switching
to the murmur3 finalizer:

| | before | after |
|---|---|---|
| `Growth.removesHalf` | 16 448 ms | 152 ms |
| full suite | 35 s | 1.3 s |
| max cluster, 200k sequential keys, evens removed | ~200 000 | 10 |

### Independent verification

Beyond the suite, checked separately: 12 fuzz trials against `java.util.HashMap` with
fresh random seeds and deliberately few distinct hash codes; a structural invariant
check that every stored key is reachable from its home slot without crossing a hole;
capacity stability across 300 000 churn cycles; and an allocation probe reporting
**0 bytes** across 1 600 000 operations. Throughput on 1M put + 1M get: 244 ms against
`java.util.HashMap`'s 296 ms.

### One caveat about this checkout

The cloud session that wrote these files could not reach Maven Central — the egress
policy returns `403` for `repo.maven.apache.org` — so `mvn test` was never executed
here. The suite was compiled with `javac --release 21` (clean, no warnings) and run
against a minimal local stand-in for the JUnit 5 API. Please run `mvn test` once
locally to confirm dependency resolution; `junit.version` is pinned to `5.11.4`.
