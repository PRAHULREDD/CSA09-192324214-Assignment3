import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;

/**
 * Q1: Handling Failure in a Bank ATM Simulation
 * Demonstrates checked exceptions (FileNotFoundException, IOException, InsufficientBalanceException),
 * unchecked exceptions (InputMismatchException, NumberFormatException, InvalidAmountException),
 * multi-catch blocks, ordered catch blocks, and guaranteed cleanup in finally.
 */
class InsufficientBalanceException extends Exception {
    private final double currentBalance;
    private final double requestedAmount;

    public InsufficientBalanceException(String message, double currentBalance, double requestedAmount) {
        super(message);
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public double getShortfall() {
        return requestedAmount - currentBalance;
    }
}

class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(String message) {
        super(message);
    }
}

public class ATMSimulation {

    private static Map<String, Double> loadAccountsFromCardReader(String cardFilePath)
            throws FileNotFoundException, IOException {
        Map<String, Double> accounts = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(cardFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    throw new IOException("Corrupted card-reader record: " + line);
                }
                accounts.put(parts[0].trim(), Double.parseDouble(parts[2].trim()));
            }
        }
        return accounts;
    }

    public static String processWithdrawal(String cardFilePath, String accountNo, String rawAmountInput) {
        boolean cardInserted = true;
        StringBuilder traceLog = new StringBuilder();
        traceLog.append(String.format("[Session] File=%s | Account=%s | Input='%s'%n",
                cardFilePath, accountNo, rawAmountInput));

        try {
            // 1. Checked I/O failure: missing or unreadable card-reader file
            Map<String, Double> accounts = loadAccountsFromCardReader(cardFilePath);

            // 2. Account validation
            if (accountNo == null || !accounts.containsKey(accountNo.trim())) {
                throw new IllegalArgumentException("Unknown account number: " + accountNo);
            }

            // 3. Unchecked input parsing failure: letters instead of numeric amount
            if (rawAmountInput == null || rawAmountInput.trim().isEmpty()) {
                throw new InputMismatchException("Empty withdrawal input received");
            }
            double amount = Double.parseDouble(rawAmountInput.trim());

            // 4. Unchecked domain rule failure: zero or negative withdrawal
            if (amount <= 0) {
                throw new InvalidAmountException("Withdrawal amount must be positive. Entered: " + amount);
            }

            // 5. Checked business rule failure: insufficient balance
            double balance = accounts.get(accountNo.trim());
            if (amount > balance) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient funds (Available: Rs. %.2f, Requested: Rs. %.2f)",
                                balance, amount),
                        balance, amount);
            }

            double remaining = balance - amount;
            traceLog.append(String.format("  -> [SUCCESS] Dispensed Rs. %.2f | Remaining Balance: Rs. %.2f%n",
                    amount, remaining));
            return "SUCCESS";

        } catch (FileNotFoundException e) {
            traceLog.append("  -> [CAUGHT: FileNotFoundException] Card-reader file missing: ")
                    .append(e.getMessage()).append("\n");
            return "CATCH_FILE_NOT_FOUND";

        } catch (IOException e) {
            traceLog.append("  -> [CAUGHT: IOException] Card-reader hardware/I/O error: ")
                    .append(e.getMessage()).append("\n");
            return "CATCH_IO_EXCEPTION";

        } catch (InputMismatchException | NumberFormatException e) {
            // Multi-catch for malformed numeric input
            traceLog.append("  -> [CAUGHT: Multi-Catch InputMismatch/NumberFormat] Invalid numeric entry: ")
                    .append(e.getMessage()).append("\n");
            return "CATCH_MULTI_NUMBER_FORMAT";

        } catch (InvalidAmountException | IllegalArgumentException e) {
            // Multi-catch for invalid domain arguments (negative amount or unknown account)
            traceLog.append("  -> [CAUGHT: Multi-Catch InvalidAmount/IllegalArgument] ")
                    .append(e.getMessage()).append("\n");
            return "CATCH_MULTI_INVALID_ARG";

        } catch (InsufficientBalanceException e) {
            traceLog.append(String.format("  -> [CAUGHT: InsufficientBalanceException] %s (Shortfall: Rs. %.2f)%n",
                    e.getMessage(), e.getShortfall()));
            return "CATCH_INSUFFICIENT_BALANCE";

        } catch (Exception e) {
            // Broadest fallback handler placed strictly last
            traceLog.append("  -> [CAUGHT: Exception] Unexpected failure: ")
                    .append(e.getMessage()).append("\n");
            return "CATCH_GENERIC_EXCEPTION";

        } finally {
            // Guaranteed cleanup regardless of normal return or exception
            if (cardInserted) {
                cardInserted = false;
                traceLog.append("  -> [FINALLY] Card ejected, keypad locked, session buffer cleared.\n");
            }
            System.out.print(traceLog.toString());
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && "--interactive".equalsIgnoreCase(args[0])) {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Enter Account Number (e.g., ACC1001): ");
                String acc = scanner.nextLine();
                System.out.print("Enter Withdrawal Amount: ");
                String amt = scanner.nextLine();
                processWithdrawal("card_reader.txt", acc, amt);
            }
            return;
        }

        System.out.println("=== ATM Simulation: Automated Test Paths ===");
        processWithdrawal("card_reader.txt", "ACC1001", "5000");       // Path 1: Valid
        processWithdrawal("card_reader.txt", "ACC1001", "five00");     // Path 2: Non-numeric input
        processWithdrawal("card_reader.txt", "ACC1001", "-1500");      // Path 3: Negative amount
        processWithdrawal("card_reader.txt", "ACC1003", "5000");       // Path 4: Insufficient balance
        processWithdrawal("missing_card.txt", "ACC1001", "1000");      // Path 5: Missing card file
        processWithdrawal("card_reader.txt", "ACC9999", "1000");       // Path 6: Unknown account
    }
}
