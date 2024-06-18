import org.junit.jupiter.api.Test;
import dev.dagless.DirectoryMonitor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is just a setup for testing the DirectoryMonitor class.
 * Unfortunately I cannot test this on my macbook since the inotify events are not supported on Mac.
 */
public class DirectoryMonitorTest {
    @Test
    public void testConstructor() {
        // SET ENVIRONMENT VARIABLES
        Exception exception = assertThrows(RuntimeException.class, () -> new DirectoryMonitor(true));

        String expectedMessage = "No provider found";
        String actualMessage = exception.getMessage();

        System.out.println(actualMessage);

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
