package lab1;

import java.util.concurrent.CyclicBarrier;

public class SharedVersion {
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
    private final Object minLock = new Object();

    // Separate lock objects for shared result writes (like reference)
    public final Object mdLock = new Object();
    public final Object dLock = new Object();

    public SharedVersion(int n, int p, double[][] MT, double[][] MX, double[] B, double[] E) {
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
            int rowStart = t * H;
            int rowEnd = (t == p - 1) ? n : (t + 1) * H;

            threads[t] = new Thread(new SharedWorker(
                    t, n, rowStart, rowEnd,
                    MT, MX, B, E,
                    MD, D,
                    this, barrier1, barrier2
            ), "Shared-T" + t);
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

    public void updateGlobalMin(double localMin) {
        synchronized (minLock) {
            if (localMin < globalMinMT) {
                globalMinMT = localMin;
            }
        }
    }

    public double getGlobalMinMT() {
        return globalMinMT;
    }

    public double[][] getMD() {
        return MD;
    }

    public double[] getD() {
        return D;
    }
}
