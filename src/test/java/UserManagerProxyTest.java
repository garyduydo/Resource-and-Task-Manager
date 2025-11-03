import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserManagerProxyTest {

    private FakeUserManager fakeUserManager;
    private FakeEventLogManager fakeLogManager;
    private UserManagerProxy proxy;
    private User fakeUser;

    static class FakeUserManager extends UserManager {
        private final java.util.Map<String, User> users = new java.util.HashMap<>();
        private boolean failPassword = false; 

        FakeUserManager() {
            super("testRoot"); 
        }

        public void setFailPassword(boolean fail) {
            this.failPassword = fail;
        }

        @Override
        public User createUser(String userId) {
            User u = new User(userId) {
                private String username = userId; 
                @Override public String getUserId() { return userId; }
                @Override public String getUsername() { return username; }
                @Override public boolean setUsername(String uname) { this.username = uname; return true; }
            };
            users.put(userId, u);
            return u;
        }

        @Override
        public boolean setPassword(User user, String password) {
            return !failPassword; 
        }

        @Override
        public User findUserByUsername(String username) {
            return users.values().stream()
                    .filter(u -> username.equals(u.getUsername()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean checkPassword(User user, String password) {
            return !failPassword; 
        }

        @Override
        public List<User> getAllUsers() {
            return new ArrayList<>(users.values());
        }

        @Override
        public boolean changeUserId(String oldId, String newId) {
            User u = users.remove(oldId);
            if (u == null) return false;
            users.put(newId, u);
            return true;
        }
    }

    static class FakeEventLogManager extends EventLogManager {
        private final List<String> logs = new ArrayList<>();

        @Override
        public void log(String userId, String username, String action, String details) {
            logs.add(String.format("%s|%s|%s|%s", userId, username, action, details));
        }

        List<String> getLogs() { return logs; }
    }

    @BeforeEach
    void setUp() {
        fakeUserManager = new FakeUserManager();
        fakeLogManager = new FakeEventLogManager();

        EventLogManager.setInstanceForTesting(fakeLogManager); 
        proxy = new UserManagerProxy(fakeUserManager); 

        fakeUser = new User("user1") {
            private String username = "user1name";
            @Override public String getUserId() { return "user1"; }
            @Override public String getUsername() { return username; }
            @Override public boolean setUsername(String uname) { this.username = uname; return true; }
        };
    }

    @Test
    void testCreateUserSuccess() throws UserAlreadyExistsException {
        fakeUserManager.setFailPassword(false);
        User u = proxy.createUser("u1", "uname", "pw");
        assertNotNull(u);
        assertTrue(fakeLogManager.getLogs().get(0).contains("CREATE_USER"));
    }

    @Test
    void testCreateUserPasswordFail() throws UserAlreadyExistsException {
        fakeUserManager.setFailPassword(true);
        User u = proxy.createUser("u1", "uname", "fail");
        assertNotNull(u);
        assertTrue(fakeLogManager.getLogs().get(0).contains("CREATE_USER_FAILED"));
    }

    @Test
    void testLoginSuccess() {
        fakeUserManager.setFailPassword(false);
        fakeUserManager.createUser("u1"); 
        User u = proxy.login("u1", "correct"); 
        assertNotNull(u);
        assertTrue(fakeLogManager.getLogs().get(0).contains("LOGIN_SUCCESS"));
    }

    @Test
    void testLoginWrongPassword() {
        fakeUserManager.setFailPassword(true);
        fakeUserManager.createUser("u1");
        User u = proxy.login("u1", "wrong");
        assertNull(u);
        assertTrue(fakeLogManager.getLogs().get(0).contains("LOGIN_FAILED"));
    }

    @Test
    void testLoginUserNotFound() {
        User u = proxy.login("no_user", "pw");
        assertNull(u);
        assertTrue(fakeLogManager.getLogs().get(0).contains("LOGIN_FAILED"));
    }

    @Test
    void testChangePasswordSuccess() {
        fakeUserManager.setFailPassword(false);
        boolean ok = proxy.changePassword(fakeUser, "newPass");
        assertTrue(ok);
        assertTrue(fakeLogManager.getLogs().get(0).contains("UPDATE_PASSWORD"));
    }

    @Test
    void testChangePasswordFail() {
        fakeUserManager.setFailPassword(true);
        boolean ok = proxy.changePassword(fakeUser, "fail");
        assertFalse(ok);
        assertTrue(fakeLogManager.getLogs().get(0).contains("UPDATE_PASSWORD_FAILED"));
    }

    @Test
    void testGetAllUsersSuccess() {
        fakeUserManager.createUser("a1");
        List<User> list = proxy.getAllUsers();
        assertFalse(list.isEmpty());
        assertTrue(fakeLogManager.getLogs().get(0).contains("VIEW_ALL_USERS"));
    }

    @Test
    void testChangeUserIdSuccess() {
        fakeUserManager.createUser("old");
        boolean ok = proxy.changeUserId("old", "new");
        assertTrue(ok);
        assertTrue(fakeLogManager.getLogs().get(0).contains("CHANGE_USER_ID"));
    }

    @Test
    void testChangeUserIdFail() {
        boolean ok = proxy.changeUserId("nope", "new");
        assertFalse(ok);
        assertTrue(fakeLogManager.getLogs().get(0).contains("CHANGE_USER_ID_FAILED"));
    }

    @Test
    void testUpdateProfileValidField() {
        boolean ok = proxy.updateProfile(fakeUser, "username", "newName");
        assertTrue(ok);
        assertTrue(fakeLogManager.getLogs().get(0).contains("UPDATE_PROFILE"));
    }

    @Test
    void testUpdateProfileInvalidField() {
        boolean ok = proxy.updateProfile(fakeUser, "unknown", "val");
        assertFalse(ok);
        assertTrue(fakeLogManager.getLogs().get(0).contains("UPDATE_PROFILE_FAILED"));
    }

    @Test
    void testLogoutLogsEvent() {
        proxy.logout(fakeUser);
        assertTrue(fakeLogManager.getLogs().get(0).contains("LOGOUT"));
    }

    @Test
    void testCreateUserThrowsException() throws UserAlreadyExistsException {
        UserManager throwingManager = new FakeUserManager() {
            @Override
            public User createUser(String userId) {
                throw new RuntimeException("Simulated failure");
            }
        };
        proxy = new UserManagerProxy(throwingManager);

        User u = proxy.createUser("uFail", "uname", "pw");
        assertNull(u);
        assertTrue(fakeLogManager.getLogs().get(0).contains("CREATE_USER_FAILED"));
    }

    @Test
    void testLoginThrowsException() {
        UserManager throwingManager = new FakeUserManager() {
            @Override
            public User findUserByUsername(String username) {
                throw new RuntimeException("Simulated login failure");
            }
        };
        proxy = new UserManagerProxy(throwingManager);

        User u = proxy.login("user", "pw");
        assertNull(u);
        assertTrue(fakeLogManager.getLogs().get(0).contains("LOGIN_ERROR"));
    }

    @Test
    void testUpdateProfileThrowsException() {
        User faultyUser = new User("faultyId") {
            @Override
            public boolean setUsername(String uname) {
                throw new RuntimeException("Simulated failure");
            }

            @Override
            public String getUserId() { return "faultyId"; }
            @Override
            public String getUsername() { return "faultyUser"; }
        };

        boolean result = proxy.updateProfile(faultyUser, "username", "newName");

        assertFalse(result, "updateProfile should return false when an exception occurs");

        assertFalse(fakeLogManager.getLogs().isEmpty(), "Log should not be empty");
        String logEntry = fakeLogManager.getLogs().get(0);
        assertTrue(logEntry.contains("UPDATE_PROFILE_FAILED"), "Log should indicate failure");
        assertTrue(logEntry.contains("Simulated failure"), "Log should contain exception message");
    }


    @Test
    void testChangePasswordThrowsException() {
        proxy = new UserManagerProxy(new FakeUserManager() {
            @Override
            public boolean setPassword(User user, String password) {
                throw new RuntimeException("Password fail");
            }
        });

        boolean ok = proxy.changePassword(fakeUser, "newPass");
        assertFalse(ok);
        assertTrue(fakeLogManager.getLogs().get(0).contains("CHANGE_PASSWORD_FAILED"));
    }

    @Test
    void testChangeUserIdThrowsException() {
        proxy = new UserManagerProxy(new FakeUserManager() {
            @Override
            public boolean changeUserId(String oldId, String newId) {
                throw new RuntimeException("Simulated exception");
            }
        });

        boolean ok = proxy.changeUserId("old", "new");
        assertFalse(ok);
        assertTrue(fakeLogManager.getLogs().get(0).contains("CHANGE_USER_ID_FAILED"));
    }

}
