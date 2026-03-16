package lab3;

import java.io.PrintStream;

/**
 * Runs benchmarks with fixed thread count (8) and varying iterations per thread.
 * X-axis = total queries (threads * iterations), Y-axis = time (ms).
 */
public class BenchmarkRunner {
    private final int threadCount;
    private final int[] iterationCounts;
    private final int recordCount;
    private final int warmupRuns;
    private final int measuredRuns;
    private final String mode;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public BenchmarkRunner(int threadCount, int[] iterationCounts, int recordCount,
                           int warmupRuns, int measuredRuns, String mode,
                           String dbUrl, String dbUser, String dbPassword) {
        this.threadCount = threadCount;
        this.iterationCounts = iterationCounts;
        this.recordCount = recordCount;
        this.warmupRuns = warmupRuns;
        this.measuredRuns = measuredRuns;
        this.mode = mode;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public void runAll() {
        boolean runHtm = mode.equals("htm") || mode.equals("both");
        boolean runStm = mode.equals("stm") || mode.equals("both");

        String htmCsvPath = Config.RESULTS_DIR + "timing_HTM.csv";
        String stmCsvPath = Config.RESULTS_DIR + "timing_STM.csv";

        if (runHtm) {
            java.io.File csvFile = new java.io.File(htmCsvPath);
            if (csvFile.exists()) csvFile.delete();
        }
        if (runStm) {
            java.io.File csvFile = new java.io.File(stmCsvPath);
            if (csvFile.exists()) csvFile.delete();
        }

        PrintStream originalOut = System.out;
        PrintStream nullOut = new PrintStream(new java.io.OutputStream() {
            public void write(int b) {}
            public void write(byte[] b, int off, int len) {}
        });

        for (int itersPerThread : iterationCounts) {
            int totalOps = threadCount * itersPerThread;
            ConsoleLogger.logMain("=== Benchmark: Threads=" + threadCount
                    + ", Iters/Thread=" + itersPerThread
                    + ", TotalOps=" + totalOps
                    + ", Records=" + recordCount + " ===");

            // HTM benchmark
            if (runHtm) {
                ConsoleLogger.logMain("Running HTM warmup (" + warmupRuns + " runs)...");
                for (int w = 0; w < warmupRuns; w++) {
                    DatabaseSetup.resetTable(dbUrl, dbUser, dbPassword, recordCount);
                    System.setOut(nullOut);
                    new HtmBenchmark(dbUrl, dbUser, dbPassword,
                            threadCount, itersPerThread, recordCount).execute();
                    System.setOut(originalOut);
                    System.gc();
                }

                ConsoleLogger.logMain("Running HTM measured (" + measuredRuns + " runs)...");
                long totalHtmNs = 0;
                for (int r = 0; r < measuredRuns; r++) {
                    DatabaseSetup.resetTable(dbUrl, dbUser, dbPassword, recordCount);
                    System.gc();
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}

                    System.setOut(nullOut);
                    long htmNs = new HtmBenchmark(dbUrl, dbUser, dbPassword,
                            threadCount, itersPerThread, recordCount).execute();
                    System.setOut(originalOut);
                    totalHtmNs += htmNs;
                }

                long avgHtmNs = totalHtmNs / measuredRuns;
                double htmMs = avgHtmNs / 1_000_000.0;
                ConsoleLogger.logMain("HTM: " + String.format("%.2f", htmMs) + " ms"
                        + " (" + totalOps + " total queries)");
                FileUtils.appendTimingCSV(htmCsvPath, threadCount, itersPerThread, avgHtmNs);
            }

            // STM benchmark
            if (runStm) {
                ConsoleLogger.logMain("Running STM warmup (" + warmupRuns + " runs)...");
                for (int w = 0; w < warmupRuns; w++) {
                    DatabaseSetup.resetTable(dbUrl, dbUser, dbPassword, recordCount);
                    System.setOut(nullOut);
                    new StmBenchmark(dbUrl, dbUser, dbPassword,
                            threadCount, itersPerThread, recordCount).execute();
                    System.setOut(originalOut);
                    System.gc();
                }

                ConsoleLogger.logMain("Running STM measured (" + measuredRuns + " runs)...");
                long totalStmNs = 0;
                for (int r = 0; r < measuredRuns; r++) {
                    DatabaseSetup.resetTable(dbUrl, dbUser, dbPassword, recordCount);
                    System.gc();
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}

                    System.setOut(nullOut);
                    long stmNs = new StmBenchmark(dbUrl, dbUser, dbPassword,
                            threadCount, itersPerThread, recordCount).execute();
                    System.setOut(originalOut);
                    totalStmNs += stmNs;
                }

                long avgStmNs = totalStmNs / measuredRuns;
                double stmMs = avgStmNs / 1_000_000.0;
                ConsoleLogger.logMain("STM: " + String.format("%.2f", stmMs) + " ms"
                        + " (" + totalOps + " total queries)");
                FileUtils.appendTimingCSV(stmCsvPath, threadCount, itersPerThread, avgStmNs);
            }
        }

        if (runHtm) ConsoleLogger.logMain("HTM timing saved to " + htmCsvPath);
        if (runStm) ConsoleLogger.logMain("STM timing saved to " + stmCsvPath);
    }
}
