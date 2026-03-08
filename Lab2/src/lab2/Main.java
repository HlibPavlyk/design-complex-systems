package lab2;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        int n = Config.DEFAULT_N;
        int p = Config.DEFAULT_P;
        boolean benchmarkMode = false;
        boolean reportMode = false;
        int warmupRuns = 2;
        int measuredRuns = 3;
        String label = "TSX-Enabled";

        for (String arg : args) {
            if (arg.startsWith("--n=")) n = Integer.parseInt(arg.substring(4));
            else if (arg.startsWith("--p=")) p = Integer.parseInt(arg.substring(4));
            else if (arg.equals("--benchmark")) benchmarkMode = true;
            else if (arg.equals("--report")) reportMode = true;
            else if (arg.startsWith("--warmup=")) warmupRuns = Integer.parseInt(arg.substring(9));
            else if (arg.startsWith("--runs=")) measuredRuns = Integer.parseInt(arg.substring(7));
            else if (arg.startsWith("--label=")) label = arg.substring(8);
        }

        new File(Config.DATA_DIR).mkdirs();
        new File(Config.RESULTS_DIR).mkdirs();

        if (reportMode) {
            String enabledCsv = Config.RESULTS_DIR + "timing_TSX-Enabled.csv";
            String disabledCsv = Config.RESULTS_DIR + "timing_TSX-Disabled.csv";
            String outputHtml = Config.RESULTS_DIR + "chart.html";

            if (!new File(enabledCsv).exists()) {
                ConsoleLogger.logMain("Missing " + enabledCsv + ". Run benchmark with --label=TSX-Enabled first.");
                return;
            }
            if (!new File(disabledCsv).exists()) {
                ConsoleLogger.logMain("Missing " + disabledCsv + ". Run benchmark with --label=TSX-Disabled first.");
                return;
            }

            GraphPlotter.generateReport(enabledCsv, disabledCsv, outputHtml);
            ConsoleLogger.logMain("Report generated. Open " + outputHtml + " in a browser.");

        } else if (benchmarkMode) {
            BenchmarkRunner runner = new BenchmarkRunner(
                    Config.BENCHMARK_SIZES, p, warmupRuns, measuredRuns, label);
            runner.runAll();

            ConsoleLogger.logMain("Benchmark [" + label + "] complete.");
            ConsoleLogger.logMain("To generate comparison report, run both benchmarks then use --report:");
            ConsoleLogger.logMain("  1. java -cp out lab2.Main --benchmark --label=TSX-Enabled");
            ConsoleLogger.logMain("  2. Disable TSX-NI, reboot");
            ConsoleLogger.logMain("  3. java -cp out lab2.Main --benchmark --label=TSX-Disabled");
            ConsoleLogger.logMain("  4. java -cp out lab2.Main --report");

        } else {
            // Single run mode — load data from Lab1
            if (n % p != 0) {
                n = ((n / p) + 1) * p;
                ConsoleLogger.logMain("Adjusted N to " + n + " (must be divisible by P=" + p + ")");
            }

            ConsoleLogger.logMain("Single run: N=" + n + ", P=" + p + " [" + label + "]");

            double[][] MT, MX;
            double[] B, E;

            // Try loading from Lab1 data first
            String lab1Mt = Config.LAB1_DATA_DIR + "MT.txt";
            if (new File(lab1Mt).exists()) {
                ConsoleLogger.logMain("Loading data from Lab1: " + Config.LAB1_DATA_DIR);
                try {
                    Object[] loaded = DataGenerator.loadFromLab1(n);
                    MT = (double[][]) loaded[0];
                    MX = (double[][]) loaded[1];
                    B = (double[]) loaded[2];
                    E = (double[]) loaded[3];
                } catch (Exception e) {
                    ConsoleLogger.logMain("Lab1 data incompatible (different N?). Generating fresh data with same seed.");
                    DataGenerator gen = new DataGenerator(n);
                    MT = gen.generateMatrix();
                    MX = gen.generateMatrix();
                    B = gen.generateVector();
                    E = gen.generateVector();
                    gen.saveAll(MT, MX, B, E);
                    Object[] loaded = DataGenerator.loadAll(n);
                    MT = (double[][]) loaded[0];
                    MX = (double[][]) loaded[1];
                    B = (double[]) loaded[2];
                    E = (double[]) loaded[3];
                }
            } else {
                ConsoleLogger.logMain("Lab1 data not found. Generating fresh data with same seed.");
                DataGenerator gen = new DataGenerator(n);
                MT = gen.generateMatrix();
                MX = gen.generateMatrix();
                B = gen.generateVector();
                E = gen.generateVector();
                gen.saveAll(MT, MX, B, E);
                Object[] loaded = DataGenerator.loadAll(n);
                MT = (double[][]) loaded[0];
                MX = (double[][]) loaded[1];
                B = (double[]) loaded[2];
                E = (double[]) loaded[3];
            }

            // Run Lock-based version
            ConsoleLogger.logMain("=== LOCK VERSION (ReentrantLock) ===");
            LockVersion lock = new LockVersion(n, p, MT, MX, B, E);
            long startLock = System.nanoTime();
            lock.execute();
            long lockTime = System.nanoTime() - startLock;

            ConsoleLogger.logMain("Lock time: " + (lockTime / 1_000_000) + " ms");

            // Print first elements for verification (teacher's requirement)
            printFirstElements(lock.getMD(), lock.getD(), n, "Lab2 Lock");

            // Validate against Lab1 results if available
            validateAgainstLab1(lock.getMD(), lock.getD(), n);

            // Save results
            FileUtils.writeResultMatrix(Config.RESULTS_DIR + "MD.txt", lock.getMD());
            FileUtils.writeResultVector(Config.RESULTS_DIR + "D.txt", lock.getD());
            ConsoleLogger.logMain("Results saved to " + Config.RESULTS_DIR);
        }
    }

    private static void printFirstElements(double[][] MD, double[] D, int n, String label) {
        int show = Math.min(5, n);
        ConsoleLogger.logMain("--- " + label + " results (first elements) ---");

        StringBuilder sb = new StringBuilder();
        sb.append("MD[0][0..").append(show - 1).append("] = ");
        for (int j = 0; j < show; j++) {
            if (j > 0) sb.append(", ");
            sb.append(String.format("%.10f", MD[0][j]));
        }
        ConsoleLogger.logMain(sb.toString());

        sb = new StringBuilder();
        sb.append("MD[1][0..").append(show - 1).append("] = ");
        for (int j = 0; j < show; j++) {
            if (j > 0) sb.append(", ");
            sb.append(String.format("%.10f", MD[1][j]));
        }
        ConsoleLogger.logMain(sb.toString());

        sb = new StringBuilder();
        sb.append("D[0..").append(show - 1).append("] = ");
        for (int i = 0; i < show; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.10f", D[i]));
        }
        ConsoleLogger.logMain(sb.toString());
    }

    private static void validateAgainstLab1(double[][] md, double[] d, int n) {
        String lab1MdPath = Config.LAB1_RESULTS_DIR + "MD.txt";
        String lab1DPath = Config.LAB1_RESULTS_DIR + "D.txt";

        if (!new java.io.File(lab1MdPath).exists() || !new java.io.File(lab1DPath).exists()) {
            ConsoleLogger.logMain("Lab1 results not found at " + Config.LAB1_RESULTS_DIR
                    + " — skipping cross-lab validation.");
            return;
        }

        try {
            double[][] lab1MD = FileUtils.readMatrix(lab1MdPath, n);
            double[] lab1D = FileUtils.readVector(lab1DPath, n);

            boolean mdOk = ResultValidator.matricesEqual(md, lab1MD, n, Config.EPSILON);
            boolean dOk = ResultValidator.vectorsEqual(d, lab1D, n, Config.EPSILON);

            ConsoleLogger.logMain("Cross-validation with Lab1: MD=" + mdOk + ", D=" + dOk);

            if (mdOk && dOk) {
                ConsoleLogger.logMain("Lab2 results MATCH Lab1 results.");
            } else {
                ConsoleLogger.logMain("WARNING: Lab2 results DIFFER from Lab1 results!");
            }

            // Print Lab1 first elements for side-by-side comparison
            printFirstElements(lab1MD, lab1D, n, "Lab1 Shared");
        } catch (Exception e) {
            ConsoleLogger.logMain("Could not validate against Lab1: " + e.getMessage());
        }
    }
}
