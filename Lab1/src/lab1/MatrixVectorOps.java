package lab1;

public class MatrixVectorOps {

    public static void addMatricesRows(double[][] A, double[][] B, double[][] C,
                                       int rowStart, int rowEnd, int n) {
        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B[i][j];
            }
        }
    }

    public static void subtractMatricesRows(double[][] A, double[][] B, double[][] C,
                                             int rowStart, int rowEnd, int n) {
        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B[i][j];
            }
        }
    }

    public static void scalarMultiplyMatrixRows(double scalar, double[][] M, double[][] C,
                                                 int rowStart, int rowEnd, int n) {
        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = scalar * M[i][j];
            }
        }
    }

    public static void multiplyMatricesRows(double[][] A, double[][] B, double[][] C,
                                             int rowStart, int rowEnd, int n) {
        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = 0; j < n; j++) {
                KahanAccumulator acc = new KahanAccumulator();
                for (int k = 0; k < n; k++) {
                    acc.add(A[i][k] * B[k][j]);
                }
                C[i][j] = acc.getSum();
            }
        }
    }

    public static void multiplyMatrixVectorRows(double[][] M, double[] V, double[] result,
                                                 int rowStart, int rowEnd, int n) {
        for (int i = rowStart; i < rowEnd; i++) {
            KahanAccumulator acc = new KahanAccumulator();
            for (int j = 0; j < n; j++) {
                acc.add(M[i][j] * V[j]);
            }
            result[i] = acc.getSum();
        }
    }

    public static void subtractVectorsRange(double[] A, double[] B, double[] result,
                                             int from, int to) {
        for (int i = from; i < to; i++) {
            result[i] = A[i] - B[i];
        }
    }

    public static double findMinInRows(double[][] M, int rowStart, int rowEnd, int n) {
        double min = Double.MAX_VALUE;
        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = 0; j < n; j++) {
                if (M[i][j] < min) min = M[i][j];
            }
        }
        return min;
    }

    public static double[][] deepCopyMatrix(double[][] src) {
        int n = src.length;
        double[][] dst = new double[n][];
        for (int i = 0; i < n; i++) {
            dst[i] = new double[src[i].length];
            System.arraycopy(src[i], 0, dst[i], 0, src[i].length);
        }
        return dst;
    }

    public static double[] deepCopyVector(double[] src) {
        double[] dst = new double[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }
}
