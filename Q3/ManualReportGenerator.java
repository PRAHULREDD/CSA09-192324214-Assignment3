import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Q3 (Part 1): Daily Sales Report Generator using manual try-catch-finally.
 * Demonstrates how opening multiple resources across a batch and closing manually
 * leaks file handles when a mid-batch parsing or I/O failure occurs.
 */
public class ManualReportGenerator {
    private static final Logger LOGGER = Logger.getLogger(ManualReportGenerator.class.getName());

    public static double generateManualReport(List<String> storeFiles, String outputFile) {
        TrackedFileReader.resetCounterForDemo();
        List<TrackedFileReader> openedReaders = new ArrayList<>();
        PrintWriter writer = null;
        double grandTotal = 0.0;
        String currentFile = "NONE";

        try {
            writer = new PrintWriter(new FileWriter(outputFile));
            for (String file : storeFiles) {
                currentFile = file;
                TrackedFileReader reader = new TrackedFileReader(file);
                openedReaders.add(reader);

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        grandTotal += Double.parseDouble(line);
                    }
                }
                // BUGGY MANUAL DESIGN: developer closed reader only at the end of the loop body,
                // so any exception in parseDouble() or readLine() jumps straight to catch/finally!
                reader.close();
            }
            writer.printf("DAILY_SALES_TOTAL=%.2f%n", grandTotal);
            return grandTotal;

        } catch (IOException | NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Failure while processing store file: " + currentFile, e);
            return -1.0;

        } finally {
            // Naive finally block closes only the writer, forgetting readers opened before the crash!
            if (writer != null) {
                writer.close();
            }
            System.out.println("[Manual Version] Open file handles remaining after execution: "
                    + TrackedFileReader.getOpenHandleCount());
        }
    }

    public static void main(String[] args) {
        List<String> validFiles = List.of(
                "data/store_chennai.txt",
                "data/store_bengaluru.txt",
                "data/store_mumbai.txt"
        );
        System.out.println("=== Manual Version: Valid Input Run ===");
        double total = generateManualReport(validFiles, "daily_summary_manual.txt");
        System.out.printf("Computed Grand Total: Rs. %.2f%n%n", total);

        List<String> failingFiles = List.of(
                "data/store_chennai.txt",
                "data/store_corrupt.txt",
                "data/store_mumbai.txt"
        );
        System.out.println("=== Manual Version: Failing Input Run (Corrupt File) ===");
        generateManualReport(failingFiles, "daily_summary_manual_fail.txt");
    }
}
