import java.util.concurrent.atomic.AtomicInteger;

/**
 * Q5: Reproduces double-booking race conditions without synchronization across 10 runs,
 * and benchmarks synchronized method vs fine-grained synchronized block on 10,000 bookings.
 */
public class SeatBookingExperiments {

    // 1. Unsynchronized implementation (exhibits check-then-act race condition)
    static class UnsynchronizedScreen {
        private final boolean[] booked;
        private int bookingsMade = 0;

        UnsynchronizedScreen(int seats) { this.booked = new boolean[seats]; }

        public boolean book(int seat) {
            if (!booked[seat]) {
                // Yield increases thread interleaving probability inside the critical section
                Thread.yield();
                booked[seat] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    // 2. Synchronized method implementation (locks on 'this' instance)
    static class SyncMethodScreen {
        private final boolean[] booked;
        private int bookingsMade = 0;

        SyncMethodScreen(int seats) { this.booked = new boolean[seats]; }

        public synchronized boolean book(int seat) {
            if (!booked[seat]) {
                booked[seat] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    // 3. Synchronized block implementation (fine-grained per-seat lock objects)
    static class SyncBlockScreen {
        private final boolean[] booked;
        private final Object[] seatLocks;
        private final AtomicInteger bookingsMade = new AtomicInteger(0);

        SyncBlockScreen(int seats) {
            this.booked = new boolean[seats];
            this.seatLocks = new Object[seats];
            for (int i = 0; i < seats; i++) seatLocks[i] = new Object();
        }

        public boolean book(int seat) {
            // Non-critical validation can happen outside lock
            if (seat < 0 || seat >= booked.length) return false;
            synchronized (seatLocks[seat]) {
                if (!booked[seat]) {
                    booked[seat] = true;
                    bookingsMade.incrementAndGet();
                    return true;
                }
                return false;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part A: Reproducing Double Booking Over 10 Unsynchronized Runs (10 Seats, 100 Threads) ===");
        for (int run = 1; run <= 10; run++) {
            UnsynchronizedScreen screen = new UnsynchronizedScreen(10);
            Thread[] users = new Thread[100];
            for (int i = 0; i < 100; i++) {
                final int seat = i % 10;
                users[i] = new Thread(() -> screen.book(seat));
                users[i].start();
            }
            for (Thread t : users) t.join();
            System.out.printf("Run %02d -> Seats sold: %d (Double-bookings: %d)%n",
                    run, screen.bookingsMade, Math.max(0, screen.bookingsMade - 10));
        }

        System.out.println("\n=== Part B: Benchmark on 10,000 Concurrent Booking Requests (1,000 Seats) ===");
        int totalRequests = 10_000;
        int totalSeats = 1_000;

        // Benchmark Synchronized Method
        SyncMethodScreen methodScreen = new SyncMethodScreen(totalSeats);
        Thread[] t1 = new Thread[totalRequests];
        long startMethod = System.nanoTime();
        for (int i = 0; i < totalRequests; i++) {
            final int seat = i % totalSeats;
            t1[i] = new Thread(() -> methodScreen.book(seat));
            t1[i].start();
        }
        for (Thread t : t1) t.join();
        long elapsedMethodMs = (System.nanoTime() - startMethod) / 1_000_000L;

        // Benchmark Fine-Grained Synchronized Block
        SyncBlockScreen blockScreen = new SyncBlockScreen(totalSeats);
        Thread[] t2 = new Thread[totalRequests];
        long startBlock = System.nanoTime();
        for (int i = 0; i < totalRequests; i++) {
            final int seat = i % totalSeats;
            t2[i] = new Thread(() -> blockScreen.book(seat));
            t2[i].start();
        }
        for (Thread t : t2) t.join();
        long elapsedBlockMs = (System.nanoTime() - startBlock) / 1_000_000L;

        System.out.printf("Synchronized Method -> Sold: %d | Time: %d ms%n", methodScreen.bookingsMade, elapsedMethodMs);
        System.out.printf("Synchronized Block  -> Sold: %d | Time: %d ms%n", blockScreen.bookingsMade.get(), elapsedBlockMs);
    }
}
