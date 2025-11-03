import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AdminManagerTest {

    @TempDir
    File tempDir;

    private User adminUser;
    private User normalUser;
    private UserManager stubUserManager;
    private AdminManager adminManager;
    private AdminManager nonAdminManager;

    @BeforeEach
    void setUp() throws IOException {
        // Admin user stub
        adminUser = new User(tempDir.getAbsolutePath() + "/admin") {
            @Override
            public boolean getAdmin() { return true; }
            @Override
            public String getUserId() { return "admin"; }
        };
        adminUser.createSelfDir();

        // Normal user stub
        normalUser = new User(tempDir.getAbsolutePath() + "/user1") {
            @Override
            public boolean getAdmin() { return false; }
            @Override
            public String getUserId() { return "user1"; }
        };
        normalUser.createSelfDir();

        // Stub UserManager
        stubUserManager = new UserManager(tempDir.getAbsolutePath()) {
            @Override
            public User getUser(String id) {
                if (id.equals("admin")) return adminUser;
                if (id.equals("user1")) return normalUser;
                return null;
            }

            @Override
            public User createUser(String userId) {
                try {
                    File dir = new File(tempDir, userId);
                    dir.mkdir();
                    return new User(dir.getAbsolutePath()) {
                        @Override
                        public String getUserId() { return userId; }
                        @Override
                        public boolean setUsername(String u) { return true; }
                        @Override
                        public boolean setPasswordHash(String p) { return true; }
                        @Override
                        public boolean setAdmin(boolean a) { return true; }
                    };
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public List<User> getAllUsers() {
                return List.of(adminUser, normalUser);
            }
        };
        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());

        adminManager = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);
        nonAdminManager = new AdminManager(tempDir.getAbsolutePath(), normalUser, scrollManager);

        TestUtils.setField(adminManager, "userManager", stubUserManager);
        TestUtils.setField(nonAdminManager, "userManager", stubUserManager);
    }

    @Test
    void testParseBooleanInputVariants() {
        assertTrue(adminManager.parseBooleanInput("true"));
        assertTrue(adminManager.parseBooleanInput("YES"));
        assertTrue(adminManager.parseBooleanInput("1"));
        assertFalse(adminManager.parseBooleanInput("no"));
        assertFalse(adminManager.parseBooleanInput(null));
        assertFalse(adminManager.parseBooleanInput("random"));
    }

    @Test
    void testGetAllUsersThrowsForNonAdmin() {
        assertThrows(SecurityException.class, () -> nonAdminManager.getAllUsers());
    }

    @Test
    void testGetAllUsersSuccess() {
        List<User> users = adminManager.getAllUsers();
        assertEquals(2, users.size());
        assertTrue(users.contains(adminUser));
        assertTrue(users.contains(normalUser));
    }

    @Test
    void testCreateUserBlankId() {
        assertFalse(adminManager.createUser("  ", "x", "p", false));
    }

    @Test
    void testCreateUserInvalidId() {
        assertFalse(adminManager.createUser("bad id!", "x", "p", false));
    }

    @Test
    void testCreateUserAlreadyExists() {
        assertFalse(adminManager.createUser("admin", "x", "p", false));
    }

    @Test
    void testCreateUserNullReturnFromManager() {
        UserManager failingManager = new UserManager(tempDir.getAbsolutePath()) {
            @Override
            public User getUser(String id) { return null; }

            @Override
            public User createUser(String id) { return null; }
        };

        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);
        TestUtils.setField(am, "userManager", failingManager);

        assertFalse(am.createUser("u1", "x", "p", false));
    }


    @Test
    void testCreateUserSuccess() throws Exception {
        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);

        boolean result = am.createUser("newUser", "userName", "pw123", false);
        assertTrue(result, "Expected createUser() to return true for valid user");

        File userDir = new File(tempDir, "newUser");
        assertTrue(userDir.exists() && userDir.isDirectory(),
            "Expected a directory to be created for new user");
    }


    @Test
    void testDeleteUserSelfFails() {
        assertFalse(adminManager.deleteUser("admin"));
    }

    @Test
    void testDeleteUserNotFound() {
        assertFalse(adminManager.deleteUser("ghost"));
    }

    @Test
    void testDeleteRootAdminBlocked() {
        assertFalse(adminManager.deleteUser("root"));
    }

    @Test
    void testDeleteUserFilesAndDir() throws IOException {
        String uid = "u5";
        File dir = new File(tempDir, uid);
        dir.mkdir();
        File f = new File(dir, "file.txt");
        Files.writeString(f.toPath(), "data");

        User u = new User(dir.getAbsolutePath()) {
            @Override public String getUserId() { return uid; }
        };

        UserManager localManager = new UserManager(tempDir.getAbsolutePath()) {
            @Override public User getUser(String id) { return u; }
        };
        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);
        TestUtils.setField(am, "userManager", localManager);

        assertTrue(am.deleteUser(uid));
        assertFalse(dir.exists());
    }

    @Test
    void testUpdateAdminStatusRootBlocked() {
        assertFalse(adminManager.updateAdminStatus("root", true));
    }

    @Test
    void testUpdateAdminStatusUserNotFound() {
        assertFalse(adminManager.updateAdminStatus("ghost", true));
    }

    @Test
    void testUpdateAdminStatusSuccess() {
        User u = new User(tempDir.getAbsolutePath() + "/u7") {
            @Override public boolean setAdmin(boolean a) { return true; }
        };
        UserManager localManager = new UserManager(tempDir.getAbsolutePath()) {
            @Override public User getUser(String id) { return u; }
        };
        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);
        TestUtils.setField(am, "userManager", localManager);

        assertTrue(am.updateAdminStatus("u7", true));
    }

    @Test
    void testCreateUserRollback() {
        UserManager rollbackManager = new UserManager(tempDir.getAbsolutePath()) {
            @Override
            public User getUser(String id) { return null; }

            @Override
            public User createUser(String userId) {
                try {
                    File dir = new File(tempDir, userId);
                    dir.mkdir();
                    return new User(dir.getAbsolutePath()) {
                        @Override
                        public String getUserId() { return userId; }
                        @Override
                        public boolean setUsername(String u) { return false; }
                        @Override
                        public boolean setPasswordHash(String p) { return true; }
                        @Override
                        public boolean setAdmin(boolean a) { return true; }
                    };
                } catch (Exception e) {
                    return null;
                }
            }
        };

        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);
        TestUtils.setField(am, "userManager", rollbackManager);

        assertFalse(am.createUser("rollbackUser", "name", "pw", false));

        File dir = new File(tempDir, "rollbackUser");
        assertFalse(dir.exists(), "Directory should be deleted during rollback");
    }

    @Test
    void testCreateUserReturnsNull() {
        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager failingManager = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager) {
            @Override
            public boolean createUser(String userId, String username, String passwordHash, boolean isAdmin) {
                return false;
            }
        };

        boolean result = failingManager.createUser("u_null", "name", "pw", false);
        assertFalse(result);
    }

    @Test
    void testCreateUserSetterFailsRollback() throws IOException {
        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager rollbackManager = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager) {
            @Override
            public boolean createUser(String userId, String username, String passwordHash, boolean isAdmin) {
                try {
                    File dir = new File(tempDir, userId);
                    dir.mkdir();
                    User u = new User(dir.getAbsolutePath()) {
                        @Override
                        public String getUserId() { return userId; }
                        @Override
                        public boolean setUsername(String s) { return false; }
                        @Override
                        public boolean setPasswordHash(String s) { return true; }
                        @Override
                        public boolean setAdmin(boolean b) { return true; }
                    };
                    boolean buh = u.setUsername(username) && u.setPasswordHash(passwordHash) && u.setAdmin(isAdmin);
                    if (!buh) {
                        File[] files = dir.listFiles();
                        if (files != null) for (File f : files) f.delete();
                        dir.delete();
                        return false;
                    }
                    return true;
                } catch (Exception e) { return false; }
            }
        };

        boolean result = rollbackManager.createUser("rollbackUser", "name", "pw", false);
        assertFalse(result);

        File dir = new File(tempDir, "rollbackUser");
        assertFalse(dir.exists(), "Directory should be deleted during rollback");
    }

    @Test
    void testDeleteUserFileDeletionFails() throws IOException {
        File userDir = new File(tempDir, "lockedUser");
        userDir.mkdir();
        File badFile = new File(userDir, "fail.txt");
        badFile.createNewFile();

        User u = new User(userDir.getAbsolutePath()) {
            @Override
            public String getUserId() { return "lockedUser"; }
            @Override
            public File getFileObj() { return userDir; }
        };

        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager) {
            @Override
            public User getUser(String userId) {
                return "lockedUser".equals(userId) ? u : null;
            }

            @Override
            public boolean deleteUser(String userId) {
                File dir = u.getFileObj();
                File[] files = dir.listFiles();
                boolean success = true;
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().equals("fail.txt")) {
                            success = false;
                            System.err.println("Simulated failure deleting file: " + f.getName());
                        } else {
                            f.delete();
                        }
                    }
                }
                if (!success || !dir.delete()) success = false;
                return success;
            }
        };

        boolean result = am.deleteUser("lockedUser");
        assertFalse(result, "deleteUser should fail if file deletion fails");
    }

    @Test
    void testCreateUserTriggersCatchBlock() throws Exception {
        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.toString(), adminUser, scrollManager ) {
            @Override
            public boolean createUser(String userId, String username, String passwordHash, boolean isAdmin) {
                try {
                    throw new IOException("Simulated IO failure");
                } catch (Exception e) {
                    System.err.println("Error creating user: " + e.getMessage());
                    return false;
                }
            }
        };

        boolean result = am.createUser("crashUser", "crash", "pw", false);
        assertFalse(result, "Expected false due to caught exception in createUser");
    }

    @Test
    void testCreateUserRollbackFailure() {
        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);
        boolean result = am.createUser("invalid user id!", "name", "hash", false);
        assertFalse(result, "createUser should fail with invalid user ID");
    }

    @Test
    void testCreateUserExceptionPath() throws IOException {
        File existingFile = new File(tempDir, "newUser");
        existingFile.createNewFile();

        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);

        boolean result = am.createUser("newUser", "name", "pw123", false);

        assertFalse(result, "createUser should return false if an exception occurs");
    }

    @Test
    void testDeleteUserAllFailureBranches() throws IOException {
        User adminRoot = new User(tempDir.toString() + "/adminRoot") {
            @Override
            public boolean getAdmin() { return true; }
            @Override
            public String getUserId() { return "adminRoot"; }
        };
        adminRoot.createSelfDir();

        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.toString(), adminRoot, scrollManager) {
            @Override
            public boolean deleteUser(String userId) {
                if ("failUser".equals(userId)) return false;
                return super.deleteUser(userId);
            }
        };

        assertFalse(am.deleteUser("root"));
        assertFalse(am.deleteUser("nonExistentUser"));

        File failUserDir = new File(tempDir, "failUser");
        failUserDir.mkdir();
        assertFalse(am.deleteUser("failUser"));

        failUserDir.delete();
    }
    private static class TestUtils {
        static void setField(Object target, String fieldName, Object value) {
            try {
                var f = target.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class UnDeletableFile extends File {
        public UnDeletableFile(String pathname) { super(pathname); }
        @Override
        public boolean delete() { return false; }
    }

    @Test
    void testDeleteUserFileDeletionFailsRealMethod() {
        File userDir = new File(tempDir, "lockedUserDir");
        userDir.mkdir();
        User u = new User(userDir.getAbsolutePath()) {
            @Override public String getUserId() { return "lockedUserDir"; }
            @Override
            public File getFileObj() {
                return new File(userDir.getAbsolutePath()) {
                    @Override public boolean delete() { return false; }
                    @Override public File[] listFiles() {
                        return new File[]{ new File(userDir, "fail.txt") {
                            @Override public boolean delete() { return false; }
                        }};
                    }
                };
            }
        };

        UserManager localManager = new UserManager(tempDir.getAbsolutePath()) {
            @Override public User getUser(String id) { return u; }
        };

        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);
        TestUtils.setField(am, "userManager", localManager);

        assertFalse(am.deleteUser("lockedUserDir"),
            "deleteUser should fail if file cannot be deleted");
    }


    class UnDeletableDir extends File {
        public UnDeletableDir(String pathname) { super(pathname); }
        @Override
        public boolean delete() { return false; }
    }

    @Test
    void testDeleteUserDirectoryDeletionFailsRealMethod() throws IOException {
        File userDir = new UnDeletableDir(tempDir.getAbsolutePath() + "/lockedDir");
        userDir.mkdir();

        User u = new User(userDir.getAbsolutePath()) {
            @Override public String getUserId() { return "lockedDir"; }
            @Override public File getFileObj() { return userDir; }
        };

        UserManager localManager = new UserManager(tempDir.getAbsolutePath()) {
            @Override public User getUser(String id) { return u; }
        };

        ScrollManager scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        AdminManager am = new AdminManager(tempDir.getAbsolutePath(), adminUser, scrollManager);
        TestUtils.setField(am, "userManager", localManager);

        assertFalse(am.deleteUser("lockedDir"), "deleteUser should fail if directory cannot be deleted");
    }


}
