# AI JUG Saxony TDD — Global Evaluation Dashboard

Comprehensive evaluation of AI coding models implementing an open hashing map (`TDDHashMap`) following a test-driven development workflow.

## 1. Test Verification & Code Quality

| Module | AI Model | Toolchain | Tests Run | Errors/Fails | Duration | JaCoCo Inst Cov | Line Cov | Branch Cov | PIT Mutation Score |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **demo1** | Gemini 3.7 Flash High | Antigravity Agent (VS Code) | 21 | 0 | 1.627s | 99.2% | 100.0% | 93.2% | 72.6% (53/73) |
| **demo2** | Kimi K3 Max | Kilo Code (VS Code) | 74 | 0 | 1.630s | 97.7% | 98.9% | 85.7% | 74.6% (47/63) |
| **demo3** | OpenAI 5.6 Sol Max | Kilo Code (VS Code) | 44 | 0 | 1.291s | 96.1% | 96.1% | 93.8% | 50.0% (38/76) |
| **demo4** | Gemma 4 31B Thinking | Kilo Code (VS Code) | 13 | 0 | 0.245s | 95.0% | 96.3% | 89.5% | 78.0% (39/50) |
| **demo5** | DeepSeek V4 Flash Max | Kilo Code (VS Code) | 80 | 0 | 1.697s | 100.0% | 100.0% | 97.8% | 64.6% (42/65) |
| **demo6** | Claude Opus 5 Ultra | Claude | 202 | 0 | 8.239s | 98.9% | 99.1% | 94.0% | 73.9% (65/88) |
| **demo7** | Qwen 3.8 max XHigh | Kilo Code (VS Code) | 90 | 0 | 2.502s | 100.0% | 100.0% | 100.0% | 79.7% (51/64) |
| **demo8** | Gemini 3.7 Flash High | Kilo Code (VS Code) | 66 | 0 | 2.142s | 97.9% | 99.1% | 87.5% | 70.4% (50/71) |
| **demo9** | Gemini 3.8 Flash High | Antigravity Agent (native) | 39 | 0 | 1.946s | 99.1% | 97.9% | 97.5% | 77.9% (53/68) |

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

## 3. Microbenchmark Throughput (JMH)

| Module | AI Model | Get Hit (ops/µs) | Get Miss (ops/µs) | Put (ops/µs) | Cycles/op | IPC |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **demo1** | Gemini 3.7 Flash High | 58.31 | 50.35 | 48.25 | - | - |
| **demo2** | Kimi K3 Max | 58.14 | 51.44 | 48.61 | - | - |
| **demo3** | OpenAI 5.6 Sol Max | 40.98 | 58.83 | 43.60 | - | - |
| **demo4** | Gemma 4 31B Thinking | 40.03 | 35.42 | 25.35 | - | - |
| **demo5** | DeepSeek V4 Flash Max | 49.40 | 66.49 | 24.55 | - | - |
| **demo6** | Claude Opus 5 Ultra | 39.33 | 59.50 | 32.84 | - | - |
| **demo7** | Qwen 3.8 max XHigh | 52.67 | 60.16 | 34.81 | - | - |
| **demo8** | Gemini 3.7 Flash High | 52.32 | 74.25 | 32.57 | - | - |
| **demo9** | Gemini 3.8 Flash High | 63.33 | 67.32 | 34.12 | - | - |
