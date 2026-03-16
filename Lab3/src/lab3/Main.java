package lab3;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        int threads = Config.DEFAULT_THREADS;
        int iterations = Config.DEFAULT_ITERATIONS_PER_THREAD;
        int records = Config.RECORD_COUNT;
        boolean benchmarkMode = false;
        boolean reportMode = false;
        String mode = "both"; // "htm", "stm", "both"
        String dbUrl = Config.DB_URL;
        String dbUser = Config.DB_USER;
        String dbPassword = Config.DB_PASSWORD;
        int warmupRuns = Config.WARMUP_RUNS;
        int measuredRuns = Config.MEASURED_RUNS;

        for (String arg : args) {
            if (arg.startsWith("--threads=")) threads = Integer.parseInt(arg.substring(10));
            else if (arg.startsWith("--iterations=")) iterations = Integer.parseInt(arg.substring(13));
            else if (arg.startsWith("--records=")) records = Integer.parseInt(arg.substring(10));
            else if (arg.equals("--benchmark")) benchmarkMode = true;
            else if (arg.equals("--report")) reportMode = true;
            else if (arg.startsWith("--mode=")) mode = arg.substring(7);
            else if (arg.startsWith("--db-url=")) dbUrl = arg.substring(9);
            else if (arg.startsWith("--db-user=")) dbUser = arg.substring(10);
            else if (arg.startsWith("--db-password=")) dbPassword = arg.substring(14);
            else if (arg.startsWith("--warmup=")) warmupRuns = Integer.parseInt(arg.substring(9));
            else if (arg.startsWith("--runs=")) measuredRuns = Integer.parseInt(arg.substring(7));
        }

        new File(Config.RESULTS_DIR).mkdirs();

        if (reportMode) {
            String htmCsv = Config.RESULTS_DIR + "timing_HTM.csv";
            String stmCsv = Config.RESULTS_DIR + "timing_STM.csv";

            if (!new File(htmCsv).exists()) {
                ConsoleLogger.logMain("Missing " + htmCsv + ". Run benchmark with --mode=htm first.");
                return;
            }
            if (!new File(stmCsv).exists()) {
                ConsoleLogger.logMain("Missing " + stmCsv + ". Run benchmark with --mode=stm first.");
                return;
            }

            GraphPlotter.generateReport(htmCsv, stmCsv, Config.RESULTS_DIR + "chart.html");
            ConsoleLogger.logMain("Report generated. Open " + Config.RESULTS_DIR + "chart.html in a browser.");

        } else if (benchmarkMode) {
            ConsoleLogger.logMain("Starting benchmark: mode=" + mode
                    + ", threads=" + threads
                    + ", records=" + records);
            ConsoleLogger.logMain("Iterations per thread: " + java.util.Arrays.toString(Config.BENCHMARK_ITERATIONS));
            ConsoleLogger.logMain("Total queries: threads * iters/thread");

            // Setup DB
            DatabaseSetup.createDatabase(dbUrl, dbUser, dbPassword);
            DatabaseSetup.createTable(dbUrl, dbUser, dbPassword);
            DatabaseSetup.populateTable(dbUrl, dbUser, dbPassword, records);

            BenchmarkRunner runner = new BenchmarkRunner(
                    threads, Config.BENCHMARK_ITERATIONS, records,
                    warmupRuns, measuredRuns, mode,
                    dbUrl, dbUser, dbPassword);
            runner.runAll();

            // Auto-generate report if both modes were run
            if (mode.equals("both")) {
                String htmCsv = Config.RESULTS_DIR + "timing_HTM.csv";
                String stmCsv = Config.RESULTS_DIR + "timing_STM.csv";
                if (new File(htmCsv).exists() && new File(stmCsv).exists()) {
                    GraphPlotter.generateReport(htmCsv, stmCsv, Config.RESULTS_DIR + "chart.html");
                    ConsoleLogger.logMain("Report generated. Open " + Config.RESULTS_DIR + "chart.html in a browser.");
                }
            }

            ConsoleLogger.logMain("Benchmark complete.");
            ConsoleLogger.logMain("Usage:");
            ConsoleLogger.logMain("  HTM only:  java -cp \"out;lib/*\" lab3.Main --benchmark --mode=htm");
            ConsoleLogger.logMain("  STM only:  java -cp \"out;lib/*\" lab3.Main --benchmark --mode=stm");
            ConsoleLogger.logMain("  Both:      java -cp \"out;lib/*\" lab3.Main --benchmark --mode=both");
            ConsoleLogger.logMain("  Report:    java -cp \"out;lib/*\" lab3.Main --report");

        } else {
            // Single run mode
            ConsoleLogger.logMain("=== Lab 3: HTM vs STM Database Benchmark ===");
            ConsoleLogger.logMain("Variant 15 | DBMS: PostgreSQL | API: java.sql (JDBC)");
            ConsoleLogger.logMain("Threads=" + threads
                    + ", Iterations/Thread=" + iterations
                    + ", Records=" + records
                    + ", Mode=" + mode);

            // Setup DB
            DatabaseSetup.createDatabase(dbUrl, dbUser, dbPassword);
            DatabaseSetup.createTable(dbUrl, dbUser, dbPassword);
            DatabaseSetup.populateTable(dbUrl, dbUser, dbPassword, records);

            boolean runHtm = mode.equals("htm") || mode.equals("both");
            boolean runStm = mode.equals("stm") || mode.equals("both");

            long htmNs = 0;
            long stmNs = 0;

            if (runHtm) {
                ConsoleLogger.logMain("=== HTM BENCHMARK (ReentrantLock + JDBC) ===");
                ConsoleLogger.logMain("When TSX-NI is enabled, ReentrantLock benefits from Hardware Lock Elision.");
                DatabaseSetup.resetTable(dbUrl, dbUser, dbPassword, records);
                HtmBenchmark htm = new HtmBenchmark(dbUrl, dbUser, dbPassword, threads, iterations, records);
                htmNs = htm.execute();
                ConsoleLogger.logMain("HTM time: " + String.format("%.2f", htmNs / 1_000_000.0) + " ms");
            }

            if (runStm) {
                ConsoleLogger.logMain("=== STM BENCHMARK (Multiverse + JDBC) ===");
                ConsoleLogger.logMain("Software Transactional Memory via Multiverse StmUtils.atomic().");
                DatabaseSetup.resetTable(dbUrl, dbUser, dbPassword, records);
                StmBenchmark stm = new StmBenchmark(dbUrl, dbUser, dbPassword, threads, iterations, records);
                stmNs = stm.execute();
                ConsoleLogger.logMain("STM time: " + String.format("%.2f", stmNs / 1_000_000.0) + " ms");
            }

            if (runHtm && runStm) {
                double htmMs = htmNs / 1_000_000.0;
                double stmMs = stmNs / 1_000_000.0;
                double ratio = htmMs > 0 ? stmMs / htmMs : 0;
                ConsoleLogger.logMain("=== COMPARISON ===");
                ConsoleLogger.logMain("HTM: " + String.format("%.2f", htmMs) + " ms");
                ConsoleLogger.logMain("STM: " + String.format("%.2f", stmMs) + " ms");
                ConsoleLogger.logMain("STM/HTM ratio: " + String.format("%.2f", ratio)
                        + (ratio > 1.0 ? " (STM is slower)" : " (STM is faster)"));
            }

            ConsoleLogger.logMain("Done. Use --benchmark for full benchmark with multiple thread counts.");
        }
    }
}
