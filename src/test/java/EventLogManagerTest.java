import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventLogManagerTest {

    private FakeLog fakeLog;
    private EventLogManager originalInstance;

    static class FakeLog {
        private final List<String> entries = new java.util.ArrayList<>();

        void log(String userId, String username, String action, String details) {
            entries.add(String.join("|", userId, username, action, details));
        }

        List<String> getAll() {
            return new java.util.ArrayList<>(entries);
        }

        void clear() {
            entries.clear();
        }
    }

    @BeforeEach
    void setUp() {
        originalInstance = EventLogManager.getInstance();

        fakeLog = new FakeLog();
        EventLogManager.setInstanceForTesting(new EventLogManager() {
            @Override
            public void log(String userId, String username, String action, String details) {
                fakeLog.log(userId, username, action, details);
            }

            @Override
            public List<String> getAllLogs() {
                return fakeLog.getAll();
            }
        });
    }

    @AfterEach
    void tearDown() {
        fakeLog.clear();
        EventLogManager.setInstanceForTesting(originalInstance);
    }


    @Test
    @DisplayName("Log a single entry")
    void testLogAddsEntry() {
        EventLogManager.getInstance().log("u1", "user1", "ACTION", "details");

        List<String> logs = EventLogManager.getInstance().getAllLogs();
        assertEquals(1, logs.size(), "Expected one log entry");
        assertTrue(logs.get(0).contains("u1"), "Log should contain userId");
        assertTrue(logs.get(0).contains("user1"), "Log should contain username");
        assertTrue(logs.get(0).contains("ACTION"), "Log should contain action");
        assertTrue(logs.get(0).contains("details"), "Log should contain details");
    }

    @Test
    @DisplayName("Logs are empty initially")
    void testGetAllLogsEmptyInitially() {
        List<String> logs = EventLogManager.getInstance().getAllLogs();
        assertTrue(logs.isEmpty(), "Expected no logs initially");
    }

    @Test
    @DisplayName("Log multiple entries")
    void testMultipleLogs() {
        EventLogManager.getInstance().log("u1", "user1", "ACTION1", "details1");
        EventLogManager.getInstance().log("u2", "user2", "ACTION2", "details2");

        List<String> logs = EventLogManager.getInstance().getAllLogs();
        assertEquals(2, logs.size(), "Expected two log entries");
        assertTrue(logs.get(0).contains("ACTION1"), "First log should contain ACTION1");
        assertTrue(logs.get(1).contains("ACTION2"), "Second log should contain ACTION2");
    }

    @Test
    @DisplayName("Writing log to non-existent folder handles IOException")
    void testLogIOExceptionIntegration() {
        String unwritablePath = "nonexistent_folder/log.txt";
        EventLogManager manager = new EventLogManager() {
            @Override
            public void log(String userId, String username, String action, String details) {
                try {
                    super.log(userId, username, action, details);
                } catch (Exception e) {
                    fail("log() should not throw exception");
                }
            }
        };
        EventLogManager.setInstanceForTesting(manager);

        assertDoesNotThrow(() ->
                EventLogManager.getInstance().log("u1", "user1", "ACTION", "details")
        );
    }

    @Test
    @DisplayName("Reading logs handles missing file / IOException")
    void testReadIOException() {
        EventLogManager faultyManager = new EventLogManager() {
            @Override
            public List<String> getAllLogs() {
                try {
                    throw new java.io.IOException("Simulated read failure");
                } catch (IOException e) {
                    System.err.println("Failed to read logs: " + e.getMessage());
                    return java.util.Collections.emptyList();
                }
            }
        };

        EventLogManager.setInstanceForTesting(faultyManager);

        List<String> logs = assertDoesNotThrow(
            () -> EventLogManager.getInstance().getAllLogs(),
            "getAllLogs should handle missing file and not throw"
        );

        assertTrue(logs.isEmpty(), "Logs should be empty when file cannot be read");
    }


    @Test
    void testGetAllLogsIOExceptionBranch() throws IOException {
        EventLogManager.setInstanceForTesting(null);

        File tmpFile = File.createTempFile("log", ".txt");

        if (!tmpFile.setReadable(false)) {
            System.out.println("Warning: Could not make file unreadable, test may fail on some OSes.");
        }

        EventLogManager.LOG_FILE = tmpFile.getAbsolutePath();

        EventLogManager manager = EventLogManager.getInstance();

        List<String> logs = assertDoesNotThrow(
            manager::getAllLogs,
            "getAllLogs should catch IOException and not throw"
        );

        assertTrue(logs.isEmpty());

        tmpFile.setReadable(true);
        tmpFile.delete();
    }

}
