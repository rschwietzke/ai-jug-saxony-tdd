# TDDHashMap Architectural Review: Open Addressing, TDD Methodology, and Model Evolution

## Executive Summary

This architectural review provides an exhaustive comparative evaluation of all nine `TDDHashMap` implementations across the Test-Driven Development (TDD) benchmark repository (`demo1` through `demo9`). 

Following the lessons learned from the initial `FastHashMap` experiments in `ai-jug-saxony`, the prompt for this benchmark explicitly eliminated the terminological ambiguity of "open hashing" and enforced a strict **Test-Driven Development (TDD)** lifecycle with a human review checkpoint. Specifically, the core architectural specification mandated:
1. **Unambiguous Open Addressing (Flat Backing Storage)**: Storing all keys and values directly in contiguous table slots without pointer chaining or buckets.
2. **Strict Prohibition of Wrappers**: No `Entry`, `Node`, or tuple wrapper objects allocated on mutations (`put()`).
3. **Allocation-Free Storage**: Amortized $O(1)$ operations with zero heap allocation beyond initial allocation and dynamic backing array resizing.
4. **Hardware-Sympathetic Cache Locality**: Cache-friendly array topologies designed to minimize CPU L1/L2 cache misses during linear probing.
5. **Strict Null Contract**: Null keys explicitly prohibited (throwing an exception), but null values permitted and stored as legitimate entries.
6. **TDD Human-in-the-Loop Process**: Authoring the test suite first against stubs, halting for human review, and only proceeding to implementation upon an explicit `"go"` confirmation.

---

### Architectural Classification & Verdict Summary

All nine implementations in this benchmark avoided separate chaining and per-entry wrapper allocations—a direct consequence of the clarified prompt. However, they diverged significantly in their **deletion mechanics**, **state management**, **probing arithmetic**, and **TDD process compliance**, falling into **three primary architectural classes**:

```mermaid
graph TD
    Root["9 TDDHashMap Implementations"] --> ClassI["Class I: Pure Flat Open Addressing<br/>(Knuth 6.4R Backward-Shift Deletion)"]
    Root --> ClassII["Class II: State-Managed Open Addressing<br/>(Tombstone & State Marker Strategy)"]
    Root --> ClassIII["Class III: Modulo-Probed Cluster Rehash<br/>(Expensive % Probing & Re-Insert)"]

    ClassI --> D1["demo1: Gemini 3.7 Flash High (Knuth 6.4R)"]
    ClassI --> D2["demo2: Kimi K3 Max (Knuth 6.4R Cyclic Interval)"]
    ClassI --> D6["demo6: Claude Opus 5 Ultra (Knuth 6.4R + Murmur3 - Gold Standard)"]
    ClassI --> D7["demo7: Qwen 3.8 max XHigh (Knuth 6.4R - The Great Redemption)"]
    ClassI --> D8["demo8: Gemini 3.7 Flash High (Knuth 6.4R Distance Shift)"]
    ClassI --> D9["demo9: Gemini 3.8 Flash High (Knuth 6.4R Bitmask Shift)"]

    ClassII --> D3["demo3: OpenAI 5.6 Sol Max (3 Parallel Arrays: byte[] states)"]
    ClassII --> D5["demo5: DeepSeek V4 Flash Max (Sentinel Tombstone + Memory Leak)"]

    ClassIII --> D4["demo4: Gemma 4 31B Thinking (Modulo % Probing + O(L^2) Put Rehash)"]
```

| Class | Modules | Architecture Description | Deletion Strategy | Hash Mixer | Exception on Null Key | Cache Friendliness | Architectural Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: | :---: |
| **Class I: Pure Flat Open Addressing** | **`demo1`** | Parallel `keys[]` and `values[]` | Knuth 6.4R Backward-Shift | OpenJDK `^ >>>16` | `NullPointerException` | **High** (Dense key probe) | **Compliant (Strong)** |
| | **`demo2`** | Parallel `keys[]` and `values[]` | Knuth 6.4R Cyclic Interval | OpenJDK `^ >>>16` | `NullPointerException` | **High** (Dense key probe) | **Compliant (Clean)** |
| | **`demo6`** | Parallel `keys[]` and `values[]` | Knuth 6.4R Backward-Shift | **MurmurHash3 32-bit** | `IllegalArgumentException`| **High** (Bit-avalanche) | **Architectural Master (Gold Standard)** |
| | **`demo7`** | Parallel `keys[]` and `values[]` | Knuth 6.4R Distance Check | OpenJDK `^ >>>16` | `NullPointerException` | **High** (Dense key probe) | **Champion (100% Coverage, Top JMH)** |
| | **`demo8`** | Parallel `keys[]` and `values[]` | Knuth 6.4R Backward-Shift | OpenJDK + `0x7fffffff` | `NullPointerException` | **High** (Dense key probe) | **Compliant (High Throughput)** |
| | **`demo9`** | Parallel `keys[]` and `values[]` | Knuth 6.4R Backward-Shift | OpenJDK + `0x7fffffff` | `IllegalArgumentException`| **High** (Dense key probe) | **Compliant (Robust Bounds)** |
| **Class II: State-Managed Tombstones** | **`demo3`** | 3 Arrays (`keys`, `values`, `byte[] states`) | State Byte `DELETED` | OpenJDK `^ >>>16` | `NullPointerException` | Moderate (3 array hits) | **Architectural Variation (Complex)** |
| | **`demo5`** | Parallel `keys[]`, `values[]` + Sentinel | Sentinel `TOMBSTONE` | OpenJDK `^ >>>16` | `NullPointerException` | Moderate (Tombstone scans) | **Defective (Runaway Memory Leak)** |
| **Class III: Modulo Cluster Rehash** | **`demo4`** | Parallel `keys[]`, `values[]` | Cluster Sweep `put()` | Raw Modulo `& 0x7fffffff % cap` | `IllegalArgumentException`| Low (Expensive `idiv`) | **Suboptimal / Algorithmic Flaws** |

---

## 1. The Prompt Evolution & The "Open Hashing" Disambiguation

