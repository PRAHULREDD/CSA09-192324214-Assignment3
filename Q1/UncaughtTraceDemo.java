/**
 * Demonstrates an uncaught runtime exception so Student B can inspect each frame of the stack trace.
 */
public class UncaughtTraceDemo {
    static void dispenseNotes(int amount, int noteDenomination) {
        // Intentional division by zero to generate a clean uncaught ArithmeticException trace
        int noteCount = amount / noteDenomination;
        System.out.println("Dispensing notes: " + noteCount);
    }

    static void authorizeTransaction(int amount) {
        dispenseNotes(amount, 0);
    }

    public static void main(String[] args) {
        System.out.println("Triggering uncaught exception for stack trace analysis...");
        authorizeTransaction(2000);
    }
}
