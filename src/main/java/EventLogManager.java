import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EventLogManager {
    protected static String LOG_FILE = "./event_logs.txt";
    protected static EventLogManager instance;

    protected EventLogManager() {}

    public static EventLogManager getInstance() {
        if (instance == null) {
            instance = new EventLogManager();
        }
        return instance;
    }

    public void log(String userId, String username, String action, String details) {
        File logFile = new File(LOG_FILE);
        logFile.getParentFile().mkdirs();

        EventLogger entry = new EventLogger(userId, username, action, details);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(entry.toString());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write event log: " + e.getMessage());
        }
    }

    public List<String> getAllLogs() {
        List<String> logs = new ArrayList<>();

        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) {
            return logs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) logs.add(line);
        } catch (IOException e) {
            System.err.println("Failed to read logs: " + e.getMessage());
        }
        return logs;
    }

    static void setInstanceForTesting(EventLogManager newInstance) {
        instance = newInstance;
    }

}
