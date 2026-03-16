# Lab 3: Hardware vs Software Transactional Memory

**Variant 15** | Course: Design of Complex Systems (PSS)

## Overview

Investigation of Hardware Transactional Memory (HTM) vs Software Transactional Memory (STM)
performance for concurrent database operations.

- **DBMS:** PostgreSQL
- **DB API:** java.sql (JDBC)
- **HTM:** `ReentrantLock` with Intel TSX-NI Hardware Lock Elision
- **STM:** Multiverse library (`StmUtils.atomic()`)

## Prerequisites

1. **JDK 8+** installed
2. **PostgreSQL** installed and running on `localhost:5432`
   - Default credentials: `postgres` / `postgres`
   - Database `lab3_benchmark` will be created automatically
3. **Library JARs** in `lib/` directory:
   - `postgresql-42.7.4.jar` — PostgreSQL JDBC driver
   - `multiverse-core-0.7.0.jar` — Multiverse STM library

### Download JARs

```bash
cd Lab3/lib
# PostgreSQL JDBC
curl -L -o postgresql-42.7.4.jar https://jdbc.postgresql.org/download/postgresql-42.7.4.jar
# Multiverse STM
curl -L -o multiverse-core-0.7.0.jar https://repo1.maven.org/maven2/org/multiverse/multiverse-core/0.7.0/multiverse-core-0.7.0.jar
```

## Build

```bash
cd Lab3
javac -cp "lib/*" -d out src/lab3/*.java
```

## Run

### Single Run (both HTM and STM)

```bash
java -cp "out;lib/*" lab3.Main
```

### Single Run (specific mode)

```bash
# HTM only
java -cp "out;lib/*" lab3.Main --mode=htm

# STM only
java -cp "out;lib/*" lab3.Main --mode=stm
```

### Parameters

```
--threads=N         Number of threads (default: 8)
--iterations=N      Iterations per thread (default: 50)
--records=N         Number of DB records (default: 100)
--mode=htm|stm|both Benchmark mode (default: both)
--db-url=URL        JDBC URL (default: jdbc:postgresql://localhost:5432/lab3_benchmark)
--db-user=USER      DB user (default: postgres)
--db-password=PASS  DB password (default: postgres)
```

### Full Benchmark

Runs both HTM and STM with thread counts {1, 2, 4, 8, 16}:

```bash
java -cp "out;lib/*" lab3.Main --benchmark
```

### Separate HTM/STM Benchmarks (for TSX-NI comparison)

```bash
# 1. With TSX-NI ENABLED — run HTM benchmark
java -cp "out;lib/*" lab3.Main --benchmark --mode=htm

# 2. Disable TSX-NI via registry, reboot
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Kernel" /v DisableTsx /t REG_DWORD /d 1 /f

# 3. After reboot — run STM benchmark
java -cp "out;lib/*" lab3.Main --benchmark --mode=stm

# 4. Re-enable TSX-NI
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Kernel" /v DisableTsx /t REG_DWORD /d 0 /f

# 5. Generate comparison report
java -cp "out;lib/*" lab3.Main --report
```

### Generate Report Only

```bash
java -cp "out;lib/*" lab3.Main --report
```

Opens `results/chart.html` with comparison graphs.

## Output

- `results/timing_HTM.csv` — HTM benchmark timing
- `results/timing_STM.csv` — STM benchmark timing
- `results/chart.html` — Interactive HTML report with comparison charts

## How It Works

### Database Setup

A PostgreSQL table `benchmark_data` is created with 100 records containing
randomly generated floating-point values (seed 42 for reproducibility).

### Benchmark Operations

Each thread concurrently executes UPDATE queries on shared table records:
```sql
UPDATE benchmark_data SET value = value * 1.01 + ? WHERE id = ?
```

This creates contention as multiple threads compete to modify overlapping records.

### HTM (Hardware Transactional Memory)

Uses `java.util.concurrent.locks.ReentrantLock` to synchronize access.
When Intel TSX-NI is enabled, the CPU applies **Hardware Lock Elision (HLE)**:
lock-protected critical sections are speculatively executed without actually
acquiring the lock. If no data conflicts occur, the speculation succeeds
and lock overhead is eliminated.

### STM (Software Transactional Memory)

Uses the **Multiverse** library's `StmUtils.atomic()` to wrap DB operations
in software-managed transactional blocks. Conflict detection and resolution
are managed entirely in software, adding overhead for versioning and
potential retries.

## Expected Results

HTM (with TSX-NI) should outperform STM due to:
- Hardware-level conflict detection (no software bookkeeping)
- Lock elision eliminates atomic CAS overhead
- CPU cache-level optimization for transactional regions

The performance gap typically increases with more threads, as STM overhead
grows with higher contention.
