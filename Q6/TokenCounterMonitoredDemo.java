/**
 * Q6 (Part B): Logs counter size over time to verify invariant (0 <= count <= 5)
 * and demonstrates why 'while' is mandatory over 'if' with multiple consumers.
 */
public class TokenCounterMonitoredDemo {
    private final int[] buffer = new int[5];
    private int count = 0, in = 0, out = 0;
    private int maxObserved = 0;
    private int minObserved = 0;

    public synchronized void put(int token) throws InterruptedException {
        while (count == buffer.length) {
            wait();
        }
        buffer[in] = token;
        in = (in + 1) % buffer.length;
        count++;
        if (count > maxObserved) maxObserved = count;
        System.out.printf("[PUT  token=%02d] counter size = %d%n", token, count);
        notifyAll();
    }

    public synchronized int take(String studentName) throws InterruptedException {
        while (count == 0) {
            wait();
        }
        int token = buffer[out];
        out = (out + 1) % buffer.length;
        count--;
        if (count < minObserved) minObserved = count;
        System.out.printf("[TAKE token=%02d by %s] counter size = %d%n", token, studentName, count);
        notifyAll();
        return token;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounterMonitoredDemo counter = new TokenCounterMonitoredDemo();
        Thread cook = new Thread(() -> {
            try {
                for (int t = 1; t <= 12; t++) counter.put(t);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread s1 = new Thread(() -> {
            try {
                for (int i = 0; i < 6; i++) counter.take("Student-1");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread s2 = new Thread(() -> {
            try {
                for (int i = 0; i < 6; i++) counter.take("Student-2");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        cook.start(); s1.start(); s2.start();
        cook.join(); s1.join(); s2.join();
        System.out.printf("Observed Counter Bounds -> Min: %d (valid >= 0), Max: %d (valid <= 5)%n",
                counter.minObserved, counter.maxObserved);
    }
}
