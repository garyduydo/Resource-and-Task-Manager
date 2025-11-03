import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminManagerProxyTest {

    private User adminUser;
    private FakeAdminManager fakeAdminManager;
    private FakeEventLogManager fakeLogManager;
    private AdminManagerProxy proxy;
    private User rootAdmin;
    private User normalUser;
    private AdminManager adminManager;
    private FakeEventLogManager logManager;
    private ScrollManager scrollManager;

    @BeforeEach
    void setUp() {
        try {
            String usersFolder = "testUsers_" + UUID.randomUUID();
            String scrollsFolder = "testScrolls_" + UUID.randomUUID();

            // Root admin
            rootAdmin = new User("root123") {
                @Override public boolean getAdmin() { return true; }
                @Override public String getUsername() { return "rootAdmin"; }
                @Override public String getUserId() { return "root123"; }
            };
            rootAdmin.createSelfDir();

            // Normal user
            normalUser = new User("user123") {
                @Override public boolean getAdmin() { return false; }
                @Override public String getUsername() { return "normalUser"; }
                @Override public String getUserId() { return "user123"; }
            };
            normalUser.createSelfDir();

            scrollManager = new ScrollManager(scrollsFolder);

            adminManager = new AdminManager(usersFolder, rootAdmin, scrollManager);

            adminManager.createUser(normalUser.getUserId(), normalUser.getUsername(), "password", false);

            logManager = new FakeEventLogManager();
            EventLogManager.setInstanceForTesting(logManager);

            proxy = new AdminManagerProxy(adminManager, rootAdmin);

        } catch (IOException e) {
            throw new RuntimeException("Setup failed", e);
        }
    }

    @Test
    void testCreateUser() {
        boolean created = proxy.createUser("user1", "User One", "password1", false);
        assertTrue(created);

        User u = adminManager.getUser("user1");
        assertNotNull(u);
        assertEquals("User One", u.getUsername());

        assertEquals(1, logManager.getLoggedEvents().size());
    }

    @Test
    void testDeleteUser() {
        proxy.createUser("user2", "User Two", "pw2", false);
        User u = adminManager.getAllUsers().stream().filter(us -> us.getUserId().equals("user2")).findFirst().orElse(null);
        assertNotNull(u);

        boolean deleted = proxy.deleteUser("user2");
        assertTrue(deleted);
        assertNull(adminManager.getUser("user2"));
    }

    @Test
    void testUpdateAdminStatus() {
        proxy.createUser("user3", "User Three", "pw3", false);
        boolean updated = proxy.updateAdminStatus("user3", true);
        assertTrue(updated);

        User u = adminManager.getUser("user3");
        assertNotNull(u);
        assertTrue(u.getAdmin());
    }

    @Test
    void testGetAllUsers() {
        proxy.createUser("user4", "User Four", "pw4", false);
        proxy.createUser("user5", "User Five", "pw5", false);

        List<User> users = proxy.getAllUsers();
        assertEquals(3, users.size()); 
    }

    static class FakeAdminManager extends AdminManager {
        private final Map<String, User> users = new HashMap<>();
        private final User currentUser;

        public FakeAdminManager(User currentUser) {
            super("unused", currentUser, new ScrollManager("unused"));
            this.currentUser = currentUser;
            users.put(currentUser.getUserId(), currentUser);
        }

        @Override
        public List<User> getAllUsers() {
            if (!currentUser.getAdmin()) throw new SecurityException();
            return new ArrayList<>(users.values());
        }

        @Override
        public User getUser(String userId) {
            return users.get(userId);
        }

        @Override
        public boolean createUser(String userId, String username, String password, boolean isAdmin) {
            if (!currentUser.getAdmin()) throw new SecurityException();
            if (users.containsKey(userId)) return false;

            User u = new User(userId) {
                @Override public String getUsername() { return username; }
                @Override public boolean getAdmin() { return isAdmin; }
                @Override public String getUserId() { return userId; }
            };
            users.put(userId, u);
            return true;
        }

        @Override
        public boolean deleteUser(String userId) {
            if (!currentUser.getAdmin()) throw new SecurityException();
            return users.remove(userId) != null;
        }

        @Override
        public boolean updateAdminStatus(String userId, boolean isAdmin) {
            if (!currentUser.getAdmin()) throw new SecurityException();
            User u = users.get(userId);
            if (u == null) return false;

            User updated = new User(u.getUserId()) {
                @Override public String getUsername() { return u.getUsername(); }
                @Override public boolean getAdmin() { return isAdmin; }
            };
            users.put(userId, updated);
            return true;
        }

        @Override
        public Map<String, String> viewScrollStats() {
            return new HashMap<>(); 
        }
    }
    
    static class FakeEventLogManager extends EventLogManager {
        private final List<String> loggedEvents = new ArrayList<>();

        @Override
        public void log(String userId, String username, String action, String details) {
            loggedEvents.add(String.format("%s:%s:%s:%s", userId, username, action, details));
        }

        public List<String> getLoggedEvents() {
            return loggedEvents;
        }
    }

    @Test
    void testCreateUserDenied() {
        User nonAdmin = new User("nonadmin") {
            @Override public boolean getAdmin() { return false; }
            @Override public String getUsername() { return "nonAdmin"; }
            @Override public String getUserId() { return "nonadmin"; }
        };

        FakeAdminManager restricted = new FakeAdminManager(nonAdmin);
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        AdminManagerProxy deniedProxy = new AdminManagerProxy(restricted, nonAdmin);
        boolean result = deniedProxy.createUser("x", "y", "z", false);

        assertEquals(false, result);
        assertTrue(fakeLog.getLoggedEvents().get(0).contains("ADMIN_CREATE_USER_DENIED"));
    }

    @Test
    void testDeleteUserDenied() {
        User nonAdmin = new User("nonadmin") {
            @Override public boolean getAdmin() { return false; }
            @Override public String getUsername() { return "nonAdmin"; }
            @Override public String getUserId() { return "nonadmin"; }
        };

        FakeAdminManager restricted = new FakeAdminManager(nonAdmin);
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        AdminManagerProxy deniedProxy = new AdminManagerProxy(restricted, nonAdmin);
        boolean result = deniedProxy.deleteUser("user1");

        assertEquals(false, result);
        assertTrue(fakeLog.getLoggedEvents().get(0).contains("ADMIN_DELETE_USER_DENIED"));
    }

    @Test
    void testUpdateAdminStatusDenied() {
        User nonAdmin = new User("nonadmin") {
            @Override public boolean getAdmin() { return false; }
            @Override public String getUsername() { return "nonAdmin"; }
            @Override public String getUserId() { return "nonadmin"; }
        };

        FakeAdminManager restricted = new FakeAdminManager(nonAdmin);
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        AdminManagerProxy deniedProxy = new AdminManagerProxy(restricted, nonAdmin);
        boolean result = deniedProxy.updateAdminStatus("user1", true);

        assertEquals(false, result);
        assertTrue(fakeLog.getLoggedEvents().get(0).contains("ADMIN_UPDATE_ROLE_DENIED"));
    }

    @Test
    void testGetAllUsersDenied() {
        User nonAdmin = new User("nonadmin") {
            @Override public boolean getAdmin() { return false; }
            @Override public String getUsername() { return "nonAdmin"; }
            @Override public String getUserId() { return "nonadmin"; }
        };

        FakeAdminManager restricted = new FakeAdminManager(nonAdmin);
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        AdminManagerProxy deniedProxy = new AdminManagerProxy(restricted, nonAdmin);
        var result = deniedProxy.getAllUsers();

        assertTrue(result.isEmpty());
        assertTrue(fakeLog.getLoggedEvents().get(0).contains("ADMIN_VIEW_USERS_DENIED"));
    }

    @Test
    void testViewScrollStatsDeniedNonAdmin() {
        User nonAdmin = new User("userX") {
            @Override public boolean getAdmin() { return false; }
            @Override public String getUsername() { return "nonAdmin"; }
            @Override public String getUserId() { return "userX"; }
        };

        FakeAdminManager fakeManager = new FakeAdminManager(nonAdmin);
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        AdminManagerProxy proxy = new AdminManagerProxy(fakeManager, nonAdmin);
        proxy.viewScrollStats(nonAdmin);

        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("VIEW_SCROLL_STATS_FAILED")));
    }

    @Test
    void testViewScrollStatsAdminManagerNull() {
        User admin = new User("admin") {
            @Override public boolean getAdmin() { return true; }
            @Override public String getUsername() { return "admin"; }
            @Override public String getUserId() { return "admin"; }
        };

        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog); 

        AdminManagerProxy proxyWithNull = new AdminManagerProxy(null, admin);

        proxyWithNull.viewScrollStats(admin);

        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("VIEW_SCROLL_STATS_FAILED")));
    }

    static class FailingAdminManager extends AdminManager {
        private final User currentUser;

        public FailingAdminManager(User currentUser) {
            super("unused", currentUser, new ScrollManager("unused"));
            this.currentUser = currentUser;
        }

        @Override
        public Map<String, String> viewScrollStats() {
            throw new RuntimeException("Simulated failure");
        }
    }    

    @Test
    void testViewScrollStatsThrowsException() {
        User admin = new User("admin123") {
            @Override public boolean getAdmin() { return true; }
            @Override public String getUsername() { return "adminUser"; }
            @Override public String getUserId() { return "admin123"; }
        };

        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        FailingAdminManager failingManager = new FailingAdminManager(admin);
        AdminManagerProxy proxyWithFail = new AdminManagerProxy(failingManager, admin);

        proxyWithFail.viewScrollStats(admin);

        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("VIEW_SCROLL_STATS_FAILED")
                        && s.contains("Simulated failure")));
    }

    @Test
    void testViewAsUserSuccess() {
        AdminManagerProxy proxy = new AdminManagerProxy(adminManager, rootAdmin);

        User target = proxy.viewAsUser(normalUser.getUserId());

        assertNotNull(target);
        assertEquals(normalUser.getUserId(), target.getUserId());
    }

    @Test
    void testViewAsUserFail() {
        AdminManagerProxy proxy = new AdminManagerProxy(adminManager, rootAdmin);

        User target = proxy.viewAsUser("nonexistent");

        assertNull(target);
    }

    @Test
    void testViewAsGuestSuccess() throws ScrollAlreadyExistsException {
        scrollManager.createScroll("scroll1");
        AdminManagerProxy proxy = new AdminManagerProxy(adminManager, rootAdmin);

        ScrollManager.ScrollTextPreview preview = proxy.viewAsGuest("scroll1");

        assertNotNull(preview);
        assertEquals("scroll1", preview.scrollId);
    }

    @Test
    void testViewAsGuestFail() {
        AdminManagerProxy proxy = new AdminManagerProxy(adminManager, rootAdmin);

        ScrollManager.ScrollTextPreview preview = proxy.viewAsGuest("nonexistent");

        assertNull(preview);
    }


}
