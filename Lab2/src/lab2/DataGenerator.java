package lab2;

import java.util.Random;

public class DataGenerator {
    private final int n;
    private final Random random;

    public DataGenerator(int n) {
        this.n = n;
        this.random = new Random(42);
    }

    private double generatePositiveValue() {
        int exponent = random.nextInt(9) - 3; // range [-3, 5]
        double mantissa = 1.0 + random.nextDouble() * 9.0; // [1.0, 10.0)
        return mantissa * Math.pow(10.0, exponent);
    }

    public double[][] generateMatrix() {
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                M[i][j] = generatePositiveValue();
            }
        }
        return M;
    }

    public double[] generateVector() {
        double[] V = new double[n];
        for (int i = 0; i < n; i++) {
            V[i] = generatePositiveValue();
        }
        return V;
    }

    public void saveAll(double[][] MT, double[][] MX, double[] B, double[] E) {
        FileUtils.writeMatrix(Config.MT_FILE, MT);
        FileUtils.writeMatrix(Config.MX_FILE, MX);
        FileUtils.writeVector(Config.B_FILE, B);
        FileUtils.writeVector(Config.E_FILE, E);
    }

    public static Object[] loadAll(int n) {
        return loadAll(n, Config.MT_FILE, Config.MX_FILE, Config.B_FILE, Config.E_FILE);
    }

    public static Object[] loadFromLab1(int n) {
        return loadAll(n,
                Config.LAB1_DATA_DIR + "MT.txt",
                Config.LAB1_DATA_DIR + "MX.txt",
                Config.LAB1_DATA_DIR + "B.txt",
                Config.LAB1_DATA_DIR + "E.txt");
    }

    private static Object[] loadAll(int n, String mtPath, String mxPath, String bPath, String ePath) {
        double[][] MT = FileUtils.readMatrix(mtPath, n);
        double[][] MX = FileUtils.readMatrix(mxPath, n);
        double[] B = FileUtils.readVector(bPath, n);
        double[] E = FileUtils.readVector(ePath, n);
        return new Object[]{MT, MX, B, E};
    }
}
