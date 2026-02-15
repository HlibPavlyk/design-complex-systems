package lab1;

import java.util.concurrent.CyclicBarrier;

public class LocalVersion {
    private final int n;
    private final int p;
    private final double[][] MT, MX;
    private final double[] B, E;
    private final double[][] MD;
    private final double[] D;

    public LocalVersion(int n, int p, double[][] MT, double[][] MX, double[] B, double[] E) {
        this.n = n;
        this.p = p;
        this.MT = MT;
        this.MX = MX;
        this.B = B;
        this.E = E;
        this.MD = new double[n][n];
        this.D = new double[n];
    }

    public void execute() {
        int H = n / p;
        Thread[] threads = new Thread[p];
        double[] localMins = new double[p];
        CyclicBarrier barrier = new CyclicBarrier(p);

        // Deep copies happen in LocalWorker constructors (timed, like reference)
        for (int t = 0; t < p; t++) {
            int rowStart = t * H;
            int rowEnd = (t == p - 1) ? n : (t + 1) * H;

            threads[t] = new Thread(new LocalWorker(
                    t, n, rowStart, rowEnd,
                    MT, MX, B, E,
                    MD, D,
                    localMins, barrier
            ), "Local-T" + t);
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
