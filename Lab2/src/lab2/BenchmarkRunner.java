package lab2;

import java.io.PrintStream;

public class BenchmarkRunner {
    private final int[] sizes;
    private final int threadCount;
    private final int warmupRuns;
    private final int measuredRuns;
    private final String label;

    public BenchmarkRunner(int[] sizes, int threadCount, int warmupRuns, int measuredRuns, String label) {
        this.sizes = sizes;
        this.threadCount = threadCount;
        this.warmupRuns = warmupRuns;
        this.measuredRuns = measuredRuns;
        this.label = label;
    }

    public void runAll() {
        String csvPath = Config.RESULTS_DIR + "timing_" + label.replace(" ", "_") + ".csv";
        java.io.File csvFile = new java.io.File(csvPath);
        if (csvFile.exists()) csvFile.delete();

        // Save original stdout, create a null stream for suppressing worker output
        PrintStream originalOut = System.out;
        PrintStream nullOut = new PrintStream(new java.io.OutputStream() {
            public void write(int b) {}
            public void write(byte[] b, int off, int len) {}
        });

        for (int n : sizes) {
            int adjustedN = n;
            if (adjustedN % threadCount != 0) {
                adjustedN = ((adjustedN / threadCount) + 1) * threadCount;
            }

            ConsoleLogger.logMain("=== Benchmark [" + label + "] N=" + adjustedN + ", P=" + threadCount + " ===");

            // Generate data with same seed as Lab1 for reproducibility
            DataGenerator gen = new DataGenerator(adjustedN);
            double[][] MT = gen.generateMatrix();
            double[][] MX = gen.generateMatrix();
            double[] B = gen.generateVector();
            double[] E = gen.generateVector();

            gen.saveAll(MT, MX, B, E);
            Object[] loaded = DataGenerator.loadAll(adjustedN);
            MT = (double[][]) loaded[0];
            MX = (double[][]) loaded[1];
            B = (double[]) loaded[2];
            E = (double[]) loaded[3];

            // Warmup (suppress worker output)
            System.setOut(nullOut);
            for (int w = 0; w < warmupRuns; w++) {
                new LockVersion(adjustedN, threadCount, MT, MX, B, E).execute();
                System.gc();
            }
            System.setOut(originalOut);

            // Measured runs
            long totalLockNs = 0;
            double[][] lastMD = null;
            double[] lastD = null;

            for (int r = 0; r < measuredRuns; r++) {
                System.gc();
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}

                System.setOut(nullOut);
                LockVersion lock = new LockVersion(adjustedN, threadCount, MT, MX, B, E);
                long start = System.nanoTime();
                lock.execute();
                long end = System.nanoTime();
                System.setOut(originalOut);
                totalLockNs += (end - start);

                if (r == 0) {
                    lastMD = lock.getMD();
                    lastD = lock.getD();

                    // Save results for this size
                    FileUtils.writeResultMatrix(
                            Config.RESULTS_DIR + "MD_n" + adjustedN + ".txt", lastMD);
                    FileUtils.writeResultVector(
                            Config.RESULTS_DIR + "D_n" + adjustedN + ".txt", lastD);
                }
            }

            long avgLockNs = totalLockNs / measuredRuns;

            ConsoleLogger.logMain("N=" + adjustedN + " | Lock: " + (avgLockNs / 1_000_000)
                    + " ms (" + label + ")");

            // Print first elements for verification
            if (lastMD != null) {
                printFirstElements(lastMD, lastD, adjustedN);
            }

            FileUtils.appendLockTimingCSV(csvPath, adjustedN, threadCount, avgLockNs);
        }

        ConsoleLogger.logMain("Timing saved to " + csvPath);
    }

    private void printFirstElements(double[][] MD, double[] D, int n) {
        int show = Math.min(5, n);
        StringBuilder sb = new StringBuilder();
        sb.append("  MD[0][0..").append(show - 1).append("] = ");
        for (int j = 0; j < show; j++) {
            if (j > 0) sb.append(", ");
            sb.append(String.format("%.6f", MD[0][j]));
        }
        ConsoleLogger.logMain(sb.toString());

        sb = new StringBuilder();
        sb.append("  D[0..").append(show - 1).append("] = ");
        for (int i = 0; i < show; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.6f", D[i]));
        }
        ConsoleLogger.logMain(sb.toString());
    }
}
