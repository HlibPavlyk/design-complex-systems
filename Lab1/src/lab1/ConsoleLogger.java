package lab1;

public class ConsoleLogger {
    private static final Object lock = new Object();

    public static void log(String threadName, String message) {
        synchronized (lock) {
            System.out.println("[" + threadName + "] " + message);
            System.out.flush();
        }
    }

    public static void logMain(String message) {
        synchronized (lock) {
            System.out.println("[Main] " + message);
            System.out.flush();
        }
    }
}
