package lab1;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

public class SharedWorker implements Runnable {
    private final int threadId;
    private final int n;
    private final int rowStart;
    private final int rowEnd;

    // Shared references — the SAME arrays for all threads
    private final double[][] sharedMT;
    private final double[][] sharedMX;
    private final double[] sharedB;
    private final double[] sharedE;

    // Shared result buffers
    private final double[][] sharedMD;
    private final double[] sharedD;

    private final SharedVersion parent;
    private final CyclicBarrier barrier1;
    private final CyclicBarrier barrier2;

    public SharedWorker(int threadId, int n, int rowStart, int rowEnd,
                        double[][] MT, double[][] MX, double[] B, double[] E,
                        double[][] MD, double[] D,
                        SharedVersion parent,
                        CyclicBarrier barrier1, CyclicBarrier barrier2) {
        this.threadId = threadId;
        this.n = n;
        this.rowStart = rowStart;
        this.rowEnd = rowEnd;
        this.sharedMT = MT;
        this.sharedMX = MX;
        this.sharedB = B;
        this.sharedE = E;
        this.sharedMD = MD;
        this.sharedD = D;
        this.parent = parent;
        this.barrier1 = barrier1;
        this.barrier2 = barrier2;
    }

    @Override
    public void run() {
        System.out.printf("Shared-T%d started%n", threadId);

        // Phase 1: Find min(MT) — read from SHARED sharedMT
        double localMin = Double.MAX_VALUE;
        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = 0; j < n; j++) {
                double val = sharedMT[i][j];
                if (val < localMin) localMin = val;
            }
        }

        parent.updateGlobalMin(localMin);

        try {
            barrier1.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        double globalMin = parent.getGlobalMinMT();

        try {
            barrier2.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        // Phase 2: Compute MD rows [rowStart, rowEnd)
        // MD[i][j] = globalMin * (MT[i][j] + MX[i][j]) - sum_k(MT[i][k] * MX[k][j])
        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = 0; j < n; j++) {
                double term1 = globalMin * (sharedMT[i][j] + sharedMX[i][j]);

                KahanAccumulator acc = new KahanAccumulator();
                for (int k = 0; k < n; k++) {
                    acc.add(sharedMT[i][k] * sharedMX[k][j]);
                }

                synchronized (parent.mdLock) {
                    sharedMD[i][j] = term1 - acc.getSum();
                }
            }
        }

        // Phase 3: Compute D elements [rowStart, rowEnd)
        // D[i] = dot(row_i(MT+MX), B) - dot(row_i(MT-MX), E)
        for (int i = rowStart; i < rowEnd; i++) {
            KahanAccumulator accPlus = new KahanAccumulator();
            for (int j = 0; j < n; j++) {
                accPlus.add((sharedMT[i][j] + sharedMX[i][j]) * sharedB[j]);
            }

            KahanAccumulator accMinus = new KahanAccumulator();
            for (int j = 0; j < n; j++) {
                accMinus.add((sharedMT[i][j] - sharedMX[i][j]) * sharedE[j]);
            }

            synchronized (parent.dLock) {
                sharedD[i] = accPlus.getSum() - accMinus.getSum();
            }
        }

        System.out.printf("Shared-T%d finished%n", threadId);
    }
}
