import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class EventLoggerTest {

    @Test
    void testConstructorAndToString() {
        String userId = "u123";
        String username = "testUser";
        String action = "TEST_ACTION";
        String details = "This is a test";

        EventLogger logger = new EventLogger(userId, username, action, details);

        String output = logger.toString();
        assertNotNull(output, "toString() should not return null");

        assertTrue(output.contains(userId), "Output should contain userId");
        assertTrue(output.contains(username), "Output should contain username");
        assertTrue(output.contains(action), "Output should contain action");
        assertTrue(output.contains(details), "Output should contain details");

        String timestampPart = output.substring(output.indexOf("[") + 1, output.indexOf("]"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        assertDoesNotThrow(() -> {
            LocalDateTime parsed = LocalDateTime.parse(timestampPart, formatter);
            assertNotNull(parsed, "Timestamp should parse correctly");
        });
    }
}