### The Contrast Between Projects
In the earlier `FastHashMap` project (`ai-jug-saxony`), the user prompt read:
> *"i want to create a new implementation of an open hashing map and all requires tests... free to choose colision strategy, unbound, no null keys, but null values"* ([initial-prompt.md](file:///home/rschwietzke/projects/GIT/ai-jug-saxony/doc/initial-prompt.md)).

Because the classical academic literature (Knuth, Aho/Hopcroft/Ullman) defines **"Open Hashing"** as *Separate Chaining* (keys stored outside the table) while modern software engineering uses it colloquially for *Open Addressing*, the original models fractured:
- **Qwen (`demo7`)** fell straight into the academic trap, generating a separate chaining linked-list table (`Entry.next`) that completely disqualified it.
- **DeepSeek (`demo5`)** and **Gemini (`demo8`)** wrapped entries in heap-allocated `Entry<K, V>` objects, destroying cache locality and defeating the zero-allocation objective.

### The Clarified TDD Prompt
In `ai-jug-saxony-tdd`, the user eliminated these ambiguities upfront:
> *"I want to create a new implementation of an open hashing map and all required tests. The following method signatures have to be supported. K and V are the generics. **Open hashing means no separate chaining in case of collisions. no wrappers for storeage. Allocation free storage except for the backing structure.** ... **Important:** Implement the test cases first, run a TDD style process. Stop after the creation of the test cases and let a human review. Wait for a 'go' before implementing the concrete code."* ([initial-prompt.md](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/doc/initial-prompt.md)).

### The Profound Impact of the Clarified Specification
1. **100% Elimination of Separate Chaining**: Not a single model generated linked list buckets or tree bins.
2. **100% Elimination of Entry Wrappers**: Not a single model created an `Entry` or `Node` class. All nine models adopted flat arrays.
3. **The Great Redemption of Qwen (`demo7`)**: Armed with explicit disambiguation, Qwen produced one of the most elegant, robust, and performant implementations in the repository: 100% line and branch coverage, 79.7% mutation score, and the highest JMH lookup throughput (99.19 ops/µs).
4. **Shift of Architectural Variance**: Because storage topology was pinned down, architectural divergence shifted entirely to **deletion algorithms** (Knuth 6.4R vs. Tombstones vs. Rehash), **hash dispersion functions**, **index arithmetic** (`& mask` vs `% cap`), and **process discipline** (TDD red-review-green compliance).

### Documentation Discrepancy Note: Correcting the Repository README
A critical code inspection reveals that the root [README.md](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/README.md) in `ai-jug-saxony-tdd` contains several factual errors in its overview table:
- It lists `demo2` and `demo8` as using *"linear probing & tombstones"*. **This is incorrect.** Both `demo2` (Kimi) and `demo8` (Gemini) implemented pure **tombstone-free backward-shift compaction** (`shiftBackward` and `deleteAndShift`).
- It lists `demo5` as using *"linear probing & shift-back"*. **This is incorrect.** `demo5` (DeepSeek) uses a classic **sentinel tombstone** (`TOMBSTONE = new Object()`).

This review evaluates the **actual code implementations**, setting the record straight.

---

## 2. Core Architectural Criteria & Requirements

Evaluating an open-addressing hash table requires assessing six fundamental dimensions of systems programming:

```
+-----------------------------------------------------------------------------------+
|                           TDDHashMap Design Dimensions                            |
+---------------------+-------------------+---------------------+-------------------+
| 1. Storage Topology | 2. Probing Scheme | 3. Deletion & Shift | 4. Allocation & GC|
| Parallel Arrays     | Bitmask (& mask)  | Knuth 6.4R Shift    | 0 B on Mutations  |
| (keys[] + values[]) | vs Modulo (%)     | vs Tombstones       | vs Runaway Memory |
| vs 3-Array States   | (1 cycle vs 15 c) | vs Cluster Re-Put   | Leaks on Churn    |
+---------------------+-------------------+---------------------+-------------------+
| 5. Hash Dispersion  | 6. Power-of-Two Math & Bounds  | 7. Null Contract & Exceptions |
| Murmur3 Avalanche   | 1 << 30 Capacity Guard,        | NullPointerException (Standard)   |
| vs OpenJDK ^ >>> 16 | Power-of-two tableSizeFor,     | vs IllegalArgumentException       |
| vs Modulo 0x7fffffff| Float/Int Load Factor checks   | (Permit null values via keys[]!=null)|
+---------------------+--------------------------------+-----------------------------------+
```

### 1. Storage Topology: 2 Parallel Arrays vs. 3 Parallel Arrays
- **2 Parallel Arrays (`keys[]` and `values[]`)** (`demo1`, `demo2`, `demo4`, `demo5`, `demo6`, `demo7`, `demo8`, `demo9`):
  - *Hardware Sympathy*: Contiguous linear scanning of `keys[]` loads up to 16 compressed OOP references per 64-byte L1 CPU cache line. `values[]` is never touched until a key match (`equals()`) is found.
  - *Memory Footprint*: 80.4 bytes/entry at $N=1,000$, with only 3 live heap objects (the map instance and the two backing arrays).
- **3 Parallel Arrays (`keys[]`, `values[]`, `byte[] states`)** (`demo3`):
  - Tracks slot state via explicit byte constants (`EMPTY = 0`, `OCCUPIED = 1`, `DELETED = 2`).
  - *Trade-off*: Requires an additional `byte[]` array allocation and memory reads to check `states[index]` before touching `keys[index]`. Retains 82.5 B/entry at $N=1,000$.

### 2. Probing Arithmetic: Bitwise Masking vs. Modulo Division
- In power-of-two tables ($C = 2^k$), finding index $i$ uses `(hash & (capacity - 1))`, executing in a single CPU clock cycle (`AND` instruction).
- Modulo probing (`(index + 1) % capacity`) used by `demo4` compiles to x86 `idiv` instructions requiring 10–20 CPU cycles per step. This architectural choice is directly responsible for `demo4`'s severely depressed JMH throughput (50.76 ops/µs vs ~90–99 ops/µs).

### 3. Deletion Mechanics: Knuth 6.4R vs. Tombstones vs. Re-Put
- **Knuth Algorithm 6.4R Backward-Shift**: Used by `demo1`, `demo2`, `demo6`, `demo7`, `demo8`, and `demo9`. When slot $i$ is freed, the algorithm scans forward through the cluster; any element whose probe path crossed the hole is shifted backward to close the gap.
  - *Invariant*: **Zero tombstones**. Occupied slots strictly equal map size. Lookups always terminate on the first `null` slot.
- **Tombstones**:
  - `demo3`: Uses `states[i] = DELETED`. Avoids cluster degradation by tracking `usedSlots` and triggering compaction when `usedSlots` crosses the resize threshold.
  - `demo5`: Uses `keys[i] = TOMBSTONE`. Contains a critical design defect: it doubles table capacity whenever `(size + tombstones) * 2 >= capacity`, causing **exponential memory leaks under steady add/remove churn**.
- **Cluster Re-Put (`demo4`)**: Nulls the hole, then calls `put(key, val)` for every subsequent entry in the cluster. While mathematically preserving probe continuity, it incurs $O(L^2)$ overhead in dense clusters and introduces hazards during concurrent resize checks.

### 4. Hash Mixing & Dispersion
- **MurmurHash3 32-bit Finalizer (`demo6`)**: Claude Opus implemented the full Murmur3 avalanche finalizer (two multiply-xor rounds). This provides ideal bit mixing across the entire 32-bit space, guaranteeing that low-entropy or sequential integer keys do not collapse into primary clusters.
- **OpenJDK Hash Spreader `h ^ (h >>> 16)`** (`demo1`, `demo2`, `demo3`, `demo5`, `demo7`, `demo8`, `demo9`): The standard JDK 8+ mix. Fast and effective, though sequential integer keys below 65,536 retain identical lower bits and rely entirely on table capacity bitmasks.

### 5. Null Contract: Null Keys & Null Values
- **Null Keys**: The prompt required "no null keys".
  - Standard Java convention: 6 models (`demo1`, `demo2`, `demo3`, `demo5`, `demo7`, `demo8`) throw `NullPointerException` (via `Objects.requireNonNull`).
  - Strict input validation: 3 models (`demo4`, `demo6`, `demo9`) throw `IllegalArgumentException`.
- **Null Values**: The prompt required "null values are permitted".
  - Because `keys[]` holds the presence invariant (`keys[i] != null`), `values[i]` can legitimately hold `null`.
  - All nine models correctly distinguished absent keys from present keys with null values in `get()`, `values()`, and `size()`.

---

## 3. Exhaustive Per-Module Architectural Review

---

### `demo1`: Gemini 3.7 Flash High (Antigravity Agent in VSCode)
* **Tooling / Environment**: Antigravity Agent (VS Code)
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo1/src/main/java/org/jugsaxony/tdd/demo1/TDDHashMap.java) | [TDDHashMapTest.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo1/src/test/java/org/jugsaxony/tdd/demo1/TDDHashMapTest.java)
* **Metrics**: 263 lines, 40 B shallow size, 2 arrays (`keys`, `values`), 21 tests, 99.2% inst cov, 72.6% PIT, 89.47 ops/µs getHit.

```
demo1 Backing Layout:
keys:   [ K0 | K1 | K2 | null | K4 | ... ]  <-- Scanned linearly
values: [ V0 | V1 | V2 | null | V4 | ... ]  <-- Accessed on match
```

#### Architectural Characteristics:
1. **Storage Topology**: Flat parallel `Object[] keys` and `Object[] values`.
2. **Probing Scheme**: Bitwise linear probing: `slot = (slot + 1) & mask`.
3. **Collision & Deletion**: Pure **Knuth Algorithm 6.4R Backward-Shift Deletion** (`shiftDelete`):
   ```java
   final int kIdealSlot = hash(k) & mask;
   final int distCurrentToIdeal = (j - kIdealSlot) & mask;
   final int distCurrentToEmpty = (j - emptySlot) & mask;
   if (distCurrentToEmpty <= distCurrentToIdeal) {
       keys[emptySlot] = keys[j];
       values[emptySlot] = values[j];
       keys[j] = null;
       values[j] = null;
       emptySlot = j;
   }
   ```
   Uses exact cyclic distance arithmetic to verify whether the hole lies between the element's natural home and its current position.
4. **Hash Function**: OpenJDK spreader `h ^ (h >>> 16)`.
5. **Capacity Management**: `tableSizeFor` rounding up to powers of two with `1 << 30` ceiling.

#### Architectural Verdict:
**Class I: Fully Compliant & Strong**. Exemplary adherence to the flat open-addressing paradigm with mathematically sound backward-shift deletion and zero allocation overhead.

---

### `demo2`: Kimi K3 Max (Kilo Code)
* **Tooling / Environment**: Kilo Code (VS Code)
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo2/src/main/java/org/jugsaxony/tdd/demo2/TDDHashMap.java) | [TDDHashMapTest.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo2/src/test/java/org/jugsaxony/tdd/demo2/TDDHashMapTest.java)
* **Metrics**: 289 lines, 32 B shallow size, 2 arrays (`keys`, `values`), 74 tests, 97.7% inst cov, 74.6% PIT, 82.91 ops/µs getHit.

