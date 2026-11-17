import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Demonstrates why narrower exceptions MUST precede broader exceptions in catch blocks.
 */
public class CatchOrderDemo {
    public static void main(String[] args) {
        // CORRECT ORDER: Narrowest subclass (FileNotFoundException) -> Parent (IOException) -> Exception
        try {
            FileReader fr = new FileReader("non_existent_card.txt");
            fr.close();
        } catch (FileNotFoundException e) {
            System.out.println("Correct order caught specific FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Caught general IOException: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Caught fallback Exception: " + e.getMessage());
        }

        /*
         * INCORRECT ORDER (Uncommenting below causes a compile-time error):
         *
         * try {
         *     FileReader fr = new FileReader("non_existent_card.txt");
         * } catch (Exception e) {
         *     System.out.println("Broad catch");
         * } catch (FileNotFoundException e) {
         *     // javac error: exception FileNotFoundException has already been caught
         * }
         */
    }
}
