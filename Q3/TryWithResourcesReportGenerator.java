import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Q3 (Part 2): Daily Sales Report Generator refactored with try-with-resources.
 * Guarantees deterministic closing in reverse declaration order and preserves suppressed exceptions.
 */
public class TryWithResourcesReportGenerator {
    private static final Logger LOGGER = Logger.getLogger(TryWithResourcesReportGenerator.class.getName());

    public static double generateSafeReport(List<String> storeFiles, String outputFile) {
        TrackedFileReader.resetCounterForDemo();
        double grandTotal = 0.0;
        String currentFile = "NONE";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            for (String file : storeFiles) {
                currentFile = file;
                // Each store file reader is scoped to its own try-with-resources block
                try (TrackedFileReader reader = new TrackedFileReader(file)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            grandTotal += Double.parseDouble(line);
                        }
                    }
                } // reader.close() is ALWAYS invoked automatically here, even on NumberFormatException
            }
            writer.printf("DAILY_SALES_TOTAL=%.2f%n", grandTotal);
            return grandTotal;

        } catch (IOException | NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Safe abort while processing store file: " + currentFile, e);
            return -1.0;
        } finally {
            System.out.println("[Try-With-Resources] Open file handles remaining after execution: "
                    + TrackedFileReader.getOpenHandleCount());
        }
    }

    // Demonstrates suppressed exceptions when both try block and close() throw exceptions
    static class FaultyResource implements AutoCloseable {
        private final String name;
        FaultyResource(String name) { this.name = name; }

        @Override
        public void close() throws IOException {
            System.out.println("Closing resource in reverse order: " + name);
            throw new IOException("Close failure on " + name);
        }
    }

    public static void demonstrateSuppressedExceptions() {
        System.out.println("\n=== Suppressed Exception & Reverse Close Order Demo ===");
        try (FaultyResource r1 = new FaultyResource("FirstOpened_R1");
             FaultyResource r2 = new FaultyResource("SecondOpened_R2")) {
            throw new RuntimeException("Primary processing exception inside try block");
        } catch (Exception e) {
            System.out.println("Primary Exception  : " + e);
            for (Throwable sup : e.getSuppressed()) {
                System.out.println("Suppressed Exception: " + sup);
            }
        }
    }

    public static void main(String[] args) {
        List<String> validFiles = List.of(
                "data/store_chennai.txt",
                "data/store_bengaluru.txt",
                "data/store_mumbai.txt"
        );
        System.out.println("=== Try-With-Resources: Valid Input Run ===");
        double total = generateSafeReport(validFiles, "daily_summary_twr.txt");
        System.out.printf("Computed Grand Total: Rs. %.2f%n%n", total);

        List<String> failingFiles = List.of(
                "data/store_chennai.txt",
                "data/store_corrupt.txt",
                "data/store_mumbai.txt"
        );
        System.out.println("=== Try-With-Resources: Failing Input Run (Corrupt File) ===");
        generateSafeReport(failingFiles, "daily_summary_twr_fail.txt");

        demonstrateSuppressedExceptions();
    }
}
