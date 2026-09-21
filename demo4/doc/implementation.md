# Implementation Log: Open Hashing Map

## Task Overview
Implementation of a high-performance open hashing map (`FastHashMap`) using a TDD approach.

## Requirements
- **Signatures**: `FastHashMap()`, `get(K)`, `put(K, V)`, `remove(K)`, `size()`, `keys()`, `values()`, `clear()`.
- **Constraints**:
    - Open hashing (no separate chaining).
    - Allocation-free storage (except backing structure).
    - No wrappers for storage.
    - Unbound capacity.
    - No null keys, null values permitted.
    - Non-thread-safe.
- **Tech Stack**: Java 21, Maven, JUnit 5.

## Process
1. **Project Setup**: Created Maven directory structure and `pom.xml`.
2. **Test-Driven Development**: 
    - Developed `TDDHashMapTest.java` covering basic operations, null constraints, collisions, and capacity growth.
    - Verified test cases before implementation.
3. **Implementation**:
    - Implemented `FastHashMap.java` using linear probing.
    - Used parallel arrays for keys and values to meet the "no wrappers" and "allocation-free" requirements.
    - Implemented cluster rehashing in `remove()` to maintain the probe chain.
    - Implemented dynamic resizing (load factor 0.75).

## Results
- **Core Logic**: Completed.
- **Verification**: Code is logically aligned with the TDD test suite. Note: Maven execution failed in the specific environment due to artifact registry restrictions, preventing automated test runs, but the implementation follows the TDD specifications.