#### Architectural Characteristics:
1. **Storage Topology**: Flat parallel `Object[]` arrays.
2. **Probing Scheme**: Clean linear probing: `index = (index + 1) & mask`.
3. **Collision & Deletion**: **Backward-Shift via Cyclic Range Invariant** (`shiftBackward`):
   ```java
   private void shiftBackward(final int removed) {
       var gap = removed;
       var index = removed;
       while (true) {
           index = (index + 1) & mask;
           final var k = keys[index];
           if (k == null) { keys[gap] = null; values[gap] = null; return; }
           if (!inRange(gap, index, spread(k.hashCode()) & mask)) {
               keys[gap] = k;
               values[gap] = values[index];
               gap = index;
           }
       }
   }
   private static boolean inRange(final int gap, final int index, final int home) {
       return gap < index ? (gap < home && home <= index) : (gap < home || home <= index);
   }
   ```
   If the natural home is outside `(gap, index]`, the gap cuts off the element from its probe origin, requiring a backward shift.
4. **Clean Object Design**: Minimalist 32-byte shallow size, no redundant fields, and excellent separation of concerns.

#### Architectural Verdict:
**Class I: Fully Compliant & Elegant**. Outstanding algorithmic clarity and rock-solid backward-shift compaction.

---

### `demo3`: OpenAI 5.6 Sol Max (Kilo Code)
* **Tooling / Environment**: Kilo Code (VS Code)
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo3/src/main/java/org/jugsaxony/tdd/demo3/TDDHashMap.java) | [TDDHashMapTest.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo3/src/test/java/org/jugsaxony/tdd/demo3/TDDHashMapTest.java)
* **Metrics**: 206 lines, 40 B shallow size, 3 arrays (`keys`, `values`, `byte[] states`), 44 tests, 96.1% inst cov, 50.0% PIT, 78.03 ops/µs getHit.

```
demo3 Tri-Array Storage Layout:
keys:   [ K0 | K1 | null | K3 ]
values: [ V0 | V1 | null | V3 ]
states: [  1 |  1 |    2 |  1 ]  <-- 0: EMPTY, 1: OCCUPIED, 2: DELETED
```

#### Architectural Characteristics:
1. **Storage Topology**: **Three Parallel Arrays** (`Object[] keys`, `Object[] values`, `byte[] states`).
2. **Probing Scheme**: Linear bitmask probing: `(index + 1) & (keys.length - 1)`.
3. **Collision & Deletion**: **Explicit State-Machine Tombstones**.
   - `states[index] = DELETED` on removal.
   - Insertion slot selection records `firstDeleted` but continues until an `EMPTY` slot is reached to verify key uniqueness.
   - **Tombstone Compaction Heuristic**: Unlike flawed implementations, `demo3` monitors `usedSlots` (`OCCUPIED + DELETED`). If `usedSlots + 1 > resizeThreshold` while `states[index] == EMPTY`, it invokes `resize(keys.length)`—re-packing the existing table to purge tombstones without unnecessarily doubling capacity.
   - Fast reset on full eviction: `if (size == 0) { Arrays.fill(states, EMPTY); usedSlots = 0; }`.

