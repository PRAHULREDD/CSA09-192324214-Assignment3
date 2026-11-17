import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Q4 (Part 2): Parallel Answer-Script Scanning using 'implements Runnable'.
 * Runs 10 trials to record finish order, tests thread priorities, and demonstrates daemon threads.
 */
class ScannerStationTask implements Runnable {
    private final String stationName;
    private final int batches;
    private final List<String> completionOrder;

    public ScannerStationTask(String stationName, int batches, List<String> completionOrder) {
        this.stationName = stationName;
        this.batches = batches;
        this.completionOrder = completionOrder;
    }

    @Override
    public void run() {
        for (int b = 1; b <= batches; b++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (completionOrder != null) {
            completionOrder.add(stationName);
        }
    }
}

public class ScannerStationRunnableDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Experiment 1: Finish Order Across 10 Runs (Runnable) ===");
        for (int run = 1; run <= 10; run++) {
            List<String> order = Collections.synchronizedList(new ArrayList<>());
            Thread[] workers = new Thread[4];
            for (int i = 0; i < 4; i++) {
                workers[i] = new Thread(new ScannerStationTask("S" + (i + 1), 4, order), "Station-" + (i + 1));
            }
            for (Thread w : workers) w.start();
            for (Thread w : workers) w.join();
            System.out.printf("Run %02d finish order: %s%n", run, order);
        }

        System.out.println("\n=== Experiment 2: Thread Priorities (MIN=1 vs MAX=10) ===");
        List<String> priorityOrder = Collections.synchronizedList(new ArrayList<>());
        Thread lowPrio = new Thread(new ScannerStationTask("LowPrio-S1", 5, priorityOrder));
        Thread highPrio = new Thread(new ScannerStationTask("HighPrio-S2", 5, priorityOrder));
        lowPrio.setPriority(Thread.MIN_PRIORITY);
        highPrio.setPriority(Thread.MAX_PRIORITY);
        lowPrio.start();
        highPrio.start();
        lowPrio.join();
        highPrio.join();
        System.out.println("Priority test finish order: " + priorityOrder);

        System.out.println("\n=== Experiment 3: Daemon Thread Behaviour ===");
        Thread daemonStation = new Thread(() -> {
            try {
                System.out.println("[Daemon-Station] Started background audit scan...");
                Thread.sleep(2000); // Long task
                System.out.println("[Daemon-Station] Completed (will NOT print if main exits first!)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        daemonStation.setDaemon(true);
        daemonStation.start();
        Thread.sleep(50);
        System.out.println("[Main] Exiting now while daemon is still sleeping -> JVM terminates daemon immediately!");
    }
}
