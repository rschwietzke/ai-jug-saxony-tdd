# Java Object Layout (JOL) Cross-Project Memory Footprint Report

Comprehensive memory layout and footprint analysis comparing all TDDHashMap implementations.

## 1. Footprint & Efficiency Comparison

| Implementation | Model | Shallow Size | Empty (B) | N=100 (B) | N=100 (B/entry) | N=1,000 (B) | N=1,000 (B/entry) | N=10,000 (B) | N=10,000 (B/entry) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Demo 1** | Gemini 3.7 Flash High (Antigravity Agent in VSCode) | 40 B | 200 B | 8,520 B | 85.2 | 80,456 B | 80.5 | 771,144 B | 77.1 |
| **Demo 2** | Kimi K3 Max (Kilo Code) | 32 B | 192 B | 8,512 B | 85.1 | 80,448 B | 80.4 | 771,136 B | 77.1 |
| **Demo 3** | OpenAI 5.6 Sol Max (Kilo Code) | 40 B | 232 B | 8,792 B | 87.9 | 82,520 B | 82.5 | 787,544 B | 78.8 |
| **Demo 4** | Gemma 4 31B Thinking (Kilo Code) | 32 B | 192 B | 8,512 B | 85.1 | 80,448 B | 80.4 | 771,136 B | 77.1 |
| **Demo 5** | DeepSeek V4 Flash Max (Kilo Code) | 32 B | 192 B | 8,512 B | 85.1 | 80,448 B | 80.4 | 902,208 B | 90.2 |
| **Demo 6** | Claude Opus 5 Ultra (Claude) | 24 B | 184 B | 8,504 B | 85.0 | 80,440 B | 80.4 | 902,200 B | 90.2 |
| **Demo 7** | Qwen 3.8 max XHigh (Kilo Code) | 32 B | 192 B | 8,512 B | 85.1 | 80,448 B | 80.4 | 771,136 B | 77.1 |
| **Demo 8** | Gemini 3.7 Flash High (Kilo.Code Agent in VSCode) | 32 B | 192 B | 8,512 B | 85.1 | 80,448 B | 80.4 | 771,136 B | 77.1 |
| **Demo 9** | Gemini 3.8 Flash High (Antigravity Agent natively) | 32 B | 192 B | 8,512 B | 85.1 | 80,448 B | 80.4 | 771,136 B | 77.1 |

## 2. Total Internal Object Count (GC Pressure)

| Implementation | Model | Empty Objects | Objects @ N=100 | Objects @ N=1,000 | Objects @ N=10,000 | Memory Strategy |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Demo 1** | Gemini 3.7 Flash High (Antigravity Agent in VSCode) | 3 | 303 | 3,003 | 30,003 | Node/Entry Objects |
| **Demo 2** | Kimi K3 Max (Kilo Code) | 3 | 303 | 3,003 | 30,003 | Node/Entry Objects |
| **Demo 3** | OpenAI 5.6 Sol Max (Kilo Code) | 4 | 304 | 3,004 | 30,004 | Node/Entry Objects |
| **Demo 4** | Gemma 4 31B Thinking (Kilo Code) | 3 | 303 | 3,003 | 30,003 | Node/Entry Objects |
| **Demo 5** | DeepSeek V4 Flash Max (Kilo Code) | 3 | 303 | 3,003 | 30,003 | Node/Entry Objects |
| **Demo 6** | Claude Opus 5 Ultra (Claude) | 3 | 303 | 3,003 | 30,003 | Node/Entry Objects |
| **Demo 7** | Qwen 3.8 max XHigh (Kilo Code) | 3 | 303 | 3,003 | 30,003 | Node/Entry Objects |
| **Demo 8** | Gemini 3.7 Flash High (Kilo.Code Agent in VSCode) | 3 | 303 | 3,003 | 30,003 | Node/Entry Objects |
| **Demo 9** | Gemini 3.8 Flash High (Antigravity Agent natively) | 3 | 303 | 3,003 | 30,003 | Node/Entry Objects |

## 3. Class Layout Details

### Demo 1 (Gemini 3.7 Flash High (Antigravity Agent in VSCode))
```
org.jugsaxony.tdd.demo1.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION               VALUE
  0   8                      (object header: mark)     N/A
  8   4                      (object header: class)    N/A
 12   4                  int TDDHashMap.size           N/A
 16   4                  int TDDHashMap.capacity       N/A
 20   4                  int TDDHashMap.mask           N/A
 24   4                  int TDDHashMap.threshold      N/A
 28   4                float TDDHashMap.loadFactor     N/A
 32   4   java.lang.Object[] TDDHashMap.keys           N/A
 36   4   java.lang.Object[] TDDHashMap.values         N/A
Instance size: 40 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
```

