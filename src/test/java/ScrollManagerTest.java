import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import fsam.FilesystemMemory;

import java.lang.System;

public class ScrollManagerTest {
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

    private static ScrollManager testManager;

    @BeforeAll
    public static void createEnv()
    {
        File tmpDir = new File("src/test/resources/fsam_testdata/scrollManagerTmp/");
        tmpDir.mkdirs();

        testManager = new ScrollManager("src/test/resources/fsam_testdata/scrollManagerTmp");
    }

    @AfterAll
    public static void cleanEnv()
    {
        // Remove tmp resource dir used to test user manager
        cleanFiles(new File("src/test/resources/fsam_testdata/scrollManagerTmp/"));
    }

    @Test
    public void ensureManagerExists()
    {
        assertTrue(testManager.exists());
    }

    @Test
    public void testGetScrollDoesntExist()
    {
        assertNull(testManager.getScroll("doesn't exist"));
    }

    @Test
    public void testCreateAndGetScroll()
    {
        assertDoesNotThrow(() -> testManager.createScroll("testScroll"));

        assertNotNull(testManager.getScroll("testScroll"));
    }

    @Test
    public void testCreateScrollExists()
    {
        assertDoesNotThrow(() -> testManager.createScroll("testExistsScroll"));

        assertThrows(ScrollAlreadyExistsException.class, () -> testManager.createScroll("testExistsScroll"));
    }

    @Test
    public void testChangeScrollId()
    {
        assertDoesNotThrow(() -> testManager.createScroll("testChangeScroll"));
        assertNotNull(testManager.getScroll("testChangeScroll"));
        assertDoesNotThrow(() -> testManager.changeScrollId("testChangeScroll", "otherScroll"));
        assertNull(testManager.getScroll("testChangeScroll"));
        assertNotNull(testManager.getScroll("otherScroll"));
    }

    @Test
    public void testChangeScrollIdDoesntExist()
    {
        assertThrows(ScrollDoesNotExistException.class, () -> testManager.changeScrollId("does not exist", "otherScroll"));
    }

    @Test
    public void testChangeScrollIdOverlap()
    {
        assertDoesNotThrow(() -> testManager.createScroll("scroll1"));
        assertDoesNotThrow(() -> testManager.createScroll("scroll2"));

        assertThrows(ScrollAlreadyExistsException.class, () -> testManager.changeScrollId("scroll1", "scroll2"));
    }

    @Test
    public void testGetAllScrolls()
    {
        assertDoesNotThrow(() -> testManager.createScroll("getAllTestScroll"));

        boolean foundTestScroll = false;

        for (Scroll u : testManager.getAllScrolls())
        {
            if (u.getScrollId().equals("getAllTestScroll"))
            {
                foundTestScroll = true;
            }
        }

        assertTrue(foundTestScroll);
    }

    @Test
    public void testFindScrollByScrollnameMatch()
    {
        assertDoesNotThrow(() -> testManager.createScroll("findScroll"));

        Scroll findScroll = testManager.getScroll("findScroll");

        findScroll.setScrollName("find this scroll");

        assertNotNull(testManager.findScrollByScrollName("find this scroll"));
    }

    @Test
    public void testFindScrollByScrollnameNoMatch()
    {
        assertNull(testManager.findScrollByScrollName("no scroll with this name"));
    }

    @Test
    public void testDeleteScroll()
    {
        assertDoesNotThrow(() -> testManager.createScroll("deleteTestScroll"));

        Scroll s = testManager.getScroll("deleteTestScroll");

        assertTrue(s.exists());
        assertTrue(testManager.deleteScroll("deleteTestScroll"));
        assertFalse(s.exists());
    }

    @Test
    public void testGetAllScrollsSorted_NewestFirst()
    {
        assertDoesNotThrow(() -> testManager.createScroll("viewA"));
        assertDoesNotThrow(() -> testManager.createScroll("viewB"));
        assertDoesNotThrow(() -> testManager.createScroll("viewC"));

        Scroll a = testManager.getScroll("viewA");
        Scroll b = testManager.getScroll("viewB");
        Scroll c = testManager.getScroll("viewC");

        a.setScrollName("Alpha");
        b.setScrollName("Beta");
        c.setScrollName("Gamma");

        a.setUploadDate(new java.util.Date(1000L));
        b.setUploadDate(new java.util.Date(2000L));
        c.setUploadDate(new java.util.Date(3000L));

        java.util.List<Scroll> sorted = testManager.getAllScrollsSorted();
        int ia = indexOfScroll(sorted, "viewA");
        int ib = indexOfScroll(sorted, "viewB");
        int ic = indexOfScroll(sorted, "viewC");

        assertTrue(ic < ib && ib < ia);
    }

