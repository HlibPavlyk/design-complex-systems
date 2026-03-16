package lab3;

import java.sql.*;
import java.util.concurrent.*;

/**
 * HTM (Hardware Transactional Memory) benchmark.
 *
 * Threads execute JDBC operations in parallel without explicit Java-level locks.
 * When Intel TSX-NI is enabled, the CPU's hardware transactional memory manages
 * concurrent access at the hardware level — internal JVM and database locking
 * benefit from Hardware Lock Elision (HLE), reducing synchronization overhead.
 *
 * This is the "clean" parallel execution that benefits from TSX-NI.
 */
public class HtmBenchmark {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int threadCount;
    private final int iterationsPerThread;
    private final int recordCount;

    public HtmBenchmark(String dbUrl, String dbUser, String dbPassword,
                         int threadCount, int iterationsPerThread, int recordCount) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.threadCount = threadCount;
        this.iterationsPerThread = iterationsPerThread;
        this.recordCount = recordCount;
    }

    public long execute() {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                         PreparedStatement updateStmt = conn.prepareStatement(
                                 "UPDATE benchmark_data SET value = value * 1.01 + ? WHERE id = ?")) {

                        for (int i = 0; i < iterationsPerThread; i++) {
                            int targetId = (i % recordCount) + 1;
                            updateStmt.setDouble(1, threadId * 0.1 + i * 0.01);
                            updateStmt.setInt(2, targetId);
                            updateStmt.executeUpdate();
                        }

                    } catch (SQLException e) {
                        ConsoleLogger.log("HTM-T" + threadId, "Error: " + e.getMessage());
                    }

                    ConsoleLogger.log("HTM-T" + threadId,
                            "Completed " + iterationsPerThread + " operations");
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return System.nanoTime() - startTime;
    }
}
