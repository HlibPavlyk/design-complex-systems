package lab2;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockVersion {
    private final int n;
    private final int p;

    // Shared global data — all threads read these directly
    private final double[][] MT, MX;
    private final double[] B, E;

    // Shared result buffers
    private final double[][] MD;
    private final double[] D;

    // Shared synchronization state for min(MT)
    private volatile double globalMinMT;
    private final Lock minLock = new ReentrantLock();

    // Single fine-grained lock for element writes (enables TSX-NI hardware lock elision)
    private final Lock writeLock = new ReentrantLock();

    public LockVersion(int n, int p, double[][] MT, double[][] MX, double[] B, double[] E) {
        this.n = n;
        this.p = p;
        this.MT = MT;
        this.MX = MX;
        this.B = B;
        this.E = E;
        this.MD = new double[n][n];
        this.D = new double[n];
        this.globalMinMT = Double.MAX_VALUE;
    }

    public void execute() {
        globalMinMT = Double.MAX_VALUE;
        int H = n / p;
        Thread[] threads = new Thread[p];

        CyclicBarrier barrier1 = new CyclicBarrier(p);
        CyclicBarrier barrier2 = new CyclicBarrier(p);

        for (int t = 0; t < p; t++) {
            final int threadId = t;
            final int rowStart = t * H;
            final int rowEnd = (t == p - 1) ? n : (t + 1) * H;

            // Anonymous Runnable (java.util.concurrent synchronization)
            threads[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    ConsoleLogger.log("Lock-T" + threadId, "started");

                    // Phase 1: Find min(MT) — read from shared MT
                    double localMin = Double.MAX_VALUE;
                    for (int i = rowStart; i < rowEnd; i++) {
                        for (int j = 0; j < n; j++) {
                            double val = MT[i][j];
                            if (val < localMin) localMin = val;
                        }
                    }

                    minLock.lock();
                    try {
                        if (localMin < globalMinMT) {
                            globalMinMT = localMin;
                        }
                    } finally {
                        minLock.unlock();
                    }

                    try {
                        barrier1.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        throw new RuntimeException(e);
                    }

                    double globalMin = globalMinMT;

                    try {
                        barrier2.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        throw new RuntimeException(e);
                    }

                    // Phase 2: Compute MD rows [rowStart, rowEnd)
                    // MD[i][j] = globalMin * (MT[i][j] + MX[i][j]) - sum_k(MT[i][k] * MX[k][j])
                    for (int i = rowStart; i < rowEnd; i++) {
                        for (int j = 0; j < n; j++) {
                            double term1 = globalMin * (MT[i][j] + MX[i][j]);

                            KahanAccumulator acc = new KahanAccumulator();
                            for (int k = 0; k < n; k++) {
                                acc.add(MT[i][k] * MX[k][j]);
                            }

                            double val = term1 - acc.getSum();

                            writeLock.lock();
                            try {
                                MD[i][j] = val;
                            } finally {
                                writeLock.unlock();
                            }
                        }
                    }

                    // Phase 3: Compute D elements [rowStart, rowEnd)
                    // D[i] = dot(row_i(MT+MX), B) - dot(row_i(MT-MX), E)
                    for (int i = rowStart; i < rowEnd; i++) {
                        KahanAccumulator accPlus = new KahanAccumulator();
                        for (int j = 0; j < n; j++) {
                            accPlus.add((MT[i][j] + MX[i][j]) * B[j]);
                        }

                        KahanAccumulator accMinus = new KahanAccumulator();
                        for (int j = 0; j < n; j++) {
                            accMinus.add((MT[i][j] - MX[i][j]) * E[j]);
                        }

                        double val = accPlus.getSum() - accMinus.getSum();

                        writeLock.lock();
                        try {
                            D[i] = val;
                        } finally {
                            writeLock.unlock();
                        }
                    }

                    ConsoleLogger.log("Lock-T" + threadId, "finished");
                }
            }, "Lock-T" + t);
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public double[][] getMD() {
        return MD;
    }

    public double[] getD() {
        return D;
    }
}
