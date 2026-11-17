import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Q7 Alternative Concepts:
 * 1. Java Parallel Streams (files.parallelStream())
 * 2. Thread-per-File approach (bounded batch demonstration to avoid OS thread exhaustion)
 */
public class ResultProcessorAlternatives {

    public static void runWithParallelStream(List<Path> files) {
        ConcurrentHashMap<String, Double> averages = new ConcurrentHashMap<>();
        AtomicInteger failed = new AtomicInteger(0);

        long start = System.nanoTime();
        files.parallelStream().forEach(path -> {
            try {
                String line = Files.readString(path).trim();
                String[] parts = line.split(",");
                int sum = 0;
                for (int i = 1; i <= 5; i++) sum += Integer.parseInt(parts[i].trim());
                averages.put(parts[0].trim(), sum / 5.0);
            } catch (Exception e) {
                failed.incrementAndGet();
            }
        });
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        System.out.printf("[ParallelStream] Valid: %d | Failed: %d | Time: %d ms%n",
                averages.size(), failed.get(), elapsedMs);
    }

    public static void runThreadPerFileSample(List<Path> files) throws InterruptedException {
        ConcurrentHashMap<String, Double> averages = new ConcurrentHashMap<>();
        AtomicInteger failed = new AtomicInteger(0);

        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>(files.size());
        for (Path path : files) {
            Thread t = new Thread(() -> {
                try {
                    String line = Files.readString(path).trim();
                    String[] parts = line.split(",");
                    int sum = 0;
                    for (int i = 1; i <= 5; i++) sum += Integer.parseInt(parts[i].trim());
                    averages.put(parts[0].trim(), sum / 5.0);
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        System.out.printf("[Thread-Per-File (%d threads)] Valid: %d | Failed: %d | Time: %d ms%n",
                files.size(), averages.size(), failed.get(), elapsedMs);
    }

    public static void main(String[] args) throws Exception {
        Path sampleDir = Paths.get("sample_scripts");
        List<Path> samplePaths = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            samplePaths.add(sampleDir.resolve(String.format("STU%04d.txt", i)));
        }
        runWithParallelStream(samplePaths);
        runThreadPerFileSample(samplePaths);
    }
}