    private int indexOfScroll(java.util.List<Scroll> list, String id)
    {
        for (int i = 0; i < list.size(); i++)
        {
            Scroll s = list.get(i);
            if (s != null && id.equals(s.getScrollId()))
            {
                return i;
            }
        }
        return -1;
    }

    @Test
    public void testDownloadScroll_Succeeds()
    {
        String id = "downloadOk";
        assertDoesNotThrow(() -> testManager.createScroll(id));
        Scroll s = testManager.getScroll(id);
        s.setScrollName("Download Me");
        s.setUploaderId("u1");
        s.setUploadDate(new java.util.Date());

        File src = new File("src/test/resources/fsam_testdata/scrollManagerTmp/src_download.txt");
        writeText(src, "hello download");

        assertTrue(s.setScrollFile(src.getPath()));

        File dest = new File("src/test/resources/fsam_testdata/scrollManagerTmp/dest_download.txt");
        if (dest.exists()) dest.delete();

        assertTrue(testManager.downloadScroll(id, dest.getPath()));
        assertTrue(dest.exists());
        assertEquals("hello download", readAll(dest));
    }

    @Test
    public void testDownloadScroll_OverwritesExisting()
    {
        String id = "downloadOverwrite";
        assertDoesNotThrow(() -> testManager.createScroll(id));
        Scroll s = testManager.getScroll(id);
        s.setScrollName("Overwrite");
        s.setUploaderId("u2");
        s.setUploadDate(new java.util.Date());

        File src = new File("src/test/resources/fsam_testdata/scrollManagerTmp/src_overwrite.txt");
        writeText(src, "first content");
        assertTrue(s.setScrollFile(src.getPath()));

        File dest = new File("src/test/resources/fsam_testdata/scrollManagerTmp/dest_overwrite.txt");
        writeText(dest, "old");

        assertTrue(testManager.downloadScroll(id, dest.getPath()));
        assertEquals("first content", readAll(dest));
    }

    @Test
    public void testDownloadScroll_BadIdOrMissingBlob()
    {
        assertFalse(testManager.downloadScroll(
            "no-such-id",
            "src/test/resources/fsam_testdata/scrollManagerTmp/nowhere.txt"));

        String id = "downloadMissingBlob";
        assertDoesNotThrow(() -> testManager.createScroll(id));

        File dest = new File("src/test/resources/fsam_testdata/scrollManagerTmp/dest_missing.txt");
        if (dest.exists()) dest.delete();

        assertTrue(testManager.downloadScroll(id, dest.getPath()));
        assertTrue(dest.exists());
        assertEquals(0L, dest.length());
    }

    @Test
    public void testSearchScrolls_ByEachFilterAndCombined()
    {
        seedScroll("S1", "Project Plan", "alice",  day("2025-10-01"), "Alpha text");
        seedScroll("S2", "Budget Q4",    "bob",    day("2025-10-05"), "Beta text");
        seedScroll("S3", "Meeting Notes","alice",  day("2025-10-10"), "Gamma text");
        seedScroll("S4", "Plan Review",  "carol",  day("2025-10-15"), "Delta text");

        java.util.List<Scroll> byUploader = testManager.searchScrolls("alice", null, null, null, null);
        assertTrue(containsId(byUploader, "S1"));
        assertTrue(containsId(byUploader, "S3"));
        assertFalse(containsId(byUploader, "S2"));
        assertFalse(containsId(byUploader, "S4"));

        java.util.List<Scroll> byId = testManager.searchScrolls(null, "S2", null, null, null);
        assertEquals(1, byId.size());
        assertEquals("S2", byId.get(0).getScrollId());

        java.util.List<Scroll> byName = testManager.searchScrolls(null, null, "plan", null, null);
        assertTrue(containsId(byName, "S1"));
        assertTrue(containsId(byName, "S4"));
        assertFalse(containsId(byName, "S2"));
        assertFalse(containsId(byName, "S3"));

        java.util.Date after = day("2025-10-05");
        java.util.Date before = day("2025-10-10");
        java.util.List<Scroll> byDate = testManager.searchScrolls(null, null, null, after, before);
        assertTrue(containsId(byDate, "S2"));
        assertTrue(containsId(byDate, "S3"));
        assertFalse(containsId(byDate, "S1"));
        assertFalse(containsId(byDate, "S4"));

        java.util.List<Scroll> combo = testManager.searchScrolls("alice", null, "plan", day("2025-10-01"), null);
        assertTrue(containsId(combo, "S1"));
        assertFalse(containsId(combo, "S3"));
    }