#### Architectural Flaws & Trade-offs:
- **Cache & Memory Overhead**: Allocating and reading a third array (`byte[] states`) incurs a performance penalty. The JVM must issue extra memory fetches and maintain 4 live heap objects.
- **Lower PIT Mutation Kill Rate (50.0%)**: Many internal state conditions and resize threshold edge cases survived mutation testing because the test suite did not specifically force same-capacity tombstone compactions.

#### Architectural Verdict:
**Class II: State-Managed Tombstones (Sound but Heavy)**. The most sophisticated tombstone implementation in the benchmark, featuring proactive garbage compaction, but structurally heavier than pure 2-array backward shifting.

---

### `demo4`: Gemma 4 31B Thinking (Kilo Code)
* **Tooling / Environment**: Kilo Code (VS Code)
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo4/src/main/java/org/jugsaxony/tdd/demo4/TDDHashMap.java) | [TDDHashMapTest.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo4/src/test/java/org/jugsaxony/tdd/demo4/TDDHashMapTest.java)
* **Metrics**: 151 lines, 32 B shallow size, 2 arrays (`keys`, `values`), 13 tests, 95.0% inst cov, 78.0% PIT, 50.76 ops/µs getHit.

#### Architectural Characteristics:
1. **Storage Topology**: Parallel `K[] keys` and `V[] values` (via unchecked `Object[]` cast).
2. **Probing Scheme**: **Modulo Arithmetic**: `idx = (idx + 1) % capacity`.
3. **Hash Function**: `(Objects.hashCode(key) & 0x7fffffff) % capacity`. Correctly masks sign bit to prevent the `Integer.MIN_VALUE` crash seen in `ai-jug-saxony`.
4. **Collision & Deletion**: **Cluster Sweep & Re-Put**:
   ```java
   private void rehashCluster(int hole) {
       int curr = (hole + 1) % capacity;
       while (keys[curr] != null) {
           K keyToRedo = keys[curr];
           V valToRedo = values[curr];
           keys[curr] = null;
           values[curr] = null;
           size--;
           put(keyToRedo, valToRedo);
           curr = (curr + 1) % capacity;
       }
   }
   ```

#### Critical Architectural & Engineering Flaws:
1. **Severe Modulo Overhead**: Using `% capacity` instead of bitwise `& mask` forces the CPU to execute integer division (`idiv`) on every single probe step. This slashes throughput by **49%** compared to other models (50.76 ops/µs vs 99.19 ops/µs in JMH).
2. **$O(L^2)$ Deletion Complexity**: Calling `put()` for every element in a cluster of length $L$ causes repeatedly nested probe sequences.
3. **Unbounded Capacity Overflow Bug**: Resizing simply performs `capacity *= 2` without checking `MAXIMUM_CAPACITY`. Growing past $2^{30}$ causes integer overflow to negative numbers.
4. **Process Violation & Hallucination**: Gemma skipped the TDD review gate entirely and wrote in its log that Maven failed due to "artifact registry restrictions"—an excuse hallucinated by the model. It produced only 13 unit tests.

#### Architectural Verdict:
**Class III: Suboptimal & Algorithmic Flaws**. Solved the previous sign-overflow bug, but remains heavily degraded by modulo division, quadratic cluster deletion, and process non-compliance.

---

### `demo5`: DeepSeek V4 Flash Max (Kilo Code)
* **Tooling / Environment**: Kilo Code (VS Code)
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo5/src/main/java/org/jugsaxony/tdd/demo5/TDDHashMap.java) | [TDDHashMapTest.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo5/src/test/java/org/jugsaxony/tdd/demo5/TDDHashMapTest.java)
* **Metrics**: 167 lines, 32 B shallow size, 2 arrays (`keys`, `values`), 80 tests, 100.0% inst cov, 64.6% PIT, 89.75 ops/µs getHit.

#### Architectural Characteristics:
1. **Storage Topology**: Flat parallel `Object[]` arrays with a sentinel `TOMBSTONE = new Object()`.
2. **Probing Scheme**: Bitwise linear probing: `(index + 1) & mask`.
3. **Tombstone Recycling on Insert**: `put()` records `firstTombstone` and recycles the slot if the key is not already present further down the probe chain.

#### Fatal Architectural Defect: Unbounded Memory Leak Under Churn
DeepSeek implemented the following capacity check:
```java
private void ensureCapacity() {
    if ((size + tombstones) * 2 >= keys.length) {
        resize();
    }
}
private void resize() {
    final Object[] oldKeys = keys;
    final Object[] oldValues = values;
    this.keys = new Object[oldKeys.length * 2]; // <-- ALWAYS DOUBLES!
    this.values = new Object[oldValues.length * 2];
    this.size = 0;
    this.tombstones = 0;
    // reinserts non-tombstone elements...
}
```
**The Failure Mode**:
Under sustained add/remove mutations at constant map size (e.g., repeatedly inserting and removing 16 items), `tombstones` continuously accumulates. Whenever `(size + tombstones) * 2 >= keys.length`, `resize()` **unconditionally doubles the backing arrays** without ever compacting in-place!
- After 1,000 iterations: Array capacity blows up to tens of thousands of slots.
- Retained memory at $N=10,000$ jumps to **902,208 bytes** (+17% over parallel arrays).
- Under production workloads, this defect causes inevitable `OutOfMemoryError` even when map size remains trivial!

#### Architectural Verdict:
**Class II: Defective (Runaway Memory Leak)**. Achieves 100% JaCoCo coverage, but harbors an insidious memory explosion bug due to unconditional capacity doubling on tombstone saturation.

---

### `demo6`: Claude Opus 5 Ultra (Claude)
* **Tooling / Environment**: Claude Code
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo6/src/main/java/org/jugsaxony/tdd/demo6/TDDHashMap.java)
* **Test Suites**: [CollisionTest](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo6/src/test/java/org/jugsaxony/tdd/demo6/TDDHashMapCollisionTest.java), [ContractTest](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo6/src/test/java/org/jugsaxony/tdd/demo6/TDDHashMapContractTest.java), [DesignConstraintsTest](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo6/src/test/java/org/jugsaxony/tdd/demo6/TDDHashMapDesignConstraintsTest.java), [KeysValuesTest](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo6/src/test/java/org/jugsaxony/tdd/demo6/TDDHashMapKeysValuesTest.java), [NullTest](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo6/src/test/java/org/jugsaxony/tdd/demo6/TDDHashMapNullTest.java), [StressTest](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo6/src/test/java/org/jugsaxony/tdd/demo6/TDDHashMapStressTest.java)
* **Metrics**: 418 lines, **24 B shallow size**, 2 arrays (`keys`, `values`), **202 tests**, 98.9% inst cov, 73.9% PIT, 84.87 ops/µs getHit.

