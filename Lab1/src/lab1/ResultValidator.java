package lab1;

public class ResultValidator {

    public static boolean matricesEqual(double[][] A, double[][] B, int n, double epsilon) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (Math.abs(A[i][j] - B[i][j]) > epsilon) {
                    System.err.println("Matrix mismatch at [" + i + "][" + j + "]: "
                            + A[i][j] + " vs " + B[i][j]);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean vectorsEqual(double[] A, double[] B, int n, double epsilon) {
        for (int i = 0; i < n; i++) {
            if (Math.abs(A[i] - B[i]) > epsilon) {
                System.err.println("Vector mismatch at [" + i + "]: "
                        + A[i] + " vs " + B[i]);
                return false;
            }
        }
        return true;
    }
}