    @Test
    public void testSearchScrolls_NullOrBlankFilters_ReturnsAllSorted()
    {
        java.util.List<Scroll> sorted = testManager.getAllScrollsSorted();
        java.util.List<Scroll> searched = testManager.searchScrolls(null, null, null, null, null);

        assertEquals(sorted.size(), searched.size());
        for (int i = 0; i < sorted.size(); i++)
        {
            assertEquals(sorted.get(i).getScrollId(), searched.get(i).getScrollId());
        }
    }

    @Test
    public void testSearchScrolls_MissingDatesExcludedWhenDateFilterApplied()
    {
        String id = "noDateScroll";
        assertDoesNotThrow(() -> testManager.createScroll(id));
        Scroll s = testManager.getScroll(id);
        s.setScrollName("No Date");
        s.setUploaderId("nd");

        // Make upload date "missing" by setting epoch 0 using the public API
        s.setUploadDate(new java.util.Date(0L));

        java.util.List<Scroll> res = testManager.searchScrolls(null, null, null, day("2025-10-01"), null);
        assertFalse(containsId(res, id)); // excluded because cannot verify date
    }

    // Helpers for search tests
    private boolean containsId(java.util.List<Scroll> list, String id)
    {
        if (list == null) return false;
        for (Scroll s : list)
        {
            if (s != null && id.equals(s.getScrollId()))
            {
                return true;
            }
        }
        return false;
    }

    private void seedScroll(String id, String name, String uploader, java.util.Date date, String content)
    {
        assertDoesNotThrow(() -> testManager.createScroll(id));
        Scroll s = testManager.getScroll(id);
        s.setScrollName(name);
        s.setUploaderId(uploader);
        s.setUploadDate(date);

        File src = new File("src/test/resources/fsam_testdata/scrollManagerTmp/src_" + id + ".txt");
        writeText(src, content);
        assertTrue(s.setScrollFile(src.getPath()));
    }

    private java.util.Date day(String ymd)
    {
        try
        {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
            df.setLenient(false);
            return df.parse(ymd);
        }
        catch (Exception e)
        {
            return new java.util.Date(0L);
        }
    }

    @Test
    public void testPreviewScrollText_First500Chars_AndTruncated()
    {
        String id = "previewLong";
        assertDoesNotThrow(() -> testManager.createScroll(id));
        Scroll s = testManager.getScroll(id);
        s.setScrollName("Long Doc");
        s.setUploaderId("writer");
        s.setUploadDate(new java.util.Date());

        // Create > 600 chars
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 650; i++) { big.append('A'); }
        File src = new File("src/test/resources/fsam_testdata/scrollManagerTmp/src_preview_long.txt");
        writeText(src, big.toString());
        assertTrue(s.setScrollFile(src.getPath()));

        ScrollManager.ScrollTextPreview p = testManager.previewScrollText(id);
        assertNotNull(p);
        assertEquals("Long Doc", p.scrollName);
        assertEquals("writer", p.uploaderId);
        assertEquals(id, p.scrollId);
        assertNotNull(p.uploadDate);

        assertNotNull(p.textPreview);
        assertEquals(500, p.textPreview.length());
        assertTrue(p.truncated);
    }

    @Test
    public void testPreviewScrollText_ShortFile_NotTruncated()
    {
        String id = "previewShort";
        assertDoesNotThrow(() -> testManager.createScroll(id));
        Scroll s = testManager.getScroll(id);
        s.setScrollName("Short Doc");
        s.setUploaderId("author");
        s.setUploadDate(new java.util.Date());

        String content = "short content";
        File src = new File("src/test/resources/fsam_testdata/scrollManagerTmp/src_preview_short.txt");
        writeText(src, content);
        assertTrue(s.setScrollFile(src.getPath()));

        ScrollManager.ScrollTextPreview p = testManager.previewScrollText(id);
        assertNotNull(p);
        assertEquals(content, p.textPreview);
        assertFalse(p.truncated);
    }

    @Test
    public void testPreviewScrollText_NullCases()
    {
        assertNull(testManager.previewScrollText("nope"));

        String id = "previewNoBlob";
        assertDoesNotThrow(() -> testManager.createScroll(id));

        ScrollManager.ScrollTextPreview p = testManager.previewScrollText(id);
        assertNotNull(p);
        assertNotNull(p.textPreview);
        assertEquals(0, p.textPreview.length());
        assertFalse(p.truncated);
    }


    private void writeText(File f, String text)
    {
        try
        {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            java.nio.file.Files.writeString(f.toPath(), text, java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            fail("Failed to write test file: " + e.getMessage());
        }
    }

    private String readAll(File f)
    {
        try
        {
            return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            fail("Failed to read test file: " + e.getMessage());
            return null;
        }
    }
}


