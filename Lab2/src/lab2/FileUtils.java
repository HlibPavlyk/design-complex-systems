package lab2;

import java.io.*;
import java.util.Locale;

public class FileUtils {

    public static void writeMatrix(String path, double[][] M) {
        ensureParentDir(path);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path)))) {
            int n = M.length;
            pw.println(n);
            for (int i = 0; i < n; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < M[i].length; j++) {
                    if (j > 0) sb.append(" ");
                    sb.append(String.format(Locale.US, "%.17g", M[i][j]));
                }
                pw.println(sb.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write matrix to " + path, e);
        }
    }

    public static double[][] readMatrix(String path, int n) {
        double[][] M = new double[n][n];
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            int size = Integer.parseInt(br.readLine().trim());
            if (size != n) {
                throw new RuntimeException("Matrix size mismatch: expected " + n + ", got " + size);
            }
            for (int i = 0; i < n; i++) {
                String[] parts = br.readLine().trim().split("\\s+");
                for (int j = 0; j < n; j++) {
                    M[i][j] = Double.parseDouble(parts[j]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read matrix from " + path, e);
        }
        return M;
    }

    public static void writeVector(String path, double[] V) {
        ensureParentDir(path);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path)))) {
            pw.println(V.length);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < V.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(String.format(Locale.US, "%.17g", V[i]));
            }
            pw.println(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write vector to " + path, e);
        }
    }

    public static double[] readVector(String path, int n) {
        double[] V = new double[n];
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            int size = Integer.parseInt(br.readLine().trim());
            if (size != n) {
                throw new RuntimeException("Vector size mismatch: expected " + n + ", got " + size);
            }
            String[] parts = br.readLine().trim().split("\\s+");
            for (int i = 0; i < n; i++) {
                V[i] = Double.parseDouble(parts[i]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read vector from " + path, e);
        }
        return V;
    }

    public static void writeResultMatrix(String path, double[][] MD) {
        writeMatrix(path, MD);
    }

    public static void writeResultVector(String path, double[] D) {
        writeVector(path, D);
    }

    public static void appendLockTimingCSV(String path, int n, int p, long lockTimeNs) {
        ensureParentDir(path);
        boolean fileExists = new File(path).exists();
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path, true)))) {
            if (!fileExists) {
                pw.println("n,p,lock_ms");
            }
            pw.printf(Locale.US, "%d,%d,%.2f%n", n, p, lockTimeNs / 1_000_000.0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to CSV " + path, e);
        }
    }

    private static void ensureParentDir(String path) {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
