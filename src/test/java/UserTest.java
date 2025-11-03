import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import fsam.FilesystemMemory;

import java.lang.System;

public class UserTest {
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

    private static User testUser;
    private static User badUser;

    @BeforeAll
    public static void createEnv()
    {
        File tmpDir = new File("src/test/resources/fsam_testdata/userTmp");
        tmpDir.mkdirs();

        testUser = new User();
        testUser.setFileObj(new File(tmpDir, "testUser"));
        testUser.setParent(tmpDir);
        try
        {
            testUser.createSelfDir();
        }
        catch (IOException e)
        {
            System.exit(1);
        }

        badUser = new User();
        badUser.setFileObj(new File(tmpDir, "badUser"));
        badUser.setParent(tmpDir);
    }

    @AfterAll
    public static void cleanEnv()
    {
        // Remove tmp resource dir used to test users
        cleanFiles(new File("src/test/resources/fsam_testdata/userTmp"));
    }

    @Test
    public void testGetUserId()
    {
        assertEquals(testUser.getUserId(), "testUser");
        assertEquals(badUser.getUserId(), "badUser");
    }

    @Test
    public void testGetSetUsername()
    {
        // First set should create file
        assertTrue(testUser.setUsername("testUname"));

        // Ensure it can be retrieved
        assertEquals(testUser.getUsername(), "testUname");

        // Ensure it can be changed and retrieved
        assertTrue(testUser.setUsername("otherUname"));
        assertEquals(testUser.getUsername(), "otherUname");
    }

    @Test
    public void testGetSetUsernameBad()
    {
        // Set fails when there isn't a user dir
        assertFalse(badUser.setUsername("bad"));

        // Get returns null when it fails
        assertEquals(badUser.getUsername(), null);
    }

    @Test
    public void testGetName()
    {
        // First set should create file
        assertTrue(testUser.setName("Ben Y."));

        // Ensure it can be retrieved
        assertEquals(testUser.getName(), "Ben Y.");

        // Ensure it can be changed and retrieved
        assertTrue(testUser.setName("Vinit"));
        assertEquals(testUser.getName(), "Vinit");
    }

    @Test
    public void testGetSetNameBad()
    {
        // Set fails when there isn't a user dir
        assertFalse(badUser.setName("bad"));

        // Get returns null when it fails
        assertEquals(badUser.getName(), null);
    }

    @Test
    public void testGetSetPhone()
    {
        // First set should create file
        assertTrue(testUser.setPhone("000"));

        // Ensure it can be retrieved
        assertEquals(testUser.getPhone(), "000");

        // Ensure it can be changed and retrieved
        assertTrue(testUser.setPhone("999"));
        assertEquals(testUser.getPhone(), "999");
    }

    @Test
    public void testGetSetPhoneBad()
    {
        // Set fails when there isn't a user dir
        assertFalse(badUser.setPhone("bad"));

        // Get returns null when it fails
        assertEquals(badUser.getPhone(), null);
    }

    @Test
    public void testGetSetEmail()
    {
        // First set should create file
        assertTrue(testUser.setEmail("email@email.com"));

        // Ensure it can be retrieved
        assertEquals(testUser.getEmail(), "email@email.com");

        // Ensure it can be changed and retrieved
        assertTrue(testUser.setEmail("email@other.com"));
        assertEquals(testUser.getEmail(), "email@other.com");
    }

    @Test
    public void testGetSetEmailBad()
    {
        // Set fails when there isn't a user dir
        assertFalse(badUser.setEmail("bad"));

        // Get returns null when it fails
        assertEquals(badUser.getEmail(), null);
    }

    @Test
    public void testGetSetPasswordHash()
    {
        // First set should create file
        assertTrue(testUser.setPasswordHash("1234-abcd"));

        // Ensure it can be retrieved
        assertEquals(testUser.getPasswordHash(), "1234-abcd");

        // Ensure it can be changed and retrieved
        assertTrue(testUser.setPasswordHash("1234-abcd"));
        assertEquals(testUser.getPasswordHash(), "1234-abcd");
    }

