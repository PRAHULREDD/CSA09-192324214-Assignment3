import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Q4 Alternative Concept: Processing n script batches across a fixed pool of p worker threads
 * using ExecutorService instead of manually instantiating Thread objects.
 */
public class ScannerExecutorAlternative {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int totalBatches = 16; // n batches
        int poolSize = 4;      // p threads
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        long start = System.nanoTime();
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 1; i <= totalBatches; i++) {
            final int batchId = i;
            tasks.add(() -> {
                Thread.sleep(20); // Simulated scanning latency per batch
                return 25; // 25 scripts per batch
            });
        }

        List<Future<Integer>> results = executor.invokeAll(tasks);
        int totalScripts = 0;
        for (Future<Integer> f : results) {
            totalScripts += f.get();
        }
        executor.shutdown();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        System.out.printf("ExecutorService (p=%d threads) scanned %d scripts across %d batches in %d ms%n",
                poolSize, totalScripts, totalBatches, elapsedMs);
    }
}
