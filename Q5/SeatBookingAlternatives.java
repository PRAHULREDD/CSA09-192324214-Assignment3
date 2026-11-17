import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Q5 Alternative Concepts:
 * 1. Lock-free AtomicBoolean[] with compareAndSet(false, true)
 * 2. Explicit ReentrantLock
 * 3. ConcurrentHashMap with atomic putIfAbsent()
 */
public class SeatBookingAlternatives {

    static class AtomicSeatBooking {
        private final AtomicBoolean[] booked;
        private final AtomicInteger sold = new AtomicInteger(0);

        AtomicSeatBooking(int seats) {
            booked = new AtomicBoolean[seats];
            for (int i = 0; i < seats; i++) booked[i] = new AtomicBoolean(false);
        }

        public boolean book(int seat) {
            if (booked[seat].compareAndSet(false, true)) {
                sold.incrementAndGet();
                return true;
            }
            return false;
        }
    }

    static class ReentrantLockSeatBooking {
        private final boolean[] booked;
        private final ReentrantLock lock = new ReentrantLock();
        private int sold = 0;

        ReentrantLockSeatBooking(int seats) { booked = new boolean[seats]; }

        public boolean book(int seat) {
            lock.lock();
            try {
                if (!booked[seat]) {
                    booked[seat] = true;
                    sold++;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }
    }

    static class ConcurrentMapSeatBooking {
        private final ConcurrentHashMap<Integer, String> seatOwners = new ConcurrentHashMap<>();

        public boolean book(int seat, String userId) {
            return seatOwners.putIfAbsent(seat, userId) == null;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int seats = 100;
        int requests = 2000;

        AtomicSeatBooking atomicBooking = new AtomicSeatBooking(seats);
        ReentrantLockSeatBooking lockBooking = new ReentrantLockSeatBooking(seats);
        ConcurrentMapSeatBooking mapBooking = new ConcurrentMapSeatBooking();

        Thread[] threads = new Thread[requests];
        for (int i = 0; i < requests; i++) {
            final int seat = i % seats;
            final String user = "User-" + i;
            threads[i] = new Thread(() -> {
                atomicBooking.book(seat);
                lockBooking.book(seat);
                mapBooking.book(seat, user);
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        System.out.println("AtomicBoolean[] sold      : " + atomicBooking.sold.get());
        System.out.println("ReentrantLock sold        : " + lockBooking.sold);
        System.out.println("ConcurrentHashMap sold    : " + mapBooking.seatOwners.size());
    }
}