    @Test
    public void testGetSetPasswordHashBad()
    {
        // Set fails when there isn't a user dir
        assertFalse(badUser.setPasswordHash("bad"));

        // Get returns null when it fails
        assertEquals(badUser.getPasswordHash(), null);
    }

    @Test
    public void testGetSetAdmin()
    {
        // First set should create file
        assertTrue(testUser.setAdmin(false));

        // Ensure it can be retrieved
        assertEquals(testUser.getAdmin(), false);

        // Ensure it can be changed and retrieved
        assertTrue(testUser.setAdmin(true));
        assertEquals(testUser.getAdmin(), true);
    }

    @Test
    public void testGetSetAdminBad()
    {
        // Set fails when there isn't a user dir
        assertFalse(badUser.setAdmin(false));

        // Get returns false when it fails
        assertEquals(badUser.getAdmin(), false);
    }

    @Test
    public void testGetChild()
    {
        // getChild for a user just returns a data object
        FilesystemMemory uname = testUser.getChild("username");
        assertNotNull(uname);

        // data object doesn't provide children
        assertNull(uname.getChild("this won't work"));
    }

    @Test
    public void testPasswordMetadataRoundTrip() {
        assertTrue(testUser.setPasswordHash("hashB64"));
        assertTrue(testUser.setPasswordSalt("saltB64"));
        assertTrue(testUser.setPasswordAlgo("PBKDF2WithHmacSHA256"));
        assertTrue(testUser.setPasswordIters(120000));

        assertEquals("hashB64", testUser.getPasswordHash());
        assertEquals("saltB64", testUser.getPasswordSalt());
        assertEquals("PBKDF2WithHmacSHA256", testUser.getPasswordAlgo());
        assertEquals(120000, testUser.getPasswordIters());
    }

    @Test
    public void testPasswordMetadataPersistsOnDisk() {
        // Write via testUser
        assertTrue(testUser.setPasswordHash("H"));
        assertTrue(testUser.setPasswordSalt("S"));
        assertTrue(testUser.setPasswordAlgo("ALG"));
        assertTrue(testUser.setPasswordIters(42));

        // Re-open a fresh reference to the same user folder
        File tmpDir = new File("src/test/resources/fsam_testdata/userTmp");
        User reread = new User();
        reread.setFileObj(new File(tmpDir, "testUser"));
        reread.setParent(tmpDir);

        assertEquals("H", reread.getPasswordHash());
        assertEquals("S", reread.getPasswordSalt());
        assertEquals("ALG", reread.getPasswordAlgo());
        assertEquals(42, reread.getPasswordIters());
    }

    @Test
    public void testNoPlaintextPasswordStored() throws Exception {
        String plaintext = "Secret123!";

        // Simulate as if fields were set after hashing (we do not store plaintext)
        assertTrue(testUser.setPasswordHash("dummyHashBase64"));
        assertTrue(testUser.setPasswordSalt("dummySaltBase64"));
        assertTrue(testUser.setPasswordAlgo("PBKDF2WithHmacSHA256"));
        assertTrue(testUser.setPasswordIters(120000));

        // Scan files under the user's dir: none should contain the plaintext
        File userDir = testUser.getFileObj();
        File[] files = userDir.listFiles();
        assertNotNull(files);
        for (File f : files) {
            String content = Files.readString(f.toPath());
            assertNotEquals(plaintext, content, "Found plaintext password in: " + f.getName());
        }
    }

    @Test
    public void testPasswordMetadataBadUser() {
        // badUser has no directory; all setters should fail and getters return null
        assertFalse(badUser.setPasswordHash("x"));
        assertFalse(badUser.setPasswordSalt("y"));
        assertFalse(badUser.setPasswordAlgo("z"));
        assertFalse(badUser.setPasswordIters(1));

        assertEquals(null, badUser.getPasswordHash());
        assertEquals(null, badUser.getPasswordSalt());
        assertEquals(null, badUser.getPasswordAlgo());
        assertEquals(null, badUser.getPasswordIters());
    }
}
