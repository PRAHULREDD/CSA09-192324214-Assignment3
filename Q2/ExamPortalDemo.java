import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Q2: User-Defined Exceptions and Propagation in an Online Examination Portal
 * Demonstrates 3-layer propagation (SubmissionStorage -> ExamService -> ExamController),
 * checked vs unchecked custom exceptions, and exception chaining.
 */
class PortalExamException extends Exception {
    public PortalExamException(String message) {
        super(message);
    }
    public PortalExamException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ExamWindowClosedException extends PortalExamException {
    public ExamWindowClosedException(String message) {
        super(message);
    }
}

class PortalStorageException extends PortalExamException {
    public PortalStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

class UnregisteredStudentException extends RuntimeException {
    public UnregisteredStudentException(String message) {
        super(message);
    }
}

// Layer 3: Storage Layer
class SubmissionStorage {
    private static final long MAX_FILE_SIZE_KB = 500;

    public void persistAnswerScript(String studentId, String fileName, long sizeKb)
            throws PortalStorageException {
        try {
            if (sizeKb > MAX_FILE_SIZE_KB) {
                throw new IOException("Disk quota exceeded (" + sizeKb + " KB > " + MAX_FILE_SIZE_KB + " KB limit)");
            }
            System.out.println("[Storage] Persisted " + fileName + " (" + sizeKb + " KB) for " + studentId);
        } catch (IOException ioEx) {
            // Wrap low-level IOException in domain-specific checked exception with chaining
            throw new PortalStorageException("Storage failed for file: " + fileName, ioEx);
        }
    }

    public void persistWithoutChaining(String fileName, long sizeKb)
            throws PortalStorageException {
        try {
            if (sizeKb > MAX_FILE_SIZE_KB) {
                throw new IOException("Disk quota exceeded");
            }
        } catch (IOException ioEx) {
            // Intentionally omitting 'ioEx' cause to demonstrate lost diagnostic context
            throw new PortalStorageException("Storage failed for file: " + fileName, null);
        }
    }
}

// Layer 2: Service Layer
class ExamService {
    private final SubmissionStorage storage = new SubmissionStorage();
    private final Set<String> registeredStudents = Set.of("STU101", "STU102", "STU103");

    // Note: UnregisteredStudentException is unchecked, so it does not require a throws declaration
    public void submitExam(String studentId, String fileName, long sizeKb, boolean windowOpen)
            throws ExamWindowClosedException, PortalStorageException {
        if (!windowOpen) {
            throw new ExamWindowClosedException(
                    "Submission rejected: Exam window closed at " + LocalDateTime.now().withNano(0));
        }
        if (!registeredStudents.contains(studentId)) {
            throw new UnregisteredStudentException(
                    "Access denied: Student " + studentId + " is not enrolled in CSA09.");
        }
        // Propagates PortalStorageException upward to controller
        storage.persistAnswerScript(studentId, fileName, sizeKb);
    }
}

// Layer 1: Controller Layer (UI Boundary)
public class ExamPortalDemo {
    private final ExamService examService = new ExamService();

    public void handleSubmissionRequest(String studentId, String fileName, long sizeKb, boolean windowOpen) {
        System.out.printf("%n--- Request: student=%s, file=%s, size=%dKB, windowOpen=%b ---%n",
                studentId, fileName, sizeKb, windowOpen);
        try {
            examService.submitExam(studentId, fileName, sizeKb, windowOpen);
            System.out.println("[UI Response 200 OK] Answer script uploaded successfully!");
        } catch (ExamWindowClosedException e) {
            System.out.println("[UI Response 403 DEADLINE_PASSED] " + e.getMessage());
        } catch (PortalStorageException e) {
            System.out.println("[UI Response 500 UPLOAD_ERROR] " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  [Root Cause Logged for Admin] "
                        + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
        } catch (UnregisteredStudentException e) {
            System.out.println("[UI Response 401 UNAUTHORIZED] " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ExamPortalDemo controller = new ExamPortalDemo();

        // Scenario 1: Valid submission
        controller.handleSubmissionRequest("STU101", "STU101_CSA09.pdf", 240, true);

        // Scenario 2: Closed exam window (Checked)
        controller.handleSubmissionRequest("STU102", "STU102_CSA09.pdf", 180, false);

        // Scenario 3: Unregistered student (Unchecked)
        controller.handleSubmissionRequest("STU999", "STU999_CSA09.pdf", 150, true);

        // Scenario 4: Oversized file -> Storage IOException chained to PortalStorageException
        controller.handleSubmissionRequest("STU103", "STU103_CSA09_HD.pdf", 920, true);

        // Scenario 5: Demonstrating what is lost when cause is NOT passed
        System.out.println("\n--- Chaining vs Non-Chaining Comparison ---");
        SubmissionStorage rawStorage = new SubmissionStorage();
        try {
            rawStorage.persistWithoutChaining("lost_cause.pdf", 900);
        } catch (PortalStorageException e) {
            System.out.println("Caught exception message : " + e.getMessage());
            System.out.println("getCause() result        : " + e.getCause()
                    + " (Original IOException stack trace and disk quota reason completely lost!)");
        }
    }
}
