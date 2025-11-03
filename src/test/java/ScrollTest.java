import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.util.Date;

import fsam.FilesystemMemory;

import java.lang.System;

public class ScrollTest {
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

    private static Scroll testScroll;
    private static Scroll badScroll;

    @BeforeAll
    public static void createEnv()
    {
        File tmpDir = new File("src/test/resources/fsam_testdata/scrollTmp");
        tmpDir.mkdirs();

        testScroll = new Scroll();
        testScroll.setFileObj(new File(tmpDir, "testScroll"));
        testScroll.setParent(tmpDir);
        try
        {
            testScroll.createSelfDir();
        }
        catch (IOException e)
        {
            System.exit(1);
        }

        badScroll = new Scroll();
        badScroll.setFileObj(new File(tmpDir, "badScroll"));
        badScroll.setParent(tmpDir);
    }

    @AfterAll
    public static void cleanEnv()
    {
        // Remove tmp resource dir used to test scrolls
        cleanFiles(new File("src/test/resources/fsam_testdata/scrollTmp"));
    }

    @Test
    public void testGetScrollId()
    {
        assertEquals(testScroll.getScrollId(), "testScroll");
        assertEquals(badScroll.getScrollId(), "badScroll");
    }

    @Test
    public void testGetSetScrollName()
    {
        // First set should create file
        assertTrue(testScroll.setScrollName("testScrollname"));

        // Ensure it can be retrieved
        assertEquals(testScroll.getScrollName(), "testScrollname");

        // Ensure it can be changed and retrieved
        assertTrue(testScroll.setScrollName("otherScrollname"));
        assertEquals(testScroll.getScrollName(), "otherScrollname");
    }

    @Test
    public void testGetSetScrollNameBad()
    {
        // Set fails when there isn't a scroll dir
        assertFalse(badScroll.setScrollName("bad"));

        // Get returns null when it fails
        assertEquals(badScroll.getScrollName(), null);
    }

    @Test
    public void testGetChild()
    {
        // getChild for a scroll just returns a data object
        FilesystemMemory name = testScroll.getChild("scrollname");
        assertNotNull(name);

        // data object doesn't provide children
        assertNull(name.getChild("this won't work"));
    }

    @Test
    public void testGetSetUploaderId()
    {
        // First set should create file
        assertTrue(testScroll.setUploaderId("testUploaderId"));

        // Ensure it can be retrieved
        assertEquals(testScroll.getUploaderId(), "testUploaderId");

        // Ensure it can be changed and retrieved
        assertTrue(testScroll.setUploaderId("otherUploaderId"));
        assertEquals(testScroll.getUploaderId(), "otherUploaderId");
    }

    @Test
    public void testGetSetUploaderIdBad()
    {
        // Set fails when there isn't a scroll dir
        assertFalse(badScroll.setUploaderId("bad"));

        // Get returns null when it fails
        assertEquals(badScroll.getUploaderId(), null);
    }

    @Test
    public void testGetSetUploadDate()
    {
        Date now = new Date();
        // First set should create file
        assertTrue(testScroll.setUploadDate(now));

        // Ensure it can be retrieved
        assertEquals(testScroll.getUploadDate(), now);

        // Ensure it can be changed and retrieved
        Date other = new Date(10);
        assertTrue(testScroll.setUploadDate(other));
        assertEquals(testScroll.getUploadDate(), other);
    }

    @Test
    public void testGetSetUploadDateBad()
    {
        // Set fails when there isn't a scroll dir
        assertFalse(badScroll.setUploadDate(new Date()));

        // Get returns null when it fails
        assertEquals(badScroll.getUploadDate(), null);
    }

    @Test
    public void testGetSetScrollFile()
    {
        // First set should create file
        assertTrue(testScroll.setScrollFile("src/test/resources/fsam_testdata/sample_scroll"));

        // Ensure it can be retrieved
        File f = testScroll.getScrollFile();
        assertNotNull(f);
        assertTrue(f.isFile());

        // Ensure it has the correct contents
        try
        {
            assertEquals(Files.mismatch(f.toPath(), new File("src/test/resources/fsam_testdata/sample_scroll").toPath()), -1);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }

        // Ensure it can be changed and retrieved
        assertTrue(testScroll.setScrollFile("src/test/resources/fsam_testdata/other_scroll"));
        f = testScroll.getScrollFile();
        assertNotNull(f);
        assertTrue(f.isFile());

        // Ensure it has the correct contents
        try
        {
            assertEquals(Files.mismatch(f.toPath(), new File("src/test/resources/fsam_testdata/other_scroll").toPath()), -1);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testGetSetScrollFileBad()
    {
        // Set fails when there isn't a scroll dir
        assertFalse(badScroll.setScrollFile("bad"));

        // Get returns null when it fails
        assertEquals(badScroll.getScrollName(), null);
    }

    @Test
    public void testGetSetScrollFileDoesNotExist()
    {
        assertFalse(testScroll.setScrollFile("this does not exist"));
    }
}
