# JMH Microbenchmark Cross-Project Comparison Report

Microbenchmark results comparing all TDD Open Hashing Map implementations (Size = 1,000 items).

## Operation: `getHit`

| Rank | Implementation | Model | Throughput (ops/µs) | Margin (±) |
| :--- | :--- | :--- | :--- | :--- |
| 1 | **demo9** | Demo 9 (Gemini 3.8 Flash High / Antigravity Agent natively) | 63.33 | ± 0.00 |
| 2 | **demo1** | Demo 1 (Gemini 3.7 Flash High / Antigravity Agent in VSCode) | 58.31 | ± 0.00 |
| 3 | **demo2** | Demo 2 (Kimi K3 Max / Kilo Code) | 58.14 | ± 0.00 |
| 4 | **demo7** | Demo 7 (Qwen 3.8 max XHigh / Kilo Code) | 52.67 | ± 0.00 |
| 5 | **demo8** | Demo 8 (Gemini 3.7 Flash High / Kilo Code in VSCode) | 52.32 | ± 0.00 |
| 6 | **demo5** | Demo 5 (DeepSeek V4 Flash Max / Kilo Code) | 49.40 | ± 0.00 |
| 7 | **demo3** | Demo 3 (OpenAI 5.6 Sol Max / Kilo Code) | 40.98 | ± 0.00 |
| 8 | **demo4** | Demo 4 (Gemma 4 31B Thinking / Kilo Code) | 40.03 | ± 0.00 |
| 9 | **demo6** | Demo 6 (Claude Opus 5 Ultra / Claude) | 39.33 | ± 0.00 |

## Operation: `getMiss`

| Rank | Implementation | Model | Throughput (ops/µs) | Margin (±) |
| :--- | :--- | :--- | :--- | :--- |
| 1 | **demo8** | Demo 8 (Gemini 3.7 Flash High / Kilo Code in VSCode) | 74.25 | ± 0.00 |
| 2 | **demo9** | Demo 9 (Gemini 3.8 Flash High / Antigravity Agent natively) | 67.32 | ± 0.00 |
| 3 | **demo5** | Demo 5 (DeepSeek V4 Flash Max / Kilo Code) | 66.49 | ± 0.00 |
| 4 | **demo7** | Demo 7 (Qwen 3.8 max XHigh / Kilo Code) | 60.16 | ± 0.00 |
| 5 | **demo6** | Demo 6 (Claude Opus 5 Ultra / Claude) | 59.50 | ± 0.00 |
| 6 | **demo3** | Demo 3 (OpenAI 5.6 Sol Max / Kilo Code) | 58.83 | ± 0.00 |
| 7 | **demo2** | Demo 2 (Kimi K3 Max / Kilo Code) | 51.44 | ± 0.00 |
| 8 | **demo1** | Demo 1 (Gemini 3.7 Flash High / Antigravity Agent in VSCode) | 50.35 | ± 0.00 |
| 9 | **demo4** | Demo 4 (Gemma 4 31B Thinking / Kilo Code) | 35.42 | ± 0.00 |

## Operation: `put`

| Rank | Implementation | Model | Throughput (ops/µs) | Margin (±) |
| :--- | :--- | :--- | :--- | :--- |
| 1 | **demo2** | Demo 2 (Kimi K3 Max / Kilo Code) | 48.61 | ± 0.00 |
| 2 | **demo1** | Demo 1 (Gemini 3.7 Flash High / Antigravity Agent in VSCode) | 48.25 | ± 0.00 |
| 3 | **demo3** | Demo 3 (OpenAI 5.6 Sol Max / Kilo Code) | 43.60 | ± 0.00 |
| 4 | **demo7** | Demo 7 (Qwen 3.8 max XHigh / Kilo Code) | 34.81 | ± 0.00 |
| 5 | **demo9** | Demo 9 (Gemini 3.8 Flash High / Antigravity Agent natively) | 34.12 | ± 0.00 |
| 6 | **demo6** | Demo 6 (Claude Opus 5 Ultra / Claude) | 32.84 | ± 0.00 |
| 7 | **demo8** | Demo 8 (Gemini 3.7 Flash High / Kilo Code in VSCode) | 32.57 | ± 0.00 |
| 8 | **demo4** | Demo 4 (Gemma 4 31B Thinking / Kilo Code) | 25.35 | ± 0.00 |
| 9 | **demo5** | Demo 5 (DeepSeek V4 Flash Max / Kilo Code) | 24.55 | ± 0.00 |

