import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import fsam.FilesystemMemory;

import java.lang.System;

public class UserManagerTest {
    // Utility to delete dir and its contents
    private static void cleanFiles(File f)
    {
        File[] subFiles = f.listFiles();
        if (subFiles != null)
        {
            for (File subF : subFiles)
            {
                cleanFiles(subF);
            }
        }
        f.delete();
    }

    private static UserManager testManager;

    @BeforeAll
    public static void createEnv()
    {
        File tmpDir = new File("src/test/resources/fsam_testdata/userManagerTmp/");
        tmpDir.mkdirs();

        testManager = new UserManager("src/test/resources/fsam_testdata/userManagerTmp");
    }

    @AfterAll
    public static void cleanEnv()
    {
        // Remove tmp resource dir used to test user manager
        cleanFiles(new File("src/test/resources/fsam_testdata/userManagerTmp/"));
    }

    @Test
    public void ensureManagerExists()
    {
        assertTrue(testManager.exists());
    }

    @Test
    public void testGetUserDoesntExist()
    {
        assertNull(testManager.getUser("doesn't exist"));
    }

    @Test
    public void testCreateAndGetUser()
    {
        assertDoesNotThrow(() -> testManager.createUser("testUser"));

        assertNotNull(testManager.getUser("testUser"));
    }

    @Test
    public void testCreateUserExists()
    {
        assertDoesNotThrow(() -> testManager.createUser("testExistsUser"));

        assertThrows(UserAlreadyExistsException.class, () -> testManager.createUser("testExistsUser"));
    }

    @Test
    public void testChangeUserId()
    {
        assertDoesNotThrow(() -> testManager.createUser("testChangeUser"));
        assertNotNull(testManager.getUser("testChangeUser"));
        assertDoesNotThrow(() -> testManager.changeUserId("testChangeUser", "otherUser"));
        assertNull(testManager.getUser("testChangeUser"));
        assertNotNull(testManager.getUser("otherUser"));
    }

    @Test
    public void testChangeUserIdDoesntExist()
    {
        assertThrows(UserDoesNotExistException.class, () -> testManager.changeUserId("does not exist", "otherUser"));
    }

    @Test
    public void testChangeUserIdOverlap()
    {
        assertDoesNotThrow(() -> testManager.createUser("user1"));
        assertDoesNotThrow(() -> testManager.createUser("user2"));

        assertThrows(UserAlreadyExistsException.class, () -> testManager.changeUserId("user1", "user2"));
    }

    @Test
    public void testGetAllUsers()
    {
        assertDoesNotThrow(() -> testManager.createUser("getAllTestUser"));

        boolean foundTestUser = false;

        for (User u : testManager.getAllUsers())
        {
            if (u.getUserId().equals("getAllTestUser"))
            {
                foundTestUser = true;
            }
        }

        assertTrue(foundTestUser);
    }

    @Test
    public void testFindUserByUsernameMatch()
    {
        assertDoesNotThrow(() -> testManager.createUser("findUser"));

        User findUser = testManager.getUser("findUser");

        findUser.setUsername("find this username");

        assertNotNull(testManager.findUserByUsername("find this username"));
    }

    @Test
    public void testFindUserByUsernameNoMatch()
    {
        assertNull(testManager.findUserByUsername("no user with this name"));
    }

