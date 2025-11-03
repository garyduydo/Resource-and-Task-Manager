import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventLogger {
    private LocalDateTime timestamp;
    private String userId;
    private String username;
    private String action;
    private String details;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public EventLogger(String userId, String username, String action, String details) {
        this.timestamp = LocalDateTime.now();
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.details = details;
    }

    @Override
    public String toString() {
        return String.format("[%s] userId=%s user=%s action=%s details=%s",
                timestamp.format(FORMATTER), userId, username, action, details);
    }
}
