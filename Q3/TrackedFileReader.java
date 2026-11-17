import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wrapper around BufferedReader that tracks active open OS file handles
 * so resource leaks in manual vs try-with-resources implementations can be measured.
 */
public class TrackedFileReader implements AutoCloseable {
    private static final AtomicInteger OPEN_HANDLES = new AtomicInteger(0);
    private final String fileName;
    private final BufferedReader delegate;
    private boolean closed = false;

    public TrackedFileReader(String fileName) throws IOException {
        this.fileName = fileName;
        this.delegate = new BufferedReader(new FileReader(fileName));
        OPEN_HANDLES.incrementAndGet();
    }

    public String readLine() throws IOException {
        return delegate.readLine();
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            OPEN_HANDLES.decrementAndGet();
            delegate.close();
        }
    }

    public static int getOpenHandleCount() {
        return OPEN_HANDLES.get();
    }

    public static void resetCounterForDemo() {
        OPEN_HANDLES.set(0);
    }
}
