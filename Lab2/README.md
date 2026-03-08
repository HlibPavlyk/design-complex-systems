# Lab 2: Intel TSX-NI Hardware Lock Elision

Multithreaded Java application investigating the impact of Intel's Hardware Lock Elision (TSX-NI) on parallel computation with `ReentrantLock` synchronization.

## Variant 15

```
MD = min(MT) * (MT + MX) - MT * MX
D  = (MT + MX) * B - (MT - MX) * E
```

Where:
- `MT`, `MX` — square matrices of size N x N
- `B`, `E` — vectors of size N
- `MD` — result matrix (N x N)
- `D` — result vector (N)

## How It Works

This lab modifies **Variant 2 (Shared Version)** from Lab 1, replacing `synchronized` blocks with fine-grained `ReentrantLock` from `java.util.concurrent.locks`.

### Lock-Based Version
- All threads read from the **same shared** arrays (MT, MX, B, E)
- Writes to result arrays (MD, D) are protected by fine-grained `ReentrantLock` — one lock acquisition per element write
- Each critical section is minimal (single assignment), enabling Intel TSX-NI to speculatively execute without acquiring the lock

### TSX-NI Hardware Lock Elision
When TSX-NI is **enabled**, the CPU detects that lock-protected regions don't cause memory conflicts between threads (each thread writes to different rows). The lock is elided — threads proceed without atomic operations.

When TSX-NI is **disabled**, every `lock()`/`unlock()` is a real atomic CAS operation, introducing serialization overhead despite no actual data conflicts.

### Synchronization
- **CyclicBarrier** — ensures all threads complete `min(MT)` computation before proceeding
- **ReentrantLock** (fine-grained) — protects individual element writes to MD[i][j] and D[i]
- **Kahan summation** — used in all dot products for numerical stability
- **Anonymous Runnable** — thread workers created as anonymous inner classes

## Requirements

- Java 8 or higher (JDK)
- Intel CPU with TSX-NI support (for meaningful comparison)
- No external dependencies

## Project Structure

```
Lab2/
  src/lab2/
    Main.java            — entry point, CLI argument parsing
    Config.java          — constants (default N, P, benchmark sizes, paths)
    LockVersion.java     — parallel computation with ReentrantLock + anonymous Runnable
    BenchmarkRunner.java — runs benchmarks across multiple N sizes
    GraphPlotter.java    — generates HTML report comparing TSX-Enabled vs TSX-Disabled
    KahanAccumulator.java— Kahan summation for numerical stability
    DataGenerator.java   — generates random input data, save/load
    FileUtils.java       — file I/O for matrices, vectors, CSV
    ResultValidator.java — compares results for correctness
    ConsoleLogger.java   — thread-safe console output
  data/                  — generated input files
  results/               — output files (MD.txt, D.txt, timing CSVs, chart.html)
  out/                   — compiled .class files
```

## Build

```bash
cd Lab2
javac -d out src/lab2/*.java
```

## Run

### Single run (loads data from Lab1)

```bash
java -cp out lab2.Main
```

### Single run with custom parameters

```bash
java -cp out lab2.Main --n=1000 --p=8
```

| Flag | Description | Default |
|------|-------------|---------|
| `--n=<size>` | Matrix/vector dimension N | 500 |
| `--p=<threads>` | Number of threads | 4 |
| `--label=<name>` | Label for timing (TSX-Enabled or TSX-Disabled) | TSX-Enabled |

### Benchmark mode (TSX comparison workflow)

**Step 1: Run with TSX-NI enabled**
```bash
java -cp out lab2.Main --benchmark --label=TSX-Enabled
```

**Step 2: Disable TSX-NI and reboot**
```cmd
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Kernel" /v DisableTsx /t REG_DWORD /d 1 /f
shutdown /r /t 0
```

**Step 3: Run with TSX-NI disabled**
```bash
java -cp out lab2.Main --benchmark --label=TSX-Disabled
```

**Step 4: Generate comparison report**
```bash
java -cp out lab2.Main --report
```

**Step 5: Re-enable TSX-NI**
```cmd
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Kernel" /v DisableTsx /t REG_DWORD /d 0 /f
shutdown /r /t 0
```

| Flag | Description | Default |
|------|-------------|---------|
| `--benchmark` | Enable benchmark mode | off |
| `--report` | Generate comparison report from both CSVs | off |
| `--warmup=<n>` | Number of JVM warmup runs per size | 2 |
| `--runs=<n>` | Number of measured runs per size | 3 |

## Output

### Single run
- `results/MD.txt` — result matrix MD
- `results/D.txt` — result vector D
- Console: timing, first elements of results, cross-validation with Lab1

### Benchmark mode
- `results/timing_TSX-Enabled.csv` — timing with TSX enabled
- `results/timing_TSX-Disabled.csv` — timing with TSX disabled
- `results/chart.html` — interactive HTML report with comparison charts

## Results Validation

The program automatically validates results against Lab1 output when available. Results are identical because the computation is the same — only the synchronization mechanism differs (`ReentrantLock` instead of `synchronized`).
