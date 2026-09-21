# AI JUG Saxony TDD — AI Model Performance & Quality Evaluation

A comparative benchmark evaluating AI coding assistants and LLMs implementing an open-addressing hash map (`TDDHashMap`) using a strict **Test-Driven Development (TDD)** methodology.

Each AI model was tasked with implementing an open hashing map (closed addressing / separate chaining prohibited; flat allocation-free storage except backing arrays; unbounded capacity; null keys disallowed, null values allowed) and its corresponding unit test suite.

---

## Submodule & Model Overview

| Module | AI Model / LLM | Toolchain / Environment | Implementation Architecture |
| :--- | :--- | :--- | :--- |
| **`demo1`** | Gemini 3.7 Flash High | Antigravity Agent (VS Code) | Flat parallel `Object[]` keys/values with linear probing |
| **`demo2`** | Kimi K3 Max | Kilo Code (VS Code) | Flat parallel `Object[]` keys/values with linear probing & tombstones |
| **`demo3`** | OpenAI 5.6 Sol Max | Kilo Code (VS Code) | Flat parallel `Object[]` keys/values with linear probing & tombstones |
| **`demo4`** | Gemma 4 31B Thinking | Kilo Code (VS Code) | Flat parallel `Object[]` keys/values with linear probing & tombstones |
| **`demo5`** | DeepSeek V4 Flash Max | Kilo Code (VS Code) | Flat parallel `Object[]` keys/values with linear probing & shift-back |
| **`demo6`** | Claude Opus 5 Ultra | Claude | Flat parallel `Object[]` keys/values with linear probing & shift-back |
| **`demo7`** | Qwen 3.8 max XHigh | Kilo Code (VS Code) | Flat parallel `Object[]` keys/values with linear probing & shift-back |
| **`demo8`** | Gemini 3.7 Flash High | Kilo.Code Agent (VS Code) | Flat parallel `Object[]` keys/values with linear probing & tombstones |
| **`demo9`** | Gemini 3.8 Flash High | Antigravity Agent (natively) | Flat parallel `Object[]` keys/values with linear probing & shift-back |

---

## Running the Test Suite & Generating Reports

The project is structured as a Maven multi-module build with an automated reporting engine in `coverage-report`. All generated HTML and Markdown reports are consolidated and persisted under `reports/`.

### Step 1: Run Unit Tests & Collect JaCoCo Coverage
Executes all unit tests across all reactor submodules (`demo1` through `demo9`), verifies all test suites, and collects per-module JaCoCo execution data:
```bash
mvn clean test
```

### Step 2: Generate Aggregated JaCoCo Code Coverage Report
Aggregates coverage metrics across all submodules into a unified multi-module HTML report:
```bash
mvn verify -pl coverage-report -am
```
- **Aggregated Report**: `reports/coverage-aggregate/index.html` (also at `coverage-report/target/site/jacoco-aggregate/index.html`)

### Step 3: Run PIT Mutation Coverage
Executes PIT mutation testing across submodules to generate `mutations.csv` and detailed mutation reports:
```bash
mvn test-compile pitest:mutationCoverage
```
- **Module Output**: `<module>/target/pit-reports/` (automatically copied to `reports/pit-reports/<module>/` upon dashboard generation)

### Step 4: Run JMH Cross-Project Microbenchmarks
Executes microbenchmarks comparing read-hit, read-miss, and put throughput across all implementations.

- **Quick Run (~30s)**:
  ```bash
  mvn test-compile exec:java \
    -Dexec.mainClass="org.jugsaxony.tdd.report.GlobalJmhBenchmark" \
    -Dexec.classpathScope="test" \
    -pl coverage-report \
    -Dexec.args="--quick"
  ```

- **Quick Run with Hardware Performance Counters (`perf`)**:
  ```bash
  mvn test-compile exec:java \
    -Dexec.mainClass="org.jugsaxony.tdd.report.GlobalJmhBenchmark" \
    -Dexec.classpathScope="test" \
    -pl coverage-report \
    -Dexec.args="--quick --perf"
  ```

