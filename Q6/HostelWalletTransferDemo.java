import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Q6 (Part A): Hostel Wallet Transfers
 * Demonstrates:
 * 1. Deadlock caused by inconsistent lock ordering (T1 locks A->B, T2 locks B->A)
 * 2. Deadlock prevention using global account ID lock ordering
 */
class WalletAccount {
    private final int accountId;
    private final String owner;
    private double balance;

    public WalletAccount(int accountId, String owner, double balance) {
        this.accountId = accountId;
        this.owner = owner;
        this.balance = balance;
    }

    public int getAccountId() { return accountId; }
    public String getOwner() { return owner; }
    public double getBalance() { return balance; }
    public void debit(double amt) { balance -= amt; }
    public void credit(double amt) { balance += amt; }
}

public class HostelWalletTransferDemo {

    // Prone to deadlock when called concurrently as transfer(A, B) and transfer(B, A)
    public static void transferDeadlockProne(WalletAccount from, WalletAccount to, double amount) {
        synchronized (from) {
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            synchronized (to) {
                if (from.getBalance() >= amount) {
                    from.debit(amount);
                    to.credit(amount);
                }
            }
        }
    }

    // Deadlock-free: always acquire lock on lower accountId first
    public static void transferOrdered(WalletAccount from, WalletAccount to, double amount) {
        if (from.getAccountId() == to.getAccountId()) return;
        WalletAccount firstLock = (from.getAccountId() < to.getAccountId()) ? from : to;
        WalletAccount secondLock = (from.getAccountId() < to.getAccountId()) ? to : from;

        synchronized (firstLock) {
            synchronized (secondLock) {
                if (from.getBalance() >= amount) {
                    from.debit(amount);
                    to.credit(amount);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part 1: Reproducing Deadlock with Opposite Lock Order ===");
        WalletAccount accA = new WalletAccount(101, "Hostel-Student-A", 1000.0);
        WalletAccount accB = new WalletAccount(102, "Hostel-Student-B", 1000.0);

        Thread t1 = new Thread(() -> transferDeadlockProne(accA, accB, 100), "Transfer-A-to-B");
        Thread t2 = new Thread(() -> transferDeadlockProne(accB, accA, 200), "Transfer-B-to-A");
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        Thread.sleep(200);
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlockedIds = bean.findDeadlockedThreads();
        System.out.println("Deadlock detected by JVM ThreadMXBean: "
                + (deadlockedIds != null ? deadlockedIds.length + " threads deadlocked!" : "None"));

        System.out.println("\n=== Part 2: Preventing Deadlock via Consistent Lock Ordering ===");
        WalletAccount safeA = new WalletAccount(201, "Student-A", 5000.0);
        WalletAccount safeB = new WalletAccount(202, "Student-B", 5000.0);

        Thread[] workers = new Thread[20];
        for (int i = 0; i < 20; i++) {
            final boolean forward = (i % 2 == 0);
            workers[i] = new Thread(() -> {
                for (int k = 0; k < 50; k++) {
                    if (forward) transferOrdered(safeA, safeB, 10.0);
                    else transferOrdered(safeB, safeA, 10.0);
                }
            });
            workers[i].start();
        }
        for (Thread w : workers) w.join();

        System.out.printf("Completed 1,000 concurrent bidirectional transfers with ZERO deadlocks!%n");
        System.out.printf("Final Balances -> A: Rs. %.2f, B: Rs. %.2f, Combined Total: Rs. %.2f (Expected: 10000.00)%n",
                safeA.getBalance(), safeB.getBalance(), safeA.getBalance() + safeB.getBalance());
    }
}
