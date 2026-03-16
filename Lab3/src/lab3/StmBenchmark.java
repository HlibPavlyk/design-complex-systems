package lab3;

import org.multiverse.api.StmUtils;
import org.multiverse.api.references.TxnDouble;
import org.multiverse.api.references.TxnInteger;

import java.sql.*;
import java.util.concurrent.*;

/**
 * STM (Software Transactional Memory) benchmark.
 *
 * Uses Multiverse transactional references (TxnDouble, TxnInteger) to manage
 * shared state between threads via software transactional memory. Each DB operation
 * is wrapped in StmUtils.atomic() along with reads/writes to transactional variables,
 * creating real STM overhead: versioning, conflict detection, validation, and
 * potential rollback/retry — all managed in software.
 *
 * This mode should be run with Intel TSX-NI disabled to measure pure STM overhead.
 */
public class StmBenchmark {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int threadCount;
    private final int iterationsPerThread;
    private final int recordCount;

    public StmBenchmark(String dbUrl, String dbUser, String dbPassword,
                         int threadCount, int iterationsPerThread, int recordCount) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.threadCount = threadCount;
        this.iterationsPerThread = iterationsPerThread;
        this.recordCount = recordCount;
    }

    public long execute() {
        // Shared transactional state — all threads read/write these via STM
        final TxnInteger totalOpsCounter = StmUtils.newTxnInteger(0);
        final TxnDouble accumulatedValue = StmUtils.newTxnDouble(0.0);

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
                            final double increment = threadId * 0.1 + i * 0.01;
                            final int id = targetId;

                            StmUtils.atomic(new Runnable() {
                                @Override
                                public void run() {
                                    // STM transactional operations — read and update shared state
                                    int ops = totalOpsCounter.get();
                                    totalOpsCounter.set(ops + 1);

                                    double acc = accumulatedValue.get();
                                    accumulatedValue.set(acc + increment);

                                    // DB operation within the same atomic block
                                    try {
                                        updateStmt.setDouble(1, increment);
                                        updateStmt.setInt(2, id);
                                        updateStmt.executeUpdate();
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
                        }

                    } catch (SQLException e) {
                        ConsoleLogger.log("STM-T" + threadId, "Error: " + e.getMessage());
                    }

                    ConsoleLogger.log("STM-T" + threadId,
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