    @Test
    public void testSetPasswordAndCheckPassword() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("pwUser1"));
        User u = testManager.getUser("pwUser1");
        assertNotNull(u);

        assertTrue(testManager.setPassword(u, "Secret123!"));
        assertTrue(testManager.checkPassword(u, "Secret123!"));
        assertFalse(testManager.checkPassword(u, "WrongPass"));

        // Fields populated
        assertNotNull(u.getPasswordHash());
        assertNotNull(u.getPasswordSalt());
        assertNotNull(u.getPasswordAlgo());
        assertNotNull(u.getPasswordIters());
    }

    @Test
    public void testSamePasswordDifferentUsersDifferentHashAndSalt() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("pwUserA"));
        assertDoesNotThrow(() -> testManager.createUser("pwUserB"));

        User a = testManager.getUser("pwUserA");
        User b = testManager.getUser("pwUserB");
        assertNotNull(a);
        assertNotNull(b);

        assertTrue(testManager.setPassword(a, "SamePass"));
        assertTrue(testManager.setPassword(b, "SamePass"));

        // Different salts -> different hashes
        assertNotEquals(a.getPasswordSalt(), b.getPasswordSalt(), "Salts should differ");
        assertNotEquals(a.getPasswordHash(), b.getPasswordHash(), "Hashes should differ");
    }

    @Test
    public void testChangePasswordRotatesSaltAndInvalidatesOld() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("pwUser2"));
        User u = testManager.getUser("pwUser2");
        assertNotNull(u);

        assertTrue(testManager.setPassword(u, "OldPass1!"));
        String oldSalt = u.getPasswordSalt();
        String oldHash = u.getPasswordHash();

        assertTrue(testManager.setPassword(u, "NewPass2!"));
        String newSalt = u.getPasswordSalt();
        String newHash = u.getPasswordHash();

        assertNotEquals(oldSalt, newSalt, "Salt should rotate on password change");
        assertNotEquals(oldHash, newHash, "Hash should change on password change");

        assertTrue(testManager.checkPassword(u, "NewPass2!"));
        assertFalse(testManager.checkPassword(u, "OldPass1!"));
    }

    @Test
    public void testCheckPasswordReturnsFalseIfNoPasswordSet() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("noPwYet"));
        User u = testManager.getUser("noPwYet");
        assertNotNull(u);

        // No setPassword call yet → should be false
        assertFalse(testManager.checkPassword(u, "anything"));
    }

    @Test
    public void testAlgoAndItersStoredAsExpected() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("algoUser"));
        User u = testManager.getUser("algoUser");
        assertNotNull(u);

        assertTrue(testManager.setPassword(u, "Aussie#Koala7"));
        assertEquals("PBKDF2WithHmacSHA256", u.getPasswordAlgo());
        assertEquals(120000, u.getPasswordIters());

        // Sanity: stored hash decodes to 32 bytes (256 bits)
        byte[] hashBytes = security.PasswordHasher.fromBase64(u.getPasswordHash());
        assertEquals(32, hashBytes.length);
    }

    @Test
    public void testCheckPasswordReturnsFalseWhenHashIsEmpty() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("noHashUser"));
        User u = testManager.getUser("noHashUser");
        assertNotNull(u);

        // Set some fields but leave hash empty
        assertTrue(u.setPasswordSalt(security.PasswordHasher.toBase64(security.PasswordHasher.generateSalt())));
        assertTrue(u.setPasswordAlgo("PBKDF2WithHmacSHA256"));
        assertTrue(u.setPasswordIters(120000));
        assertTrue(u.setPasswordHash(""));  // empty

        assertFalse(testManager.checkPassword(u, "anything"));
    }

    @Test
    public void testCheckPasswordReturnsFalseWhenSaltMissing() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("noSaltUser"));
        User u = testManager.getUser("noSaltUser");
        assertNotNull(u);

        // Only set hash/iters/algo – no salt
        assertTrue(u.setPasswordHash(security.PasswordHasher.toBase64(new byte[32])));
        assertTrue(u.setPasswordAlgo("PBKDF2WithHmacSHA256"));
        assertTrue(u.setPasswordIters(120000));

        assertFalse(testManager.checkPassword(u, "anything"));
    }

    @Test
    public void testCheckPasswordReturnsFalseWhenIterationsMissing() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("noIterUser"));
        User u = testManager.getUser("noIterUser");
        assertNotNull(u);

        // Set salt/hash/algo but NOT iterations
        byte[] salt = security.PasswordHasher.generateSalt();
        assertTrue(u.setPasswordSalt(security.PasswordHasher.toBase64(salt)));
        assertTrue(u.setPasswordHash(security.PasswordHasher.toBase64(new byte[32])));
        assertTrue(u.setPasswordAlgo("PBKDF2WithHmacSHA256"));

        assertFalse(testManager.checkPassword(u, "anything"));
    }

    @Test
    public void testCheckPasswordReturnsFalseIfSaltBase64IsInvalid() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("badB64SaltUser"));
        User u = testManager.getUser("badB64SaltUser");
        assertNotNull(u);

        // Corrupt salt so Base64 decode throws IllegalArgumentException (caught -> false)
        assertTrue(u.setPasswordSalt("%%%NOT_BASE64%%%"));
        assertTrue(u.setPasswordHash(security.PasswordHasher.toBase64(new byte[32])));
        assertTrue(u.setPasswordAlgo("PBKDF2WithHmacSHA256"));
        assertTrue(u.setPasswordIters(120000));

        assertFalse(testManager.checkPassword(u, "anything"));
    }

    @Test
    public void testCheckPasswordReturnsFalseIfIterationsAreInvalid() throws Exception {
        assertDoesNotThrow(() -> testManager.createUser("badIterUser"));
        User u = testManager.getUser("badIterUser");
        assertNotNull(u);

        // Negative iterations -> PBKDF2 throws; caught -> false
        byte[] salt = security.PasswordHasher.generateSalt();
        assertTrue(u.setPasswordSalt(security.PasswordHasher.toBase64(salt)));
        assertTrue(u.setPasswordHash(security.PasswordHasher.toBase64(new byte[32])));
        assertTrue(u.setPasswordAlgo("PBKDF2WithHmacSHA256"));
        assertTrue(u.setPasswordIters(-5));  // invalid

        assertFalse(testManager.checkPassword(u, "anything"));
    }
}
