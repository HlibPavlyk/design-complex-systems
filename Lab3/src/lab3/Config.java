package lab3;

public class Config {
    // Database connection
    public static final String DB_URL = "jdbc:postgresql://localhost:5432/lab3_benchmark";
    public static final String DB_USER = "postgres";
    public static final String DB_PASSWORD = "postgres";

    // Benchmark parameters
    public static final int DEFAULT_THREADS = 8;
    public static final int DEFAULT_ITERATIONS_PER_THREAD = 50;
    public static final int RECORD_COUNT = 100;

    // Benchmark: fixed 8 threads, varying iterations per thread
    public static final int BENCHMARK_THREADS = 8;
    // total_ops = BENCHMARK_THREADS * iters → 200,400,600,1000,1200,1400,1800,2000,2400
    public static final int[] BENCHMARK_ITERATIONS = {25, 50, 75, 125, 150, 175, 225, 250, 300};

    // Benchmark control
    public static final int WARMUP_RUNS = 2;
    public static final int MEASURED_RUNS = 3;

    // File paths
    public static final String RESULTS_DIR = "results/";
}