- **Full Measurement Run with Perf & GC Profiling**:
  ```bash
  mvn test-compile exec:java \
    -Dexec.mainClass="org.jugsaxony.tdd.report.GlobalJmhBenchmark" \
    -Dexec.classpathScope="test" \
    -pl coverage-report \
    -Dexec.args="--perf --gc"
  ```

- **Filtering Specific Demos / Operations**:
  ```bash
  mvn test-compile exec:java \
    -Dexec.mainClass="org.jugsaxony.tdd.report.GlobalJmhBenchmark" \
    -Dexec.classpathScope="test" \
    -pl coverage-report \
    -Dexec.args="--quick --filter getHit_demo[12]"
  ```

- **Running an Individual Submodule Benchmark Directly**:
  ```bash
  mvn test-compile exec:java \
    -pl demo1 \
    -Dexec.mainClass="org.jugsaxony.tdd.demo1.TDDHashMapBenchmark" \
    -Dexec.classpathScope="test" \
    -Dexec.args="-f 1 -wi 2 -i 3"
  ```

### Step 5: Generate JOL Memory Footprint Analysis & Master Dashboard
Runs Java Object Layout (JOL) memory footprint analysis across all implementations, gathers Surefire test results, JaCoCo coverage, PIT mutation scores, and JMH microbenchmark results, then generates all HTML dashboards into `reports/`:
```bash
mvn test -pl coverage-report -am
```
Alternatively, invoke the dashboard generator directly:
```bash
mvn test-compile exec:java \
  -Dexec.mainClass="org.jugsaxony.tdd.report.GlobalDashboardGenerator" \
  -Dexec.classpathScope="test" \
  -pl coverage-report
```

---

## End-to-End One-Liner

To run the entire evaluation pipeline — unit tests, aggregated coverage, mutation testing, microbenchmarks, and dashboard generation — in a single command:

```bash
mvn clean test && \
mvn verify -pl coverage-report -am && \
mvn test-compile pitest:mutationCoverage && \
mvn test-compile exec:java -Dexec.mainClass="org.jugsaxony.tdd.report.GlobalJmhBenchmark" -Dexec.classpathScope="test" -pl coverage-report -Dexec.args="--quick" && \
mvn test -pl coverage-report -am
```

---

## Generated Reports Directory (`reports/`)

All consolidated and per-module reports are located in `reports/` (preserved across `mvn clean`):

| Report | Path | Description |
| :--- | :--- | :--- |
| **Master Executive Dashboard** | `reports/index.html` | Interactive dashboard with high-level KPI cards, test counts, coverage, mutation scores, memory, and benchmark throughput |
| **TDDHashMap Detailed Matrix** | `reports/tddhashmap.html` | In-depth breakdown of open hashing map implementation metrics, test counts, and mutation survival rates |
| **JOL Memory Footprint Report** | `reports/jol-report.html` | Detailed object layout, shallow size, empty table size, deep retained size @ 1,000 entries, and bytes/entry |
| **JMH Microbenchmark Report** | `reports/jmh-report.html` | Comparative read-hit, read-miss, and put throughput tables and bar charts |
| **Aggregated JaCoCo Report** | `reports/coverage-aggregate/index.html` | Drill-down multi-module code coverage across all demo packages |
| **Global Markdown Dashboard** | `reports/global-dashboard.md` | Executive summary tables formatted for Markdown documentation and GitHub |
| **JMH Raw JSON & CSV** | `reports/jmh-results.json`, `reports/jmh-report.csv` | Machine-readable benchmark outputs |
| **Per-Module JaCoCo Reports** | `reports/jacoco/<module>/index.html` | Granular line and branch coverage per submodule |
| **Per-Module PIT Reports** | `reports/pit-reports/<module>/index.html` | Granular killed/survived mutation details per submodule |
| **Surefire XML Results** | `reports/surefire-reports/<module>/` | Test duration and pass/fail XML reports per submodule |
