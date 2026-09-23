# AI JUG Saxony TDD — Global Evaluation Dashboard

Comprehensive evaluation of AI coding models implementing an open hashing map (`TDDHashMap`) following a test-driven development workflow.

## 1. Test Verification & Code Quality

| Module | AI Model | Toolchain | Tests Run | Errors/Fails | Duration | JaCoCo Inst Cov | Line Cov | Branch Cov | PIT Mutation Score |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **demo1** | Gemini 3.7 Flash High | Antigravity Agent (VS Code) | 21 | 0 | 1.560s | 99.2% | 100.0% | 93.2% | 74.5% (70/94) |
| **demo2** | Kimi K3 Max | Kilo Code (VS Code) | 37 | 0 | 0.668s | 97.7% | 98.9% | 85.7% | 74.7% (62/83) |
| **demo3** | OpenAI 5.6 Sol Max | Kilo Code (VS Code) | 22 | 0 | 0.713s | 96.1% | 96.1% | 93.8% | 50.0% (50/100) |
| **demo4** | Gemma 4 31B Thinking | Kilo Code (VS Code) | 13 | 0 | 0.176s | 95.0% | 96.3% | 89.5% | 75.4% (52/69) |
| **demo5** | DeepSeek V4 Flash Max | Kilo Code (VS Code) | 40 | 0 | 0.792s | 100.0% | 100.0% | 97.8% | 64.4% (56/87) |
| **demo6** | Claude Opus 5 Ultra | Claude | 101 | 0 | 6.018s | 98.9% | 99.1% | 94.0% | 74.3% (84/113) |
| **demo7** | Qwen 3.8 max XHigh | Kilo Code (VS Code) | 45 | 0 | 0.914s | 100.0% | 100.0% | 100.0% | 79.5% (66/83) |
| **demo8** | Gemini 3.7 Flash High | Kilo Code (VS Code) | 33 | 0 | 1.706s | 97.9% | 99.1% | 87.5% | 69.1% (65/94) |
| **demo9** | Gemini 3.8 Flash High | Antigravity Agent (native) | 39 | 0 | 2.292s | 99.1% | 97.9% | 97.5% | 80.5% (70/87) |

## 2. Memory Footprint (JOL)

| Module | AI Model | Shallow (B) | Empty (B) | N=1,000 (B) | Bytes/Entry | Objects @ 10k |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **demo1** | Gemini 3.7 Flash High | 40 B | 200 B | 80,456 B | 80.5 B | 30,003 |
| **demo2** | Kimi K3 Max | 32 B | 192 B | 80,448 B | 80.4 B | 30,003 |
| **demo3** | OpenAI 5.6 Sol Max | 40 B | 232 B | 82,520 B | 82.5 B | 30,004 |
| **demo4** | Gemma 4 31B Thinking | 32 B | 192 B | 80,448 B | 80.4 B | 30,003 |
| **demo5** | DeepSeek V4 Flash Max | 32 B | 192 B | 80,448 B | 80.4 B | 30,003 |
| **demo6** | Claude Opus 5 Ultra | 24 B | 184 B | 80,440 B | 80.4 B | 30,003 |
| **demo7** | Qwen 3.8 max XHigh | 32 B | 192 B | 80,448 B | 80.4 B | 30,003 |
| **demo8** | Gemini 3.7 Flash High | 32 B | 192 B | 80,448 B | 80.4 B | 30,003 |
| **demo9** | Gemini 3.8 Flash High | 32 B | 192 B | 80,448 B | 80.4 B | 30,003 |

## 3. Microbenchmark Throughput & Hardware Performance Counters (JMH)

> ⚡ **Hardware Performance Counters Enabled**: Includes Linux `perf` metrics (Cycles/op, Instructions/op, IPC, Branch Mispredictions, L1 D-Cache Misses).

| Module | AI Model | Put (ops/µs) | Get Hit (ops/µs) | Get Miss (ops/µs) | Cycles/op | Insns/op | IPC | Branch Miss % | L1 Miss % |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **demo1** | Gemini 3.7 Flash High | 57.09 | 68.04 | 43.38 | 34.5 | 101.0 | 2.93 | 0.08% | 8.80% |
| **demo2** | Kimi K3 Max | 63.60 | 78.27 | 77.22 | 34.6 | 102.3 | 2.95 | 0.08% | 8.38% |
| **demo3** | OpenAI 5.6 Sol Max | 35.81 | 68.00 | 55.54 | 40.4 | 132.1 | 3.27 | 0.12% | 9.26% |
| **demo4** | Gemma 4 31B Thinking | 35.57 | 35.95 | 36.23 | 60.7 | 102.6 | 1.69 | 0.14% | 10.18% |
| **demo5** | DeepSeek V4 Flash Max | 46.42 | 67.91 | 54.31 | 35.6 | 110.2 | 3.10 | 0.12% | 6.32% |
| **demo6** | Claude Opus 5 Ultra | 45.39 | 53.42 | 72.38 | 38.4 | 100.8 | 2.62 | 0.13% | 10.99% |
| **demo7** | Qwen 3.8 max XHigh | 56.28 | 65.24 | 63.47 | 37.7 | 106.7 | 2.83 | 0.09% | 7.11% |
| **demo8** | Gemini 3.7 Flash High | 44.38 | 62.74 | 71.23 | 38.6 | 105.3 | 2.73 | 0.09% | 6.02% |
| **demo9** | Gemini 3.8 Flash High | 32.68 | 66.82 | 60.37 | 35.7 | 101.7 | 2.85 | 0.09% | 7.73% |