### Demo 2 (Kimi K3 Max (Kilo Code))
```
org.jugsaxony.tdd.demo2.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION               VALUE
  0   8                      (object header: mark)     N/A
  8   4                      (object header: class)    N/A
 12   4                  int TDDHashMap.size           N/A
 16   4                  int TDDHashMap.mask           N/A
 20   4                  int TDDHashMap.threshold      N/A
 24   4   java.lang.Object[] TDDHashMap.keys           N/A
 28   4   java.lang.Object[] TDDHashMap.values         N/A
Instance size: 32 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
```

### Demo 3 (OpenAI 5.6 Sol Max (Kilo Code))
```
org.jugsaxony.tdd.demo3.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION                  VALUE
  0   8                      (object header: mark)        N/A
  8   4                      (object header: class)       N/A
 12   4                  int TDDHashMap.size              N/A
 16   4                  int TDDHashMap.usedSlots         N/A
 20   4                  int TDDHashMap.resizeThreshold   N/A
 24   4   java.lang.Object[] TDDHashMap.keys              N/A
 28   4   java.lang.Object[] TDDHashMap.values            N/A
 32   4               byte[] TDDHashMap.states            N/A
 36   4                      (object alignment gap)       
Instance size: 40 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

### Demo 4 (Gemma 4 31B Thinking (Kilo Code))
```
org.jugsaxony.tdd.demo4.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION               VALUE
  0   8                      (object header: mark)     N/A
  8   4                      (object header: class)    N/A
 12   4                  int TDDHashMap.size           N/A
 16   4                  int TDDHashMap.capacity       N/A
 20   4   java.lang.Object[] TDDHashMap.keys           N/A
 24   4   java.lang.Object[] TDDHashMap.values         N/A
 28   4                      (object alignment gap)    
Instance size: 32 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

### Demo 5 (DeepSeek V4 Flash Max (Kilo Code))
```
org.jugsaxony.tdd.demo5.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION               VALUE
  0   8                      (object header: mark)     N/A
  8   4                      (object header: class)    N/A
 12   4                  int TDDHashMap.size           N/A
 16   4                  int TDDHashMap.tombstones     N/A
 20   4   java.lang.Object[] TDDHashMap.keys           N/A
 24   4   java.lang.Object[] TDDHashMap.values         N/A
 28   4                      (object alignment gap)    
Instance size: 32 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

### Demo 6 (Claude Opus 5 Ultra (Claude))
```
org.jugsaxony.tdd.demo6.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION               VALUE
  0   8                      (object header: mark)     N/A
  8   4                      (object header: class)    N/A
 12   4                  int TDDHashMap.size           N/A
 16   4   java.lang.Object[] TDDHashMap.keys           N/A
 20   4   java.lang.Object[] TDDHashMap.values         N/A
Instance size: 24 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
```

### Demo 7 (Qwen 3.8 max XHigh (Kilo Code))
```
org.jugsaxony.tdd.demo7.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION               VALUE
  0   8                      (object header: mark)     N/A
  8   4                      (object header: class)    N/A
 12   4                  int TDDHashMap.size           N/A
 16   4                  int TDDHashMap.threshold      N/A
 20   4   java.lang.Object[] TDDHashMap.keys           N/A
 24   4   java.lang.Object[] TDDHashMap.values         N/A
 28   4                      (object alignment gap)    
Instance size: 32 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

### Demo 8 (Gemini 3.7 Flash High (Kilo.Code Agent in VSCode))
```
org.jugsaxony.tdd.demo8.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION               VALUE
  0   8                      (object header: mark)     N/A
  8   4                      (object header: class)    N/A
 12   4                  int TDDHashMap.size           N/A
 16   4                  int TDDHashMap.threshold      N/A
 20   4   java.lang.Object[] TDDHashMap.keys           N/A
 24   4   java.lang.Object[] TDDHashMap.values         N/A
 28   4                      (object alignment gap)    
Instance size: 32 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

### Demo 9 (Gemini 3.8 Flash High (Antigravity Agent natively))
```
org.jugsaxony.tdd.demo9.TDDHashMap object internals:
OFF  SZ                 TYPE DESCRIPTION               VALUE
  0   8                      (object header: mark)     N/A
  8   4                      (object header: class)    N/A
 12   4                  int TDDHashMap.size           N/A
 16   4                  int TDDHashMap.threshold      N/A
 20   4   java.lang.Object[] TDDHashMap.keys           N/A
 24   4   java.lang.Object[] TDDHashMap.values         N/A
 28   4                      (object alignment gap)    
Instance size: 32 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

