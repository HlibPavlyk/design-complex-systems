package lab1;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

public class LocalWorker implements Runnable {
    private final int threadId;
    private final int n;
    private final int rowStart;
    private final int rowEnd;

    // Thread-local copies (deep copied in constructor)
    private final double[][] localMT;
    private final double[][] localMX;
    private final double[] localB;
    private final double[] localE;

    private final double[][] resultMD;
    private final double[] resultD;

    private final double[] localMins;
    private final CyclicBarrier barrier;

    public LocalWorker(int threadId, int n, int rowStart, int rowEnd,
                       double[][] origMT, double[][] origMX, double[] origB, double[] origE,
                       double[][] MD, double[] D,
                       double[] localMins, CyclicBarrier barrier) {
        this.threadId = threadId;
        this.n = n;
        this.rowStart = rowStart;
        this.rowEnd = rowEnd;

        // Deep copy ALL data into thread-local arrays (like reference)
        this.localMT = MatrixVectorOps.deepCopyMatrix(origMT);
        this.localMX = MatrixVectorOps.deepCopyMatrix(origMX);
        this.localB = MatrixVectorOps.deepCopyVector(origB);
        this.localE = MatrixVectorOps.deepCopyVector(origE);

        this.resultMD = MD;
        this.resultD = D;
        this.localMins = localMins;
        this.barrier = barrier;
    }

    @Override
    public void run() {
        System.out.printf("Local-T%d started%n", threadId);

        // Phase 1: Find local min(MT) over assigned rows
        double myMin = MatrixVectorOps.findMinInRows(localMT, rowStart, rowEnd, n);
        localMins[threadId] = myMin;

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        double globalMin = Double.MAX_VALUE;
        for (int i = 0; i < localMins.length; i++) {
            if (localMins[i] < globalMin) globalMin = localMins[i];
        }

        // Phase 2: Compute MD rows [rowStart, rowEnd)
        // MD = min(MT) * (MT + MX) - MT * MX
        int numRows = rowEnd - rowStart;

        double[][] sumMTMX = new double[numRows][n];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < n; j++) {
                sumMTMX[i][j] = localMT[rowStart + i][j] + localMX[rowStart + i][j];
            }
        }

        double[][] scaledSum = new double[numRows][n];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < n; j++) {
                scaledSum[i][j] = globalMin * sumMTMX[i][j];
            }
        }

        double[][] prodMTMX = new double[numRows][n];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < n; j++) {
                KahanAccumulator acc = new KahanAccumulator();
                for (int k = 0; k < n; k++) {
                    acc.add(localMT[rowStart + i][k] * localMX[k][j]);
                }
                prodMTMX[i][j] = acc.getSum();
            }
        }

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < n; j++) {
                resultMD[rowStart + i][j] = scaledSum[i][j] - prodMTMX[i][j];
            }
        }

        // Phase 3: Compute D elements [rowStart, rowEnd)
        // D = (MT + MX) * B - (MT - MX) * E
        for (int i = 0; i < numRows; i++) {
            int globalI = rowStart + i;

            KahanAccumulator accPlus = new KahanAccumulator();
            for (int j = 0; j < n; j++) {
                accPlus.add(sumMTMX[i][j] * localB[j]);
            }

            KahanAccumulator accMinus = new KahanAccumulator();
            for (int j = 0; j < n; j++) {
                double diff = localMT[globalI][j] - localMX[globalI][j];
                accMinus.add(diff * localE[j]);
            }

            resultD[globalI] = accPlus.getSum() - accMinus.getSum();
        }

        System.out.printf("Local-T%d finished%n", threadId);
    }
}