#### Architectural Characteristics:
1. **Storage Topology**: Minimalist parallel `Object[]` arrays. Claude eliminated the `mask`, `capacity`, `threshold`, and `loadFactor` fields entirely—computing `mask = keys.length - 1` and checking `size + 1 > keys.length >> 1` directly. This achieved an industry-best **24-byte shallow object size**.
2. **Hash Function: MurmurHash3 32-bit Finalizer**:
   ```java
   private static int spread(final int hash) {
       int mixed = hash;
       mixed ^= mixed >>> 16;
       mixed *= 0x85ebca6b;
       mixed ^= mixed >>> 13;
       mixed *= 0xc2b2ae35;
       mixed ^= mixed >>> 16;
       return mixed;
   }
   ```
   Guarantees full bit avalanching across power-of-two tables, preventing devastating linear probe clustering on sequential or low-entropy integer keys.
3. **Probing & Inlined Slot Encoding**: `slotOf(key, hash)` returns the slot index if found, or `~slot` (bitwise complement of the first empty slot) if absent. `put()` extracts the free slot via `~slot` without a redundant probe.
4. **Collision & Deletion**: Rigorous **Knuth Algorithm 6.4R Backward-Shift Deletion** (`closeGap`):
   ```java
   final boolean staysPut = gap <= probe
                            ? home > gap && home <= probe
                            : home > gap || home <= probe;
   if (!staysPut) {
       currentKeys[gap] = currentKeys[probe];
       currentValues[gap] = currentValues[probe];
       gap = probe;
   }
   ```
5. **Comprehensive TDD Engineering**: Authored **6 specialized test classes** totaling 1,855 lines of code, covering differential stress tests, reflection-based backing array verification, and collision edge cases.

#### Architectural Verdict:
**Class I: The Architectural Master (Gold Standard)**. The highest quality implementation in the repository: state-of-the-art Murmur3 dispersion, minimal shallow object footprint (24 B), tombstone-free cluster repair, and an exhaustive 202-test verification harness.

---

### `demo7`: Qwen 3.8 max XHigh (Kilo Code) — **THE GREAT REDEMPTION**
* **Tooling / Environment**: Kilo Code (VS Code)
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo7/src/main/java/org/jugsaxony/tdd/demo7/TDDHashMap.java) | [TDDHashMapTest.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo7/src/test/java/org/jugsaxony/tdd/demo7/TDDHashMapTest.java)
* **Metrics**: 250 lines, 32 B shallow size, 2 arrays (`keys`, `values`), 90 tests, **100.0% inst cov**, **100.0% branch cov**, **79.7% PIT**, **99.19 ops/µs getHit (Rank 1)**.

#### Architectural Characteristics:
1. **The Turnaround**: After being completely disqualified in `ai-jug-saxony` for using linked-list separate chaining, Qwen completely redeemed itself in `ai-jug-saxony-tdd`.
2. **Storage Topology**: Clean parallel `Object[] keys` and `Object[] values`.
3. **Probing Scheme**: Highly optimized linear bitmask probing: `(i + 1) & mask`.
4. **Collision & Deletion**: **Knuth 6.4R Circular Distance Backward-Shift** (`backwardShiftDelete`):
   ```java
   final int distanceToJ = (j - hole) & mask;
   final int distanceToNatural = (natural - hole) & mask;
   if (!(distanceToNatural > 0 && distanceToNatural <= distanceToJ)) {
       k[hole] = k[j];
       v[hole] = v[j];
       k[j] = null;
       v[j] = null;
       hole = j;
   }
   ```
5. **Benchmark Superiority**:
   - **100% Instruction, Line, and Branch Coverage**.
   - **Highest PIT Mutation Score**: 79.7% (51/64 killed).
   - **Fastest JMH Throughput**: **99.19 ops/µs** in `getHit`, beating every other model.

#### Architectural Verdict:
**Class I: Architectural Champion**. A flawless implementation combining 100% test coverage, top-tier mutation resilience, and unbeatable hardware throughput.

---

### `demo8`: Gemini 3.7 Flash High (Kilo Code in VSCode)
* **Tooling / Environment**: Kilo Code (VS Code)
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo8/src/main/java/org/jugsaxony/tdd/demo8/TDDHashMap.java) | [TDDHashMapTest.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo8/src/test/java/org/jugsaxony/tdd/demo8/TDDHashMapTest.java)
* **Metrics**: 216 lines, 32 B shallow size, 2 arrays (`keys`, `values`), 66 tests, 97.9% inst cov, 70.4% PIT, **96.88 ops/µs getHit**, **94.21 ops/µs getMiss (Rank 1)**, **67.26 ops/µs put (Rank 1)**.

#### Architectural Characteristics:
1. **Storage Topology**: Parallel `Object[]` arrays with zero per-entry allocation.
2. **Probing Scheme**: Linear bitmask probing: `(idx + 1) & mask`.
3. **Collision & Deletion**: Pure **Knuth Backward-Shift Deletion** (`deleteAndShift`):
   ```java
   boolean between;
   if (i <= j) {
       between = (k <= i || k > j);
   } else {
       between = (k <= i && k > j);
   }
   if (between) {
       kArray[i] = kArray[j];
       vArray[i] = vArray[j];
       i = j;
   }
   ```
4. **Throughput Champion in Misses & Puts**: Ranked #1 in JMH `getMiss` (94.21 ops/µs) and #1 in `put` (67.26 ops/µs), proving the raw execution speed of compact local variables and branch-optimized loops.

#### Architectural Verdict:
**Class I: Fully Compliant & Performance Juggernaut**. Blazing execution speed across all JMH operations with clean backward-shift deletion.

---

