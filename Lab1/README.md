# Lab 1: MESIF Cache Coherence Protocol Simulation

Multithreaded Java application that demonstrates the impact of the MESIF cache coherence protocol on parallel computation performance.

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

The program implements two parallel computation strategies and compares their performance:

### Local Version (no MESIF benefit)
Each thread **deep-copies** all input data (MT, MX, B, E) into thread-local arrays before computation. This eliminates inter-core cache sharing — each core works entirely from its own L1D cache. The deep-copy overhead is included in timing.

### Shared Version (MESIF benefit)
All threads read from the **same shared** arrays. The MESIF protocol allows cores to fetch shared read-only data directly from another core's L1D cache (Forward state) instead of going to main memory, reducing latency. Writes to result arrays (MD, D) are protected by `synchronized` blocks with separate lock objects.

### Synchronization
- **CyclicBarrier** — ensures all threads complete `min(MT)` computation before proceeding to phases 2-3
- **synchronized blocks** — protect shared result writes (SharedVersion) and global min updates
- **Kahan summation** — used in all dot products and accumulations to minimize floating-point rounding errors

## Requirements

- Java 8 or higher (JDK)
- No external dependencies

## Project Structure

```
Lab1/
  src/lab1/
    Main.java            — entry point, CLI argument parsing
    Config.java          — constants (default N, P, benchmark sizes, paths)
    BenchmarkRunner.java — runs benchmarks across multiple N sizes
    GraphPlotter.java    — generates HTML report with SVG charts
    LocalVersion.java    — orchestrates local (deep-copy) threads
    LocalWorker.java     — worker thread for local version
    SharedVersion.java   — orchestrates shared-memory threads
    SharedWorker.java    — worker thread for shared version
    MatrixVectorOps.java — matrix/vector math utilities
    KahanAccumulator.java— Kahan summation for numerical stability
    DataGenerator.java   — generates random input data, save/load
    FileUtils.java       — file I/O for matrices, vectors, CSV
    ResultValidator.java — compares Local vs Shared results
    ConsoleLogger.java   — thread-safe console output
  data/                  — generated input files (MT.txt, MX.txt, B.txt, E.txt)
  results/               — output files (MD.txt, D.txt, timing.csv, chart.html)
  out/                   — compiled .class files
```

## Build

```bash
cd Lab1
javac -d out src/lab1/*.java
```

## Run

### Single run (default N=500, P=4)

```bash
java -cp out lab1.Main
```

### Single run with custom parameters

```bash
java -cp out lab1.Main --n=1000 --p=8
```

| Flag | Description | Default |
|------|-------------|---------|
| `--n=<size>` | Matrix/vector dimension N | 500 |
| `--p=<threads>` | Number of threads | 4 |

> N is automatically adjusted upward to be divisible by P.

### Benchmark mode

Runs both versions across multiple N sizes (100, 200, 500, 1000, 1200, 1500, 1800, 2200, 2500), with warmup runs and measured runs, then generates a performance report.

```bash
java -cp out lab1.Main --benchmark
```

With custom warmup/measurement settings:

```bash
java -cp out lab1.Main --benchmark --warmup=3 --runs=5
```

| Flag | Description | Default |
|------|-------------|---------|
| `--benchmark` | Enable benchmark mode | off |
| `--warmup=<n>` | Number of JVM warmup runs per size | 2 |
| `--runs=<n>` | Number of measured runs per size (results are averaged) | 3 |

## Output

### Single run
- `results/MD.txt` — result matrix MD
- `results/D.txt` — result vector D
- Console: timing comparison and speedup coefficient

### Benchmark mode
- `results/timing.csv` — timing data for all sizes
- `results/chart.html` — interactive HTML report with:
  - Line chart comparing Local vs Shared execution time
  - Bar chart showing speedup coefficient per N
  - Summary table with all measurements
- `results/MD_n<N>.txt`, `results/D_n<N>.txt` — results per size
- Console: per-size timing table

Open `results/chart.html` in any browser to view the performance charts.

## Example Output

```
[Main] === Benchmark for N=1000, P=4 ===
[Main] Results match: MD=true, D=true
[Main] N=1000 | Local: 1445 ms | Shared: 1178 ms | Speedup: 1.23

=== Performance Comparison ===
N        | Local(ms)    | Shared(ms)   | Ratio
---------+--------------+--------------+---------
100      | 4.75         | 5.43         | 1.14
500      | 119.46       | 98.31        | 0.82
1000     | 1445.13      | 1178.12      | 0.82
2500     | 59666.24     | 42567.89     | 0.71
```

## Expected Results

- For small N (100-200): overhead from thread creation dominates, so speedup may be < 1.0
- For large N (500+): the Shared version is consistently faster (speedup 1.2x-1.5x) because MESIF allows efficient cache-to-cache data sharing without deep-copy overhead
