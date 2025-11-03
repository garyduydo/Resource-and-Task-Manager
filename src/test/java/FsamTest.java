import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NotDirectoryException;

import fsam.FilesystemMemory;

public class FsamTest {
    protected class TestFsam extends FilesystemMemory {
        public TestFsam()
        {
            super();
        }

        public TestFsam(String objPath)
        {
            super(objPath);
        }

        public TestFsam getChild(String childPath)
        {
            TestFsam child = new TestFsam();
            child.setFileObj(new File(getFileObj(), childPath));
            child.setParent(getFileObj());

            return child;
        }
    }

    private TestFsam fsam;

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

    @BeforeAll
    public static void createEnv()
    {
        File tmpDir = new File("src/test/resources/fsam_testdata/tmp/rwDirectory");
        tmpDir.mkdirs();
    }

    @AfterAll
    public static void cleanEnv()
    {
        // Remove tmp resource dir used to test obj creation
        cleanFiles(new File("src/test/resources/fsam_testdata/tmp"));
    }

    @BeforeEach
    public void setUp()
    {
        fsam = new TestFsam("src/test/resources/fsam_testdata");
    }

    @Test
    public void testRootExists()
    {
        assertTrue(fsam.exists());
    }

    @Test
    public void testRootIsDir()
    {
        assertTrue(fsam.isDir());
    }

    @Test
    public void testRootIsNotFile()
    {
        assertFalse(fsam.isFile());
    }

    // NOTE : test_string is a pre-existing test resource
    @Test
    public void testChildExists()
    {
        assertTrue(fsam.childExists("test_string"));
    }

    @Test
    public void testSpawnChild()
    {
        TestFsam child = fsam.getChild("test_string");
        assertTrue(child.exists());
        assertTrue(child.isFile());
        assertFalse(child.isDir());
    }

    @Test
    public void testCreateSelfDir()
    {
        TestFsam selfDir = fsam.getChild("tmp/selfsubdir");
        assertFalse(selfDir.exists());
        assertDoesNotThrow(() -> selfDir.createSelfDir());
        assertTrue(selfDir.exists());
    }

    @Test
    public void testCreateSelfDirExistsAsFile()
    {
        TestFsam overlapFile = fsam.getChild("test_string");
        assertThrows(IOException.class, () -> overlapFile.createSelfDir());
    }

    @Test
    public void testCreateSelfDirExistsNoOp()
    {
        TestFsam tmpDir = fsam.getChild("tmp");
        assertDoesNotThrow(() -> tmpDir.createSelfDir());
    }

    @Test
    public void testCreateSelfFile()
    {
        TestFsam selfFile = fsam.getChild("tmp/selffile");
        assertFalse(selfFile.exists());
        assertDoesNotThrow(() -> selfFile.createSelfFile());
        assertTrue(selfFile.exists());
    }

    @Test
    public void testCreateSelfFileOverlapping()
    {
        TestFsam overlapFile = fsam.getChild("test_string");
        assertThrows(FileAlreadyExistsException.class, () -> overlapFile.createSelfFile());
    }

    @Test
    public void testCreateChildDir()
    {
        TestFsam childDir = fsam.getChild("tmp/subdir1/subdir2");
        assertFalse(childDir.exists());
        assertDoesNotThrow(() -> fsam.createChildDir("tmp/subdir1/subdir2"));
        assertTrue(childDir.exists());
    }

    @Test
    public void testCreateChildDirExistsAsFile()
    {
        assertThrows(NotDirectoryException.class, () -> fsam.createChildDir("test_string"));
    }

    @Test
    public void testCreateChildDirExistsNoOp()
    {
        assertDoesNotThrow(() -> fsam.createChildDir("tmp"));
    }

    @Test
    public void testCreateChildFile()
    {
        TestFsam childFile = fsam.getChild("tmp/dummy_file");
        assertFalse(childFile.exists());
        assertDoesNotThrow(() -> fsam.createChildFile("tmp/dummy_file"));
        assertTrue(childFile.exists());
    }

    @Test
    public void testCreateChildFileExists()
    {
        assertThrows(FileAlreadyExistsException.class, () -> fsam.createChildFile("test_string"));
    }

    @Test
    public void testMoveSelfExplicitRelative()
    {
        TestFsam moveTarget = fsam.getChild("tmp/moveExplicitTarget");
        assertDoesNotThrow(() -> fsam.createChildFile("tmp/moveExplicitTarget"));
        assertTrue(moveTarget.exists());
        assertDoesNotThrow(() -> moveTarget.moveSelf(fsam, "tmp/moveExplicitResult"));
        assertTrue(moveTarget.exists());
    }

    @Test
    public void testMoveSelfImplicitRelative()
    {
        TestFsam moveTarget = fsam.getChild("tmp/moveImplicitTarget");
        assertDoesNotThrow(() -> fsam.createChildFile("tmp/moveImplicitTarget"));
        assertTrue(moveTarget.exists());
        assertDoesNotThrow(() -> moveTarget.moveSelf("tmp/moveImplicitResult"));
        assertTrue(moveTarget.exists());
    }

    @Test
    public void testMoveSelfOverlapping()
    {
        TestFsam moveTarget = fsam.getChild("tmp/moveOverlappingTarget");
        TestFsam moveOverlap = fsam.getChild("tmp/moveOverlappingResult");
        assertDoesNotThrow(() -> moveTarget.createSelfFile());
        assertDoesNotThrow(() -> moveOverlap.createSelfFile());
        assertThrows(FileAlreadyExistsException.class, () -> moveTarget.moveSelf("tmp/moveOverlappingResult"));
    }

    @Test
    public void testMoveSelfNoParent()
    {
        assertThrows(IOException.class, () -> fsam.moveSelf("uh oh"));
    }

