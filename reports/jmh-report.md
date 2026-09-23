# JMH Microbenchmark Cross-Project Comparison Report

Microbenchmark results comparing all TDD Open Hashing Map implementations (Size = 1,000 items).

> ⚡ **Hardware Performance Counters Enabled**: Includes Linux `perf` metrics (Cycles/op, Instructions/op, IPC, Branch Mispredictions, L1 D-Cache Misses).

## Operation: `getHit`

| Rank | Implementation | Model | Throughput (ops/µs) | Margin (±) | Cycles/op | Insns/op | IPC | Branch Miss % | L1 D-Cache Miss % |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | **demo2** | Demo 2 (Kimi K3 Max / Kilo Code) | 78.27 | ± 0.00 | 34.6 | 102.3 | 2.95 | 0.08% | 8.38% |
| 2 | **demo1** | Demo 1 (Gemini 3.7 Flash High / Antigravity Agent in VSCode) | 68.04 | ± 0.00 | 34.5 | 101.0 | 2.93 | 0.08% | 8.80% |
| 3 | **demo3** | Demo 3 (OpenAI 5.6 Sol Max / Kilo Code) | 68.00 | ± 0.00 | 40.4 | 132.1 | 3.27 | 0.12% | 9.26% |
| 4 | **demo5** | Demo 5 (DeepSeek V4 Flash Max / Kilo Code) | 67.91 | ± 0.00 | 35.6 | 110.2 | 3.10 | 0.12% | 6.32% |
| 5 | **demo9** | Demo 9 (Gemini 3.8 Flash High / Antigravity Agent natively) | 66.82 | ± 0.00 | 35.7 | 101.7 | 2.85 | 0.09% | 7.73% |
| 6 | **demo7** | Demo 7 (Qwen 3.8 max XHigh / Kilo Code) | 65.24 | ± 0.00 | 37.7 | 106.7 | 2.83 | 0.09% | 7.11% |
| 7 | **demo8** | Demo 8 (Gemini 3.7 Flash High / Kilo Code in VSCode) | 62.74 | ± 0.00 | 38.6 | 105.3 | 2.73 | 0.09% | 6.02% |
| 8 | **demo6** | Demo 6 (Claude Opus 5 Ultra / Claude) | 53.42 | ± 0.00 | 38.4 | 100.8 | 2.62 | 0.13% | 10.99% |
| 9 | **demo4** | Demo 4 (Gemma 4 31B Thinking / Kilo Code) | 35.95 | ± 0.00 | 60.7 | 102.6 | 1.69 | 0.14% | 10.18% |

## Operation: `getMiss`

| Rank | Implementation | Model | Throughput (ops/µs) | Margin (±) | Cycles/op | Insns/op | IPC | Branch Miss % | L1 D-Cache Miss % |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | **demo2** | Demo 2 (Kimi K3 Max / Kilo Code) | 77.22 | ± 0.00 | 37.0 | 101.6 | 2.74 | 0.02% | 12.74% |
| 2 | **demo6** | Demo 6 (Claude Opus 5 Ultra / Claude) | 72.38 | ± 0.00 | 40.1 | 113.9 | 2.84 | 0.03% | 12.58% |
| 3 | **demo8** | Demo 8 (Gemini 3.7 Flash High / Kilo Code in VSCode) | 71.23 | ± 0.00 | 37.4 | 102.8 | 2.75 | 0.04% | 9.79% |
| 4 | **demo7** | Demo 7 (Qwen 3.8 max XHigh / Kilo Code) | 63.47 | ± 0.00 | 41.7 | 117.0 | 2.81 | 0.03% | 9.24% |
| 5 | **demo9** | Demo 9 (Gemini 3.8 Flash High / Antigravity Agent natively) | 60.37 | ± 0.00 | 38.2 | 102.3 | 2.68 | 0.04% | 10.77% |
| 6 | **demo3** | Demo 3 (OpenAI 5.6 Sol Max / Kilo Code) | 55.54 | ± 0.00 | 44.9 | 128.7 | 2.87 | 0.05% | 10.17% |
| 7 | **demo5** | Demo 5 (DeepSeek V4 Flash Max / Kilo Code) | 54.31 | ± 0.00 | 44.2 | 117.7 | 2.66 | 0.04% | 9.86% |
| 8 | **demo1** | Demo 1 (Gemini 3.7 Flash High / Antigravity Agent in VSCode) | 43.38 | ± 0.00 | 48.9 | 100.4 | 2.06 | 0.05% | 13.19% |
| 9 | **demo4** | Demo 4 (Gemma 4 31B Thinking / Kilo Code) | 36.23 | ± 0.00 | 76.1 | 128.8 | 1.69 | 0.04% | 13.93% |

## Operation: `put`

| Rank | Implementation | Model | Throughput (ops/µs) | Margin (±) | Cycles/op | Insns/op | IPC | Branch Miss % | L1 D-Cache Miss % |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | **demo2** | Demo 2 (Kimi K3 Max / Kilo Code) | 63.60 | ± 0.00 | 41.9 | 129.5 | 3.09 | 0.11% | 6.15% |
| 2 | **demo1** | Demo 1 (Gemini 3.7 Flash High / Antigravity Agent in VSCode) | 57.09 | ± 0.00 | 44.6 | 131.5 | 2.95 | 0.09% | 6.11% |
| 3 | **demo7** | Demo 7 (Qwen 3.8 max XHigh / Kilo Code) | 56.28 | ± 0.00 | 44.9 | 140.5 | 3.13 | 0.12% | 4.65% |
| 4 | **demo5** | Demo 5 (DeepSeek V4 Flash Max / Kilo Code) | 46.42 | ± 0.00 | 52.1 | 162.0 | 3.11 | 0.09% | 5.39% |
| 5 | **demo6** | Demo 6 (Claude Opus 5 Ultra / Claude) | 45.39 | ± 0.00 | 53.4 | 149.1 | 2.80 | 0.22% | 5.71% |
| 6 | **demo8** | Demo 8 (Gemini 3.7 Flash High / Kilo Code in VSCode) | 44.38 | ± 0.00 | 54.9 | 159.1 | 2.90 | 0.21% | 6.11% |
| 7 | **demo3** | Demo 3 (OpenAI 5.6 Sol Max / Kilo Code) | 35.81 | ± 0.00 | 60.4 | 178.2 | 2.95 | 0.28% | 5.81% |
| 8 | **demo4** | Demo 4 (Gemma 4 31B Thinking / Kilo Code) | 35.57 | ± 0.00 | 75.6 | 183.3 | 2.42 | 0.35% | 4.75% |
| 9 | **demo9** | Demo 9 (Gemini 3.8 Flash High / Antigravity Agent natively) | 32.68 | ± 0.00 | 56.0 | 161.7 | 2.89 | 0.18% | 5.45% |

