import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Q7: Concurrent, Fault-Tolerant Semester Result Processor
 * Processes up to 10,000 answer-script score files concurrently using ExecutorService,
 * Callable/Future, and ConcurrentHashMap while isolating missing and corrupt files.
 */
public class ResultProcessor {

    public static class StudentResult {
        public final String studentId;
        public final int totalMarks;
        public final double percentage;
        public final String grade;
        public final boolean passed;

        public StudentResult(String studentId, int totalMarks, double percentage, String grade, boolean passed) {
            this.studentId = studentId;
            this.totalMarks = totalMarks;
            this.percentage = percentage;
            this.grade = grade;
            this.passed = passed;
        }
    }

    public static class BatchSummary {
        public final int totalExpected;
        public final int successCount;
        public final int missingCount;
        public final int corruptCount;
        public final long elapsedMillis;

        public BatchSummary(int totalExpected, int successCount, int missingCount, int corruptCount, long elapsedMillis) {
            this.totalExpected = totalExpected;
            this.successCount = successCount;
            this.missingCount = missingCount;
            this.corruptCount = corruptCount;
            this.elapsedMillis = elapsedMillis;
        }
    }

    // Callable task processing one student's score file in isolation
    static class ScriptScoreTask implements Callable<StudentResult> {
        private final Path filePath;
        private final ConcurrentHashMap<String, StudentResult> resultStore;
        private final ConcurrentLinkedQueue<String> errorAuditLog;
        private final AtomicInteger missingCounter;
        private final AtomicInteger corruptCounter;

        ScriptScoreTask(Path filePath,
                        ConcurrentHashMap<String, StudentResult> resultStore,
                        ConcurrentLinkedQueue<String> errorAuditLog,
                        AtomicInteger missingCounter,
                        AtomicInteger corruptCounter) {
            this.filePath = filePath;
            this.resultStore = resultStore;
            this.errorAuditLog = errorAuditLog;
            this.missingCounter = missingCounter;
            this.corruptCounter = corruptCounter;
        }

        @Override
        public StudentResult call() {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) {
                    corruptCounter.incrementAndGet();
                    errorAuditLog.add("[CORRUPT-EMPTY] " + filePath.getFileName());
                    return null;
                }

                String[] parts = line.trim().split(",");
                if (parts.length != 6) {
                    corruptCounter.incrementAndGet();
                    errorAuditLog.add("[CORRUPT-FORMAT] " + filePath.getFileName() + " -> expected 6 fields");
                    return null;
                }

                String studentId = parts[0].trim();
                int sum = 0;
                boolean allSubjectsPass = true;
                for (int i = 1; i <= 5; i++) {
                    int mark = Integer.parseInt(parts[i].trim());
                    if (mark < 0 || mark > 100) {
                        throw new IllegalArgumentException("Mark out of range [0..100]: " + mark);
                    }
                    if (mark < 40) allSubjectsPass = false;
                    sum += mark;
                }

                double pct = sum / 5.0;
                String grade = computeGrade(pct, allSubjectsPass);
                StudentResult res = new StudentResult(studentId, sum, pct, grade, allSubjectsPass);
                resultStore.put(studentId, res);
                return res;

            } catch (NoSuchFileException e) {
                missingCounter.incrementAndGet();
                errorAuditLog.add("[MISSING] " + filePath.getFileName());
                return null;
            } catch (IOException | IllegalArgumentException e) {
                corruptCounter.incrementAndGet();
                errorAuditLog.add("[CORRUPT] " + filePath.getFileName() + " -> " + e.getMessage());
                return null;
            }
        }

        private static String computeGrade(double pct, boolean passed) {
            if (!passed) return "F";
            if (pct >= 90) return "O";
            if (pct >= 80) return "A";
            if (pct >= 65) return "B";
            if (pct >= 45) return "C";
            return "P";
        }
    }

    public static BatchSummary runBatch(List<Path> scriptPaths, int poolSize)
            throws InterruptedException, ExecutionException {
        ConcurrentHashMap<String, StudentResult> results = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<String> errorLog = new ConcurrentLinkedQueue<>();
        AtomicInteger missingCount = new AtomicInteger(0);
        AtomicInteger corruptCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        long startNs = System.nanoTime();

        List<Callable<StudentResult>> tasks = new ArrayList<>(scriptPaths.size());
        for (Path p : scriptPaths) {
            tasks.add(new ScriptScoreTask(p, results, errorLog, missingCount, corruptCount));
        }

        List<Future<StudentResult>> futures = executor.invokeAll(tasks);
        for (Future<StudentResult> f : futures) {
            f.get(); // Ensures all tasks have completed cleanly
        }
        executor.shutdown();

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        return new BatchSummary(scriptPaths.size(), results.size(), missingCount.get(), corruptCount.get(), elapsedMs);
    }

    public static List<Path> generateDataset(Path dir, int totalScripts) throws IOException {
        Files.createDirectories(dir);
        Random rng = new Random(42L); // Fixed seed for reproducible fault injection
        List<Path> expectedPaths = new ArrayList<>(totalScripts);

        for (int i = 1; i <= totalScripts; i++) {
            String id = String.format("STU%05d", i);
            Path file = dir.resolve(id + ".txt");
            expectedPaths.add(file);

            int roll = rng.nextInt(100);
            if (roll == 0) {
                // ~1% missing files (do not create file on disk)
                Files.deleteIfExists(file);
            } else if (roll == 1) {
                // ~1% corrupt files (non-numeric or out-of-range marks)
                try (BufferedWriter w = Files.newBufferedWriter(file)) {
                    w.write(id + ",88,BAD_MARK,74,91,82\n");
                }
            } else {
                try (BufferedWriter w = Files.newBufferedWriter(file)) {
                    w.write(String.format("%s,%d,%d,%d,%d,%d%n",
                            id,
                            40 + rng.nextInt(61),
                            40 + rng.nextInt(61),
                            40 + rng.nextInt(61),
                            40 + rng.nextInt(61),
                            40 + rng.nextInt(61)));
                }
            }
        }
        return expectedPaths;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Stage 1: Hand-Checked 10-File Verification Set ===");
        Path sampleDir = Paths.get("sample_scripts");
        List<Path> samplePaths = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            samplePaths.add(sampleDir.resolve(String.format("STU%04d.txt", i)));
        }
        BatchSummary sampleSummary = runBatch(samplePaths, 2);
        System.out.printf("Sample Set (Expected: 8 Success, 1 Missing, 1 Corrupt) -> Actual: %d Success, %d Missing, %d Corrupt%n",
                sampleSummary.successCount, sampleSummary.missingCount, sampleSummary.corruptCount);

        System.out.println("\n=== Stage 2: 10,000 Answer-Script Benchmark (Pool Sizes: 1, 2, 4, 8) ===");
        Path largeDir = Paths.get("scripts_10k");
        List<Path> tenKPaths = generateDataset(largeDir, 10_000);

        int[] poolSizes = {1, 2, 4, 8};
        for (int pool : poolSizes) {
            BatchSummary summary = runBatch(tenKPaths, pool);
            double throughput = (summary.elapsedMillis > 0)
                    ? (summary.totalExpected * 1000.0 / summary.elapsedMillis) : 0.0;
            System.out.printf("Pool Size = %d | Time = %4d ms | Throughput = %8.1f files/sec | Success=%d, Missing=%d, Corrupt=%d%n",
                    pool, summary.elapsedMillis, throughput,
                    summary.successCount, summary.missingCount, summary.corruptCount);
        }
    }
}
