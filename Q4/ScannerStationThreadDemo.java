/**
 * Q4 (Part 1): Parallel Answer-Script Scanning using 'extends Thread'.
 * Demonstrates thread life-cycle states (NEW, RUNNABLE, TIMED_WAITING, TERMINATED),
 * start(), sleep(), join(), isAlive(), and run() vs start() behaviour.
 */
class ScannerStationThread extends Thread {
    private final int scriptsToScan;
    private int scannedCount = 0;

    public ScannerStationThread(String stationName, int scriptsToScan) {
        super(stationName);
        this.scriptsToScan = scriptsToScan;
    }

    @Override
    public void run() {
        for (int i = 1; i <= scriptsToScan; i++) {
            scannedCount++;
            try {
                Thread.sleep(30); // Transitions thread into TIMED_WAITING
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.printf("[%s] Completed scanning %d scripts (Executing on thread: %s)%n",
                getName(), scannedCount, Thread.currentThread().getName());
    }

    public int getScannedCount() {
        return scannedCount;
    }
}

public class ScannerStationThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part A: Calling run() Directly vs start() ===");
        ScannerStationThread directTest = new ScannerStationThread("Direct-Call-Station", 2);
        System.out.println("State before run(): " + directTest.getState() + " | isAlive=" + directTest.isAlive());
        // Calling run() directly executes synchronously on 'main' thread — NO new thread created!
        directTest.run();
        System.out.println("State after direct run(): " + directTest.getState() + " | isAlive=" + directTest.isAlive());

        System.out.println("\n=== Part B: Parallel Scanning with 4 ScannerStationThread Instances ===");
        ScannerStationThread[] stations = {
                new ScannerStationThread("Station-1", 5),
                new ScannerStationThread("Station-2", 5),
                new ScannerStationThread("Station-3", 5),
                new ScannerStationThread("Station-4", 5)
        };

        for (ScannerStationThread s : stations) {
            System.out.println(s.getName() + " before start() -> State: " + s.getState() + ", isAlive: " + s.isAlive());
        }

        for (ScannerStationThread s : stations) {
            s.start(); // Transitions from NEW to RUNNABLE
        }

        Thread.sleep(45); // Allow threads to enter TIMED_WAITING during their sleep() cycle
        for (ScannerStationThread s : stations) {
            System.out.println(s.getName() + " mid-run        -> State: " + s.getState() + ", isAlive: " + s.isAlive());
        }

        int totalScanned = 0;
        for (ScannerStationThread s : stations) {
            s.join(); // Main thread waits until station thread reaches TERMINATED
            System.out.println(s.getName() + " after join()   -> State: " + s.getState() + ", isAlive: " + s.isAlive());
            totalScanned += s.getScannedCount();
        }

        System.out.println("All stations finished. Total scripts scanned = " + totalScanned);
    }
}
