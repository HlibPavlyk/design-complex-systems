package lab3;

import java.io.*;
import java.util.Locale;

public class FileUtils {

    public static void appendTimingCSV(String path, int threads, int iterationsPerThread, long timeNs) {
        boolean exists = new File(path).exists();
        double timeMs = timeNs / 1_000_000.0;
        int totalOps = threads * iterationsPerThread;

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path, true)))) {
            if (!exists) {
                pw.println("threads,iterations_per_thread,total_ops,time_ms");
            }
            pw.printf(Locale.US, "%d,%d,%d,%.2f%n", threads, iterationsPerThread, totalOps, timeMs);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV: " + path, e);
        }
    }
}
