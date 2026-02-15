package lab1;

public class Main {
    public static void main(String[] args) {
        int n = Config.DEFAULT_N;
        int p = Config.DEFAULT_P;
        boolean benchmarkMode = false;
        int warmupRuns = 2;
        int measuredRuns = 3;

        for (String arg : args) {
            if (arg.startsWith("--n=")) n = Integer.parseInt(arg.substring(4));
            else if (arg.startsWith("--p=")) p = Integer.parseInt(arg.substring(4));
            else if (arg.equals("--benchmark")) benchmarkMode = true;
            else if (arg.startsWith("--warmup=")) warmupRuns = Integer.parseInt(arg.substring(9));
            else if (arg.startsWith("--runs=")) measuredRuns = Integer.parseInt(arg.substring(7));
        }

        if (n % p != 0) {
            n = ((n / p) + 1) * p;
            ConsoleLogger.logMain("Adjusted N to " + n + " (must be divisible by P=" + p + ")");
        }

        new java.io.File(Config.DATA_DIR).mkdirs();
        new java.io.File(Config.RESULTS_DIR).mkdirs();

        if (benchmarkMode) {
            BenchmarkRunner runner = new BenchmarkRunner(
                    Config.BENCHMARK_SIZES, p, warmupRuns, measuredRuns);
            runner.runAll();

            GraphPlotter.generateReport(
                    Config.RESULTS_DIR + "timing.csv",
                    Config.RESULTS_DIR + "chart.html");

            ConsoleLogger.logMain("Benchmark complete. See results/ directory.");
        } else {
            ConsoleLogger.logMain("Single run: N=" + n + ", P=" + p);

            DataGenerator gen = new DataGenerator(n);
            double[][] MT = gen.generateMatrix();
            double[][] MX = gen.generateMatrix();
            double[] B = gen.generateVector();
            double[] E = gen.generateVector();

            gen.saveAll(MT, MX, B, E);
            Object[] loaded = DataGenerator.loadAll(n);
            MT = (double[][]) loaded[0];
            MX = (double[][]) loaded[1];
            B = (double[]) loaded[2];
            E = (double[]) loaded[3];

            // Local version (deep copies included in timing)
            ConsoleLogger.logMain("=== LOCAL VERSION ===");
            LocalVersion local = new LocalVersion(n, p, MT, MX, B, E);
            long startLocal = System.nanoTime();
            local.execute();
            long localTime = System.nanoTime() - startLocal;

            // Shared version
            ConsoleLogger.logMain("=== SHARED VERSION ===");
            SharedVersion shared = new SharedVersion(n, p, MT, MX, B, E);
            long startShared = System.nanoTime();
            shared.execute();
            long sharedTime = System.nanoTime() - startShared;

            boolean mdOk = ResultValidator.matricesEqual(
                    local.getMD(), shared.getMD(), n, Config.EPSILON);
            boolean dOk = ResultValidator.vectorsEqual(
                    local.getD(), shared.getD(), n, Config.EPSILON);

            ConsoleLogger.logMain("Results match: MD=" + mdOk + ", D=" + dOk);
            ConsoleLogger.logMain("Local time:  " + (localTime / 1_000_000) + " ms");
            ConsoleLogger.logMain("Shared time: " + (sharedTime / 1_000_000) + " ms");
            ConsoleLogger.logMain("Speedup (Local/Shared): " +
                    String.format("%.2f", (double) localTime / sharedTime));

            FileUtils.writeResultMatrix(Config.RESULTS_DIR + "MD.txt", local.getMD());
            FileUtils.writeResultVector(Config.RESULTS_DIR + "D.txt", local.getD());
            ConsoleLogger.logMain("Results saved to " + Config.RESULTS_DIR);
        }
    }
}