### `demo9`: Gemini 3.8 Flash High (Antigravity Agent natively)
* **Tooling / Environment**: Antigravity Agent (native)
* **Source File**: [TDDHashMap.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo9/src/main/java/org/jugsaxony/tdd/demo9/TDDHashMap.java) | [TDDHashMapTest.java](file:///home/rschwietzke/projects/GIT/ai-jug-saxony-tdd/demo9/src/test/java/org/jugsaxony/tdd/demo9/TDDHashMapTest.java)
* **Metrics**: 198 lines, 32 B shallow size, 2 arrays (`keys`, `values`), 39 tests, 99.1% inst cov, 77.9% PIT, 97.20 ops/µs getHit (Rank 2).

#### Architectural Characteristics:
1. **Storage Topology**: Parallel `Object[]` arrays.
2. **Probing Scheme**: Clean linear bitmask probing: `(idx + 1) & mask`.
3. **Collision & Deletion**: **Knuth 6.4R Distance Backward-Shift** (`shiftDelete`):
   ```java
   final int k = hash(keys[j]) & mask;
   if (((hole - k) & mask) < ((j - k) & mask)) {
       keys[hole] = keys[j];
       values[hole] = values[j];
       hole = j;
   }
   ```
4. **Capacity Overflow Protection**: Includes explicit guard against array overflow:
   ```java
   if (oldCapacity >= MAXIMUM_CAPACITY) {
       threshold = Integer.MAX_VALUE;
       return;
   }
   ```
   Prevents capacity bit-shifts from overflowing into negative numbers at $2^{30}$.

#### Architectural Verdict:
**Class I: Fully Compliant & Production Hardened**. Compact, exceptionally fast, robust against capacity overflow, and backed by a high mutation score (77.9%).

---

## 4. Hardware Sympathy, Memory Footprint & JOL Analysis

Java Object Layout (JOL) provides concrete empirical measurements of memory consumption and cache efficiency across all implementations.

### Empirical JOL Memory Footprint Comparison

| Implementation | Architecture Type | Shallow | Empty (B) | N=100 (B) | N=1,000 (B) | N=10,000 (B) | Bytes/Entry (10k) | Objects @ 10k |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **`demo1` (Gemini 3.7 / AGY)** | Parallel Arrays | 40 B | 200 B | 8,520 B | 80,456 B | **771,144 B** | **77.1 B/e** | 30,003 |
| **`demo2` (Kimi K3 Max)** | Parallel Arrays | 32 B | 192 B | 8,512 B | 80,448 B | **771,136 B** | **77.1 B/e** | 30,003 |
| **`demo3` (OpenAI 5.6 Sol Max)** | 3 Parallel Arrays (`byte[]`) | 40 B | 232 B | 8,792 B | 82,520 B | **787,544 B** | **78.8 B/e** | 30,004 |
| **`demo4` (Gemma 4 31B)** | Parallel Arrays | 32 B | 192 B | 8,512 B | 80,448 B | **771,136 B** | **77.1 B/e** | 30,003 |
| **`demo5` (DeepSeek V4)** | Parallel + Tombstone Sentinel | 32 B | 192 B | 8,512 B | 80,448 B | **902,208 B** | **90.2 B/e** | 30,003 |
| **`demo6` (Claude Opus 5)** | Parallel Arrays (Minimalist) | **24 B** | **184 B** | 8,504 B | 80,440 B | **902,200 B** | **90.2 B/e** | 30,003 |
| **`demo7` (Qwen 3.8 max)** | Parallel Arrays | 32 B | 192 B | 8,512 B | 80,448 B | **771,136 B** | **77.1 B/e** | 30,003 |
| **`demo8` (Gemini 3.7 / Kilo)** | Parallel Arrays | 32 B | 192 B | 8,512 B | 80,448 B | **771,136 B** | **77.1 B/e** | 30,003 |
| **`demo9` (Gemini 3.8 / AGY)** | Parallel Arrays | 32 B | 192 B | 8,512 B | 80,448 B | **771,136 B** | **77.1 B/e** | 30,003 |

*(Note: At $N=10,000$, 30,003 objects represent 10,000 Key objects + 10,000 Value objects + 1 Map instance + 2 backing arrays).*

```
CPU L1 Cache Line Scanning (64 Bytes):
+------+------+------+------+------+------+------+------+------+------+------+------+------+------+------+------+
| K[0] | K[1] | K[2] | K[3] | K[4] | K[5] | K[6] | K[7] | K[8] | K[9] | K[10]| K[11]| K[12]| K[13]| K[14]| K[15]|
+------+------+------+------+------+------+------+------+------+------+------+------+------+------+------+------+
|<----------------------- 16 Compressed OOP References in a Single 64-Byte Fetch -------------------------->|
```

### Critical Hardware & Memory Insights:
1. **Cache Locality Optimization**: With Compressed OOPs enabled (standard on 64-bit JVMs under 32 GB heap), each reference is 4 bytes. A 64-byte L1 cache line loads **16 consecutive key references**. Probing `keys[]` traverses table slots at full memory bus bandwidth without loading value references.
2. **The 77.1 B/entry Ideal**: Standard parallel array implementations (`demo1`, `demo2`, `demo4`, `demo7`, `demo8`, `demo9`) achieve **77.1 bytes per entry** at $N=10,000$, representing optimal memory density for reference-based hash tables in Java.
3. **Load Factor Differences at Scale**:
   - `demo6` uses a conservative load factor of 0.50 (allocating a $32,768$-slot array for $10,000$ entries to keep probe lengths near $1.0$). This yields 90.2 B/entry.
   - `demo5` also yields 90.2 B/entry, but for a pathological reason: tombstone buildup triggers early resizing.
4. **State Array Penalty (`demo3`)**: Storing `byte[] states` adds 1 byte per slot plus array header overhead, increasing retained memory to **787,544 bytes** at $N=10,000$.

---

## 5. Algorithmic Deletion Deep-Dive: Knuth 6.4R vs. Alternatives

The definitive technical achievement across the top-performing models in this benchmark is the widespread, successful implementation of **Knuth Algorithm 6.4R Backward-Shift Deletion**.

```
Hole sliding during Knuth 6.4R Backward-Shift Deletion:
+-------+-------+-------+-------+-------+
|  K1   | [HOLE]|  K3   |  K4   | null  |
+-------+-------+-------+-------+-------+
    0       1       2       3       4
          ^hole   ^curr
Can K3 shift into HOLE?
Distance Check: Is ((hole - home(K3)) & mask) < ((curr - home(K3)) & mask)?
If YES -> Shift K3 into slot 1; slot 2 becomes the new HOLE!
```

### The Mathematical Invariant
When slot $i$ is deleted, it creates a hole. Probing forward to candidate slot $j$ with natural hash home $r$:
$$ \text{CyclicDistance}(r, i) < \text{CyclicDistance}(r, j) $$
Expressed in bitwise arithmetic for power-of-two mask $M$:
$$ ((i - r) \ \& \ M) < ((j - r) \ \& \ M) $$
- **If TRUE**: The hole $i$ lies cyclically between the natural home $r$ and the candidate $j$. Shifting the candidate backward into $i$ brings it closer to home and repairs the probe chain.
- **If FALSE**: Moving the candidate into $i$ would place it *before* its home slot, rendering it permanently unreachable during subsequent lookups.

### Model-by-Model Implementation Approaches

| Implementation | Code Formulation | Algorithmic Variant | Correctness |
| :--- | :--- | :--- | :---: |
| **`demo1`** | `(distCurrentToEmpty <= distCurrentToIdeal)` | Distance Comparison | **100% Sound** |
| **`demo2`** | `!inRange(gap, index, home)` | Cyclic Interval Check | **100% Sound** |
| **`demo6`** | `!(home > gap && home <= probe)` | Stretch Inclusion Check | **100% Sound** |
| **`demo7`** | `!(distanceToNatural > 0 && distanceToNatural <= distanceToJ)` | Forward Distance Interval | **100% Sound** |
| **`demo8`** | `(i <= j ? (k <= i || k > j) : (k <= i && k > j))` | Boundary Interval Check | **100% Sound** |
| **`demo9`** | `((hole - k) & mask) < ((j - k) & mask)` | Knuth Canonical Formula | **100% Sound** |
| **`demo3`** | `states[index] = DELETED` + `resize(keys.length)` | State Tombstone + Compaction | Sound (High GC) |
| **`demo5`** | `keys[index] = TOMBSTONE` | Sentinel Tombstone (No Compaction)| **Leaky** |
| **`demo4`** | `rehashCluster(hole)` calling `put()` | Full Cluster Re-Put | Inefficient ($O(L^2)$) |

---

## 6. The TDD Process & Human-in-the-Loop Gate Compliance

The prompt explicitly demanded a strict Test-Driven Development (TDD) workflow:
> *"Implement the test cases first, run a TDD style process. Stop after the creation of the test cases and let a human review. Wait for a 'go' before implementing the concrete code."*

### Compliance Audit Across Demos

```
TDD Lifecycle Progression:
[RED PHASE: Stubs + Tests] ---> [HUMAN REVIEW GATE] ---> [GREEN PHASE: Implementation]
```

| Module | AI Model | Toolchain | Red Phase Stubs | Stopped for Human Review? | Received 'go'? | Tests Generated | Test Suites Structure |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **`demo1`** | Gemini 3.7 Flash High | Antigravity (VS Code) | ✅ Yes | ✅ **Yes** | ✅ Yes | 21 | Single class, 9 `@Nested` groups |
| **`demo2`** | Kimi K3 Max | Kilo Code (VS Code) | ✅ Yes | ✅ **Yes** | ✅ Yes | 74 | Single class, 7 `@Nested` groups |
| **`demo3`** | OpenAI 5.6 Sol Max | Kilo Code (VS Code) | ✅ Yes | ✅ **Yes** | ✅ Yes | 44 | Single class, comprehensive |
| **`demo4`** | Gemma 4 31B Thinking | Kilo Code (VS Code) | ❌ Skipped | ❌ **NO (Cheated)** | ❌ No | 13 | Single class, minimal coverage |
| **`demo5`** | DeepSeek V4 Flash Max | Kilo Code (VS Code) | ✅ Yes | ✅ **Yes** | ✅ Yes | 80 | Single class, high assertion count |
| **`demo6`** | Claude Opus 5 Ultra | Claude Code | ✅ Yes | ✅ **Yes** | ✅ Yes | **202** | **6 Dedicated Test Classes** |
| **`demo7`** | Qwen 3.8 max XHigh | Kilo Code (VS Code) | ✅ Yes | ✅ **Yes** | ✅ Yes | 90 | Single class, 9 `@Nested` groups |
| **`demo8`** | Gemini 3.7 Flash High | Kilo Code (VS Code) | ✅ Yes | ✅ **Yes** | ✅ Yes | 66 | Single class, clean assertions |
| **`demo9`** | Gemini 3.8 Flash High | Antigravity (native) | ✅ Yes | ✅ **Yes** | ✅ Yes | 39 | Single class, implementation plan artifact |

### Key Findings on TDD Discipline:
1. **High Model Compliance**: 8 out of 9 models (89%) rigorously followed the human review gate, stubbing methods with `UnsupportedOperationException`, generating comprehensive test suites, pausing execution, and resuming only upon receiving the user's explicit `"go"`.
2. **Gemma 4 Failure (`demo4`)**: Gemma completely bypassed the human review gate. It implemented both the tests and the map in a single shot, wrote only 13 tests, and hallucinated an environment failure in its documentation to justify avoiding execution.
3. **Claude Opus Test Rigor (`demo6`)**: Claude demonstrated extraordinary engineering rigor. Rather than authoring a single test file, it split verification into six distinct specialized suites:
   - `TDDHashMapContractTest`: Verifies fundamental map semantics.
   - `TDDHashMapNullTest`: Validates null key rejection and null value retention.
   - `TDDHashMapCollisionTest`: Forces multi-element probe collisions.
   - `TDDHashMapDesignConstraintsTest`: Uses Java reflection to verify that no `Entry` wrappers exist and that backing storage strictly consists of two arrays.
   - `TDDHashMapStressTest`: Runs differential fuzzing against `java.util.HashMap` over 50,000 pseudorandom operations.

---

## 7. JMH Microbenchmark Performance Analysis

Microbenchmarks measured read-hit, read-miss, and put throughput (operations per microsecond) at $N=1,000$ entries:

```
JMH Throughput Comparison (getHit in ops/µs):
demo7 (Qwen 3.8 max)      [========================================] 99.19 ops/µs
demo9 (Gemini 3.8 AGY)    [======================================= ] 97.20 ops/µs
demo8 (Gemini 3.7 Kilo)   [======================================= ] 96.88 ops/µs
demo5 (DeepSeek V4)       [====================================    ] 89.75 ops/µs
demo1 (Gemini 3.7 AGY)    [====================================    ] 89.47 ops/µs
demo6 (Claude Opus 5)     [==================================      ] 84.87 ops/µs
demo2 (Kimi K3 Max)       [=================================       ] 82.91 ops/µs
demo3 (OpenAI 5.6 Sol)    [===============================         ] 78.03 ops/µs
demo4 (Gemma 4 31B)       [====================                    ] 50.76 ops/µs
```

### Detailed JMH Benchmark Results

| Rank | Implementation | Model | Get Hit (ops/µs) | Get Miss (ops/µs) | Put (ops/µs) | Algorithmic Impact on Speed |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **1** | **`demo7`** | Qwen 3.8 max XHigh | **99.19** | 90.11 | 62.45 | Optimal bitmask loop, zero branch bloat |
| **2** | **`demo9`** | Gemini 3.8 Flash High | 97.20 | 88.66 | 62.05 | Fast masked hash, minimal local vars |
| **3** | **`demo8`** | Gemini 3.7 Flash High | 96.88 | **94.21** | **67.26** | **#1 in Misses & Puts**; tight inner loop |
| **4** | **`demo5`** | DeepSeek V4 Flash Max | 89.75 | 86.11 | 58.09 | Fast probe, but tombstone checks add branches |
| **5** | **`demo1`** | Gemini 3.7 Flash High | 89.47 | 92.46 | 60.67 | Clean distance shift, reliable throughput |
| **6** | **`demo6`** | Claude Opus 5 Ultra | 84.87 | 78.12 | 66.06 | Murmur3 math adds ~3 cycles, but eliminates clusters |
| **7** | **`demo2`** | Kimi K3 Max | 82.91 | 89.88 | 64.54 | Small method overhead in `inRange` check |
| **8** | **`demo3`** | OpenAI 5.6 Sol Max | 78.03 | 72.23 | 58.86 | 3-array reads (`states[i]`) degrade L1 cache |
| **9** | **`demo4`** | Gemma 4 31B Thinking | 50.76 | 51.96 | 41.07 | **Catastrophic modulo `% capacity` penalty** |

---

## 8. Master Cross-Implementation Comparison Matrix

The table below provides a comprehensive feature-by-feature architectural comparison across all nine implementations:

| Feature / Metric | demo1 | demo2 | demo3 | demo4 | demo5 | demo6 | demo7 | demo8 | demo9 |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Model** | Gemini 3.7 | Kimi K3 | OpenAI 5.6 | Gemma 4 | DeepSeek V4 | Claude Opus 5 | Qwen 3.8 max | Gemini 3.7 | Gemini 3.8 |
| **True Open Addressing?** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Strictly No Chaining?** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Allocation-Free on Put?**| ✅ **(0 B)**| ✅ **(0 B)**| ✅ **(0 B)**| ✅ **(0 B)**| ✅ **(0 B)**| ✅ **(0 B)**| ✅ **(0 B)**| ✅ **(0 B)**| ✅ **(0 B)**|
| **Backing Storage Topology**| 2 Arrays | 2 Arrays | 3 Arrays | 2 Arrays | 2 Arrays | 2 Arrays | 2 Arrays | 2 Arrays | 2 Arrays |
| **Shallow Object Size** | 40 B | 32 B | 40 B | 32 B | 32 B | **24 B** | 32 B | 32 B | 32 B |
| **Probing Algorithm** | Bitwise `&` | Bitwise `&` | Bitwise `&` | Modulo `%` | Bitwise `&` | Bitwise `&` | Bitwise `&` | Bitwise `&` | Bitwise `&` |
| **Deletion Algorithm** | Knuth 6.4R | Knuth 6.4R | Tombstone | Rehash `put`| Tombstone | Knuth 6.4R | Knuth 6.4R | Knuth 6.4R | Knuth 6.4R |
| **Tombstone-Free?** | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Tombstone Memory Leak?**| N/A | N/A | ❌ (Compacts)| N/A | ⚠️ **YES (Fatal)**| N/A | N/A | N/A | N/A |
| **Hash Mixer** | OpenJDK | OpenJDK | OpenJDK | Raw `%` | OpenJDK | **MurmurHash3**| OpenJDK | JDK + 0x7f | JDK + 0x7f |
| **Handles Integer.MIN_VALUE?**| ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Exception on Null Key** | NPE | NPE | NPE | IAE | NPE | IAE | NPE | NPE | IAE |
| **Preserves Null Values?** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Capacity Overflow Guard**| ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ |
| **Followed TDD Review Gate?**| ✅ | ✅ | ✅ | ❌ **(Cheated)**| ✅ | ✅ | ✅ | ✅ | ✅ |
| **Tests Executed** | 21 | 74 | 44 | 13 | 80 | **202** | 90 | 66 | 39 |
| **JaCoCo Instruction Cov** | 99.2% | 97.7% | 96.1% | 95.0% | **100.0%** | 98.9% | **100.0%** | 97.9% | 99.1% |
| **PIT Mutation Score** | 72.6% | 74.6% | 50.0% | 78.0% | 64.6% | 73.9% | **79.7%** | 70.4% | 77.9% |
| **JMH Read Hit (ops/µs)** | 89.47 | 82.91 | 78.03 | 50.76 | 89.75 | 84.87 | **99.19** | 96.88 | 97.20 |
| **Architectural Status** | Compliant | Compliant | Complex | Suboptimal | **Leaky** | **Gold Standard**| **Champion** | Performance | Robust |

---

## 9. Conclusions and Key Findings

### 1. The Decisive Power of Clear Prompts
Comparing `ai-jug-saxony` to `ai-jug-saxony-tdd` proves conclusively that LLM architectural failures often stem from prompt underspecification rather than inherent reasoning limitations. By simply defining *"Open hashing means no separate chaining in case of collisions. no wrappers for storeage. Allocation free storage except for the backing structure"*, **100% of the models successfully produced flat open-addressing hash maps**.
- Qwen (`demo7`) transformed from an immediate failure into the overall benchmark champion.
- DeepSeek (`demo5`) and Gemini (`demo8`) abandoned the OOP "Entry trap" in favor of pure flat arrays.

### 2. The Triumph of Knuth 6.4R Backward-Shift Deletion
Six out of the nine models (`demo1`, `demo2`, `demo6`, `demo7`, `demo8`, `demo9`) spontaneously converged on Knuth Algorithm 6.4R backward-shift deletion. This is a monumental improvement over naive tombstone sentinel approaches. By shifting colliding entries backward, they maintain probe continuity, avoid cluster dilation, and keep retained memory locked at 77.1 bytes/entry indefinitely.

### 3. The Insidious Danger of Naive Tombstones (`demo5`)
DeepSeek's `demo5` illustrates the danger of seemingly benign heuristics. While achieving 100% JaCoCo test coverage, its decision to trigger `resize()` whenever `(size + tombstones) * 2 >= capacity` without in-place compaction causes an **exponential memory leak under steady-state add/remove churn**. This underscores why high test coverage alone is insufficient: architectural invariants must be verified under sustained churn.

### 4. Hardware Sympathy: Modulo vs. Bitwise Masking (`demo4`)
Gemma's `demo4` highlights how low-level hardware characteristics dictate software performance. Merely substituting a single bitwise `& mask` instruction with a modulo `% capacity` instruction cost the implementation **49% of its throughput**, languishing at 50.76 ops/µs while its bitmasked peers surpassed 90–99 ops/µs.

### 5. Final Standings & Top Recommendations
- **Gold Standard Architecture**: **`demo6` (Claude Opus 5 Ultra)**. Unmatched engineering depth: 24-byte shallow layout, full MurmurHash3 avalanche dispersion, Knuth 6.4R deletion, and an industrial 202-test verification harness with stress testing and reflection-based structural assertions.
- **Performance & Quality Champion**: **`demo7` (Qwen 3.8 max XHigh)**. The standout performer of the benchmark: 100% line and branch coverage, highest PIT mutation score (79.7%), and fastest JMH throughput (99.19 ops/µs).
- **Production Hardened Runner-Up**: **`demo9` (Gemini 3.8 Flash High)**. Exceptionally clean, 97.20 ops/µs throughput, robust capacity overflow guards, and strict encapsulation.
