import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Q6 Alternative Concepts:
 * 1. ArrayBlockingQueue<Integer>(5) replacing manual wait()/notifyAll()
 * 2. ReentrantLock.tryLock(timeout) with backoff replacing strict lock ordering
 */
public class AlternativesQ6 {

    static class TryLockAccount {
        final String name;
        double balance;
        final ReentrantLock lock = new ReentrantLock();

        TryLockAccount(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }
    }

    public static boolean transferWithTryLock(TryLockAccount from, TryLockAccount to, double amt)
            throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (from.balance >= amt) {
                                from.balance -= amt;
                                to.balance += amt;
                            }
                            return true;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }
            Thread.sleep((long) (Math.random() * 5)); // Randomized backoff prevents livelock
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        // 1. ArrayBlockingQueue Producer-Consumer
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);
        int[] sum = new int[1];

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) queue.put(i);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) sum[0] += queue.take();
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        producer.start(); consumer.start();
        producer.join(); consumer.join();
        System.out.println("ArrayBlockingQueue token sum: " + sum[0] + " (expected 210)");

        // 2. tryLock Transfer
        TryLockAccount a = new TryLockAccount("A", 1000);
        TryLockAccount b = new TryLockAccount("B", 1000);
        Thread t1 = new Thread(() -> {
            try { transferWithTryLock(a, b, 250); } catch (InterruptedException ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try { transferWithTryLock(b, a, 100); } catch (InterruptedException ignored) {}
        });
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.printf("tryLock Balances -> A: %.2f, B: %.2f%n", a.balance, b.balance);
    }
}
