import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminMenuTest {

    private static Path tempDir;
    private User rootAdmin;
    private User normalUser;
    private AdminManager adminManager;
    private ScrollManager scrollManager;

    @BeforeAll
    static void setupAll() throws IOException {
        tempDir = Files.createTempDirectory("test_users");
    }

    @AfterAll
    static void teardownAll() throws IOException {
        deleteDirectory(tempDir.toFile());
    }

    @BeforeEach
    void setup() throws IOException {
        rootAdmin = new User(tempDir.resolve("root").toString());
        rootAdmin.createSelfDir();
        rootAdmin.setUsername("root");
        rootAdmin.setPasswordHash("rootpass");
        rootAdmin.setAdmin(true);

        normalUser = new User(tempDir.resolve("user").toString());
        normalUser.createSelfDir();
        normalUser.setUsername("user");
        normalUser.setPasswordHash("userpass");
        normalUser.setAdmin(false);

        scrollManager = new ScrollManager(tempDir.toString());
        adminManager = new AdminManager(tempDir.toString(), rootAdmin, scrollManager);
        adminManager.createUser(normalUser.getUserId(), normalUser.getUsername(), normalUser.getPasswordHash(), false);
    }

    @AfterEach
    void teardown() throws IOException {
        for (File f : tempDir.toFile().listFiles()) {
            deleteDirectory(f);
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) deleteDirectory(f);
        }
        dir.delete();
    }

    private void runMenuWithInput(String input) {
        InputStream sysInBackup = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        // Stub UserManager that stores users and updates them
        UserManager stubUserManager = new UserManager(tempDir.toString()) {
            @Override
            public User createUser(String userId) {
                if (userId.isBlank()) return null;
                File dir = new File(tempDir.toFile(), userId);
                if (!dir.exists()) dir.mkdir();
                return new User(dir.getAbsolutePath()) {
                    @Override
                    public String getUserId() { return userId; }

                    @Override
                    public boolean setUsername(String s) { super.setUsername(s); return true; }
                    @Override
                    public boolean setAdmin(boolean a) { super.setAdmin(a); return true; }
                    @Override
                    public boolean setPasswordHash(String s) { super.setPasswordHash(s); return true; }
                };
            }

            @Override
            public User getUser(String id) {
                File dir = new File(tempDir.toFile(), id);
                if (!dir.exists()) return null;
                return new User(dir.getAbsolutePath()) {
                    @Override
                    public String getUserId() { return id; }
                };
            }
        };

        scrollManager = new ScrollManager(tempDir.toString());
        AdminMenu adminMenu = new AdminMenu(adminManager, stubUserManager, scrollManager, rootAdmin);
        adminMenu.show();

        System.setIn(sysInBackup);
    }

    @Test
    void testCreateUserYes() {
        String input = "2\nuser1\nuser1name\npass1\nyes\n8\n";
        runMenuWithInput(input);

        var user = adminManager.getUser("user1");
        assertNotNull(user);
        assertEquals("user1name", user.getUsername());
        assertTrue(user.getAdmin());
    }

    @Test
    void testCreateUserY() {
        String input = "2\nuser2\nuser2name\npass2\ny\n8\n"; 
        runMenuWithInput(input);

        var user = adminManager.getUser("user2");
        assertNotNull(user);
        assertTrue(user.getAdmin());
    }

    @Test
    void testCreateUserTrue() {
        String input = "2\nuser3\nuser3name\npass3\ntrue\n8\n"; 
        runMenuWithInput(input);

        var user = adminManager.getUser("user3");
        assertNotNull(user);
        assertTrue(user.getAdmin());
    }

    @Test
    void testCreateUserBlankId() {
        String input = "2\n\n\n\n\n8\n"; 
        runMenuWithInput(input);

        assertEquals(2, adminManager.getAllUsers().size());
    }

    @Test
    void testDeleteUserConfirmYes() throws IOException {
        adminManager.createUser("user4", "user4name", "pass4", false);
        String input = "3\nuser4\nyes\n8\n";
        runMenuWithInput(input);
        assertNull(adminManager.getUser("user4"));
    }

    @Test
    void testDeleteUserConfirmNo() throws IOException {
        adminManager.createUser("user5", "user5name", "pass5", false);
        String input = "3\nuser5\nno\n8\n"; 
        runMenuWithInput(input);
        assertNotNull(adminManager.getUser("user5"));
    }

    @Test
    void testUpdateAdminStatusYes() throws IOException {
        adminManager.createUser("user6", "user6name", "pass6", false);
        String input = "4\nuser6\nyes\n8\n"; 
        runMenuWithInput(input);
        var user = adminManager.getUser("user6");
        assertTrue(user.getAdmin());
    }

    @Test
    void testUpdateAdminStatusNo() throws IOException {
        adminManager.createUser("user8", "user8name", "pass8", true);
        String input = "4\nuser8\nno\n8\n"; 
        runMenuWithInput(input);
        var user = adminManager.getUser("user8");
        assertFalse(user.getAdmin());
    }

    @Test
    void testViewAllUsers() {
        String input = "1\n8\n"; 
        runMenuWithInput(input);
    }

    @Test
    void testInvalidChoice() {
        String input = "99\n8\n"; 
        runMenuWithInput(input);
    }

    @Test
    void testUpdateAdminStatusBlankId() {
        String input = "4\n\n8\n"; 
        runMenuWithInput(input);
    }


    @Test
    void testViewEventLogsNoFilter() {
        String input = "no\n"; 
        InputStream sysInBackup = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        EventLogManager fakeManager = new EventLogManager() {
            @Override
            public List<String> getAllLogs() {
                return java.util.List.of("userid=u1 user=user1 action=login");
            }
        };
        EventLogManager.setInstanceForTesting(fakeManager);

        AdminMenu adminMenu = new AdminMenu(adminManager, null, null, rootAdmin);
        adminMenu.viewEventLogs();

        System.setIn(sysInBackup);
        EventLogManager.setInstanceForTesting(null);
    }

    @Test
    void testViewEventLogsFilterByUserId() {
        String input = "yes\nu1\n\n"; 
        InputStream sysInBackup = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        EventLogManager fakeManager = new EventLogManager() {
            @Override
            public List<String> getAllLogs() {
                return java.util.List.of(
                    "userid=u1 user=user1 action=login",
                    "userid=u2 user=user2 action=logout"
                );
            }
        };
        EventLogManager.setInstanceForTesting(fakeManager);

        AdminMenu adminMenu = new AdminMenu(adminManager, null, null, rootAdmin);
        adminMenu.viewEventLogs();

        System.setIn(sysInBackup);
        EventLogManager.setInstanceForTesting(null);
    }

    @Test
    void testViewEventLogsFilterByUsername() {
        String input = "yes\n\nuser2\n"; 
        InputStream sysInBackup = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        EventLogManager fakeManager = new EventLogManager() {
            @Override
            public List<String> getAllLogs() {
                return java.util.List.of(
                    "userid=u1 user=user1 action=login",
                    "userid=u2 user=user2 action=logout"
                );
            }
        };
        EventLogManager.setInstanceForTesting(fakeManager);

        AdminMenu adminMenu = new AdminMenu(adminManager, null, null, rootAdmin);
        adminMenu.viewEventLogs();

        System.setIn(sysInBackup);
        EventLogManager.setInstanceForTesting(null);
    }

    @Test
    void testViewEventLogsFilterByUserIdAndUsername() {
        String input = "yes\nu1\nuser1\n"; 
        InputStream sysInBackup = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        EventLogManager fakeManager = new EventLogManager() {
            @Override
            public List<String> getAllLogs() {
                return java.util.List.of(
                    "userid=u1 user=user1 action=login",
                    "userid=u2 user=user2 action=logout"
                );
            }
        };
        EventLogManager.setInstanceForTesting(fakeManager);

        AdminMenu adminMenu = new AdminMenu(adminManager, null, null, rootAdmin);
        adminMenu.viewEventLogs();

        System.setIn(sysInBackup);
        EventLogManager.setInstanceForTesting(null);
    }

    @Test
    void testViewEventLogsFilterNoMatches() {
        String input = "yes\nu99\nuserX\n"; 
        InputStream sysInBackup = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        EventLogManager fakeManager = new EventLogManager() {
            @Override
            public List<String> getAllLogs() {
                return java.util.List.of(
                    "userid=u1 user=user1 action=login",
                    "userid=u2 user=user2 action=logout"
                );
            }
        };
        EventLogManager.setInstanceForTesting(fakeManager);

        AdminMenu adminMenu = new AdminMenu(adminManager, null, null, rootAdmin);
        adminMenu.viewEventLogs();

        System.setIn(sysInBackup);
        EventLogManager.setInstanceForTesting(null);
    }

    @Test
    void testViewScrollStats() {
        String input = "6\n8\n"; 
        runMenuWithInput(input);
    }

    @Test
    void testViewScrollStatsNonEmpty() throws IOException {
        try {
            scrollManager.createScroll("scroll1");
            scrollManager.createScroll("scroll2");
        } catch (ScrollAlreadyExistsException e) {
            e.printStackTrace();
        }

        String input = "6\n8\n"; 
        runMenuWithInput(input);
    }
}
