package lab1;

import java.io.PrintStream;

public class BenchmarkRunner {
    private final int[] sizes;
    private final int threadCount;
    private final int warmupRuns;
    private final int measuredRuns;

    public BenchmarkRunner(int[] sizes, int threadCount, int warmupRuns, int measuredRuns) {
        this.sizes = sizes;
        this.threadCount = threadCount;
        this.warmupRuns = warmupRuns;
        this.measuredRuns = measuredRuns;
    }

    public void runAll() {
        java.io.File csvFile = new java.io.File(Config.RESULTS_DIR + "timing.csv");
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

            ConsoleLogger.logMain("=== Benchmark for N=" + adjustedN + ", P=" + threadCount + " ===");

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
                new LocalVersion(adjustedN, threadCount, MT, MX, B, E).execute();
                System.gc();
                new SharedVersion(adjustedN, threadCount, MT, MX, B, E).execute();
                System.gc();
            }
            System.setOut(originalOut);

            // Measured runs (suppress worker output)
            long totalLocalNs = 0;
            long totalSharedNs = 0;

            for (int r = 0; r < measuredRuns; r++) {
                // GC before measurement
                System.gc();
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}

                // Local version
                System.setOut(nullOut);
                LocalVersion local = new LocalVersion(adjustedN, threadCount, MT, MX, B, E);
                long startLocal = System.nanoTime();
                local.execute();
                long endLocal = System.nanoTime();
                System.setOut(originalOut);
                totalLocalNs += (endLocal - startLocal);

                // GC between versions
                System.gc();
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}

                // Shared version
                System.setOut(nullOut);
                SharedVersion shared = new SharedVersion(adjustedN, threadCount, MT, MX, B, E);
                long startShared = System.nanoTime();
                shared.execute();
                long endShared = System.nanoTime();
                System.setOut(originalOut);
                totalSharedNs += (endShared - startShared);

                // Validate on first run
                if (r == 0) {
                    boolean mdOk = ResultValidator.matricesEqual(
                            local.getMD(), shared.getMD(), adjustedN, Config.EPSILON);
                    boolean dOk = ResultValidator.vectorsEqual(
                            local.getD(), shared.getD(), adjustedN, Config.EPSILON);
                    ConsoleLogger.logMain("Results match: MD=" + mdOk + ", D=" + dOk);

                    FileUtils.writeResultMatrix(
                            Config.RESULTS_DIR + "MD_n" + adjustedN + ".txt", local.getMD());
                    FileUtils.writeResultVector(
                            Config.RESULTS_DIR + "D_n" + adjustedN + ".txt", local.getD());
                }
            }

            long avgLocalNs = totalLocalNs / measuredRuns;
            long avgSharedNs = totalSharedNs / measuredRuns;
            double speedup = (double) avgLocalNs / avgSharedNs;

            ConsoleLogger.logMain("N=" + adjustedN + " | Local: " + (avgLocalNs / 1_000_000)
                    + " ms | Shared: " + (avgSharedNs / 1_000_000)
                    + " ms | Speedup: " + String.format("%.2f", speedup));

            FileUtils.appendTimingCSV(Config.RESULTS_DIR + "timing.csv",
                    adjustedN, threadCount, avgLocalNs, avgSharedNs);
        }
    }
}