    @Test
    public void testMoveChild()
    {
        TestFsam originalChild = fsam.getChild("tmp/moveChildTarget");
        assertDoesNotThrow(() -> originalChild.createSelfFile());
        assertDoesNotThrow(() -> fsam.moveChild("tmp/moveChildTarget", "tmp/moveChildResult"));
        TestFsam movedChild = fsam.getChild("tmp/moveChildResult");
        assertTrue(movedChild.exists());
        assertTrue(movedChild.isFile());
    }

    @Test
    public void testMoveChildOverlapping()
    {
        TestFsam originalChild = fsam.getChild("tmp/moveChildOverlappingTarget");
        assertDoesNotThrow(() -> originalChild.createSelfFile());
        TestFsam overlapChild = fsam.getChild("tmp/moveChildOverlappingResult");
        assertDoesNotThrow(() -> overlapChild.createSelfFile());
        assertThrows(FileAlreadyExistsException.class, () -> fsam.moveChild("tmp/moveChildOverlappingTarget", "tmp/moveChildOverlappingResult"));
    }

    @Test
    public void testMoveChildDoesNotExist()
    {
        assertThrows(IOException.class, () -> fsam.moveChild("oops", "bad"));
    }

    @Test
    public void testGetExistingString()
    {
        TestFsam stringData = fsam.getChild("test_string");
        assertTrue(stringData.exists());
        try
        {
            // Ensure a regular read works
            assertEquals(stringData.getSelfString(), "hello world");
        }
        catch (IOException e)
        {
            // Fail instantly
            assertTrue(false);
        }
        try
        {
            // Ensure child read is equiv. to self read
            assertEquals(stringData.getSelfString(), fsam.getChildString("test_string"));
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }


    // TODO : Read write with self/child/new obj for all four types
    @Test
    public void testReadWriteString()
    {
        TestFsam strRW = fsam.getChild("tmp/stringRW");
        assertDoesNotThrow(() -> strRW.createSelfFile());
        assertDoesNotThrow(() -> strRW.setSelfString("hello world"));
        try
        {
            assertEquals(strRW.getSelfString(), "hello world");
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testReadWriteChildString()
    {
        TestFsam tmpDir = fsam.getChild("tmp/");
        assertDoesNotThrow(() -> tmpDir.createChildFile("stringChildRW"));
        assertDoesNotThrow(() -> tmpDir.setChildString("stringChildRW", "hello world"));
        try
        {
            assertEquals(tmpDir.getChildString("stringChildRW"), "hello world");
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testReadWriteInt()
    {
        TestFsam intRW = fsam.getChild("tmp/intRW");
        assertDoesNotThrow(() -> intRW.setSelfInt(10));
        try
        {
            assertEquals(intRW.getSelfInt(), 10);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testReadWriteChildInt()
    {
        TestFsam tmpDir = fsam.getChild("tmp/");
        assertDoesNotThrow(() -> tmpDir.createChildFile("intChildRW"));
        assertDoesNotThrow(() -> tmpDir.setChildInt("intChildRW", -1));
        try
        {
            assertEquals(tmpDir.getChildInt("intChildRW"), -1);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testReadWriteFloat()
    {
        TestFsam floatRW = fsam.getChild("tmp/floatRW");
        assertDoesNotThrow(() -> floatRW.createSelfFile());
        assertDoesNotThrow(() -> floatRW.setSelfFloat(0.0f));
        try
        {
            assertEquals(floatRW.getSelfFloat(), 0.0f);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testReadWriteChildFloat()
    {
        TestFsam tmpDir = fsam.getChild("tmp/");
        assertDoesNotThrow(() -> tmpDir.createChildFile("floatChildRW"));
        assertDoesNotThrow(() -> tmpDir.setChildFloat("floatChildRW", -100.0f));
        try
        {
            assertEquals(tmpDir.getChildFloat("floatChildRW"), -100.0f);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testReadWriteBool()
    {
        TestFsam boolRW = fsam.getChild("tmp/boolRW");
        assertDoesNotThrow(() -> boolRW.createSelfFile());
        assertDoesNotThrow(() -> boolRW.setSelfBoolean(true));
        try
        {
            assertEquals(boolRW.getSelfBoolean(), true);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testReadWriteChildBool()
    {
        TestFsam tmpDir = fsam.getChild("tmp/");
        assertDoesNotThrow(() -> tmpDir.createChildFile("boolChildRW"));
        assertDoesNotThrow(() -> tmpDir.setChildBoolean("boolChildRW", false));
        try
        {
            assertEquals(tmpDir.getChildBoolean("boolChildRW"), false);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
    }

    @Test
    public void testReadStringAsNumerical()
    {
        TestFsam str = fsam.getChild("test_string");
        assertThrows(NumberFormatException.class, () -> str.getSelfInt());
        assertThrows(NumberFormatException.class, () -> str.getSelfFloat());
        assertThrows(NumberFormatException.class, () -> fsam.getChildInt("test_string"));
        assertThrows(NumberFormatException.class, () -> fsam.getChildFloat("test_string"));
    }

    @Test
    public void testReadDoesNotExist()
    {
        TestFsam badFile = fsam.getChild("INVALID");
        assertThrows(IOException.class, () -> badFile.getSelfString());
    }

    @Test
    public void testReadDirectory()
    {
        TestFsam tmpDir = fsam.getChild("tmp/rwDirectory");
        assertThrows(IOException.class, () -> tmpDir.getSelfString());
    }

    @Test
    public void testWriteDirectory()
    {
        TestFsam tmpDir = fsam.getChild("tmp/rwDirectory");
        assertThrows(FileAlreadyExistsException.class, () -> tmpDir.setSelfString("uh oh"));

    }
}
