package lab1;

public class KahanAccumulator {
    private double sum;
    private double compensation;

    public KahanAccumulator() {
        this.sum = 0.0;
        this.compensation = 0.0;
    }

    public KahanAccumulator(double initialValue) {
        this.sum = initialValue;
        this.compensation = 0.0;
    }

    public void add(double value) {
        double y = value - compensation;
        double t = sum + y;
        compensation = (t - sum) - y;
        sum = t;
    }

    public double getSum() {
        return sum;
    }

    public void reset() {
        sum = 0.0;
        compensation = 0.0;
    }

    public static double sumArray(double[] arr, int from, int to) {
        KahanAccumulator acc = new KahanAccumulator();
        for (int i = from; i < to; i++) {
            acc.add(arr[i]);
        }
        return acc.getSum();
    }
}
