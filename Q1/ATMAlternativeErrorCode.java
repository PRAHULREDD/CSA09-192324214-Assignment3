import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Q1 Alternative Concept: Avoiding exceptions for control flow by validating inputs upfront
 * with Scanner.hasNextDouble() and returning integer status/error codes.
 */
public class ATMAlternativeErrorCode {
    public static final int SUCCESS = 0;
    public static final int ERR_CARD_FILE_MISSING = -1;
    public static final int ERR_INVALID_NUMBER = -2;
    public static final int ERR_NON_POSITIVE_AMOUNT = -3;
    public static final int ERR_INSUFFICIENT_BALANCE = -4;
    public static final int ERR_ACCOUNT_NOT_FOUND = -5;

    public static int withdrawWithoutExceptions(String filePath, String accountNo, String rawInput) {
        File cardFile = new File(filePath);
        if (!cardFile.exists() || !cardFile.canRead()) {
            return ERR_CARD_FILE_MISSING;
        }

        Map<String, Double> balances = new HashMap<>();
        try (Scanner fileScanner = new Scanner(cardFile)) {
            while (fileScanner.hasNextLine()) {
                String[] parts = fileScanner.nextLine().trim().split(",");
                if (parts.length == 3) {
                    balances.put(parts[0].trim(), Double.parseDouble(parts[2].trim()));
                }
            }
        } catch (FileNotFoundException e) {
            return ERR_CARD_FILE_MISSING;
        }

        if (accountNo == null || !balances.containsKey(accountNo)) {
            return ERR_ACCOUNT_NOT_FOUND;
        }

        if (rawInput == null) {
            return ERR_INVALID_NUMBER;
        }
        Scanner inputScanner = new Scanner(rawInput.trim());
        if (!inputScanner.hasNextDouble()) {
            inputScanner.close();
            return ERR_INVALID_NUMBER;
        }
        double amount = inputScanner.nextDouble();
        if (inputScanner.hasNext()) { // trailing garbage like "500abc"
            inputScanner.close();
            return ERR_INVALID_NUMBER;
        }
        inputScanner.close();

        if (amount <= 0) {
            return ERR_NON_POSITIVE_AMOUNT;
        }

        double currentBalance = balances.get(accountNo);
        if (amount > currentBalance) {
            return ERR_INSUFFICIENT_BALANCE;
        }

        return SUCCESS;
    }

    public static void main(String[] args) {
        System.out.println("=== ATM Alternative (Error Codes without Exception Control Flow) ===");
        System.out.println("Valid withdrawal code      : " + withdrawWithoutExceptions("card_reader.txt", "ACC1001", "5000"));
        System.out.println("Invalid letters code       : " + withdrawWithoutExceptions("card_reader.txt", "ACC1001", "abc"));
        System.out.println("Negative amount code       : " + withdrawWithoutExceptions("card_reader.txt", "ACC1001", "-200"));
        System.out.println("Insufficient balance code  : " + withdrawWithoutExceptions("card_reader.txt", "ACC1003", "5000"));
        System.out.println("Missing card file code     : " + withdrawWithoutExceptions("missing.txt", "ACC1001", "1000"));
    }
}
