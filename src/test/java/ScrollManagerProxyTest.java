import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScrollManagerProxyTest {

    private ScrollManager scrollManager;
    private ScrollManagerProxy proxy;
    private User testUser;
    private File tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = File.createTempFile("scrolls", "");
        tempDir.delete();
        tempDir.mkdir();

        scrollManager = new ScrollManager(tempDir.getAbsolutePath());
        proxy = new ScrollManagerProxy(scrollManager);

        testUser = new User("u1");
        testUser.setUsername("tester");
    }

    @AfterEach
    void tearDown() {
        for (File f : tempDir.listFiles()) {
            if (f.isDirectory()) {
                for (File child : f.listFiles()) {
                    child.delete();
                }
            }
            f.delete();
        }
        tempDir.delete();
    }

    @Test
    void testCreateScroll() {
        Scroll s = proxy.createScroll(testUser, "scroll1");
        assertNotNull(s);

        Scroll duplicate = proxy.createScroll(testUser, "scroll1");
        assertNull(duplicate);
    }

    @Test
    void testUpdateScroll() throws ScrollAlreadyExistsException {
        scrollManager.createScroll("scroll2");

        boolean updated = proxy.updateScrollName(testUser, "scroll2", "NewName");
        assertTrue(updated);

        boolean failUpdate = proxy.updateScrollName(testUser, "nonexistent", "Name");
        assertFalse(failUpdate);
    }

    @Test
    void testDeleteScroll() throws ScrollAlreadyExistsException {
        scrollManager.createScroll("scroll3");

        boolean deleted = proxy.deleteScroll(testUser, "scroll3");
        assertTrue(deleted);

        boolean failDelete = proxy.deleteScroll(testUser, "scroll3");
        assertFalse(failDelete);
    }

    @Test
    void testDownloadScroll() throws IOException, ScrollAlreadyExistsException {
        Scroll s = scrollManager.createScroll("scroll4");
        File blob = s.getScrollFile();
        assertNotNull(blob);

        File downloaded = proxy.downloadScroll(testUser, "scroll4");
        assertNotNull(downloaded);

        File failDownload = proxy.downloadScroll(testUser, "nonexistent");
        assertNull(failDownload);

        blob.delete();
        File failDownload2 = proxy.downloadScroll(testUser, "scroll4");
        assertNull(failDownload2);
    }

    @Test
    void testViewScroll() throws ScrollAlreadyExistsException {
        scrollManager.createScroll("scroll5");

        Scroll viewed = proxy.viewScroll(testUser, "scroll5");
        assertNotNull(viewed);

        Scroll notFound = proxy.viewScroll(testUser, "nonexistent");
        assertNull(notFound);
    }

    @Test
    void testFindScrollByName() throws ScrollAlreadyExistsException {
        Scroll s = scrollManager.createScroll("scroll6");
        s.setScrollName("MagicScroll");

        Scroll found = proxy.findScrollByName(testUser, "MagicScroll");
        assertNotNull(found);

        Scroll notFound = proxy.findScrollByName(testUser, "NoScroll");
        assertNull(notFound);
    }

    @Test
    void testGetAllScrolls() throws ScrollAlreadyExistsException {
        scrollManager.createScroll("scroll7");
        scrollManager.createScroll("scroll8");

        List<Scroll> scrolls = proxy.getAllScrolls(testUser);
        assertEquals(2, scrolls.size());
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

    static class FailingScroll extends Scroll {
        public FailingScroll(String id) {
            super(id);
        }

        @Override
        public boolean setScrollFile(String filePath) {
            throw new RuntimeException("Simulated update failure");
        }

        @Override
        public boolean setScrollName(String name) {
            throw new RuntimeException("Simulated set name failure");
        }

        @Override
        public File getScrollFile() {
            throw new RuntimeException("Simulated file access failure");
        }
    }

    static class FailingScrollManager extends ScrollManager {
        public FailingScrollManager(String dir) {
            super(dir);
        }

        @Override
        public Scroll getScroll(String scrollId) {
            return new FailingScroll(scrollId);
        }

        @Override
        public boolean changeScrollId(String oldId, String newId) throws ScrollAlreadyExistsException, ScrollDoesNotExistException {
            throw new ScrollAlreadyExistsException("Simulated change failure");
        }
    }

    @Test
    void testUpdateScrollFileThrowsException() {
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        ScrollManager failingManager = new FailingScrollManager(tempDir.getAbsolutePath());
        ScrollManagerProxy proxyWithFail = new ScrollManagerProxy(failingManager);

        boolean result = proxyWithFail.updateScrollFile(testUser, "scrollX", "file.txt");
        assertFalse(result);
        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("UPDATE_SCROLL_FILE_FAILED") && s.contains("Simulated update failure")));
    }

    @Test
    void testUpdateScrollNameThrowsException() {
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        ScrollManager failingManager = new FailingScrollManager(tempDir.getAbsolutePath());
        ScrollManagerProxy proxyWithFail = new ScrollManagerProxy(failingManager);

        boolean result = proxyWithFail.updateScrollName(testUser, "scrollX", "newName");
        assertFalse(result);
        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("UPDATE_SCROLL_FAILED") && s.contains("Simulated set name failure")));
    }

    @Test
    void testDownloadScrollThrowsException() {
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        ScrollManager failingManager = new FailingScrollManager(tempDir.getAbsolutePath());
        ScrollManagerProxy proxyWithFail = new ScrollManagerProxy(failingManager);

        File result = proxyWithFail.downloadScroll(testUser, "scrollX");
        assertNull(result);
        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("DOWNLOAD_SCROLL_FAILED") && s.contains("Simulated file access failure")));
    }

    @Test
    void testChangeScrollIdThrowsException() {
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        ScrollManager failingManager = new FailingScrollManager(tempDir.getAbsolutePath());
        ScrollManagerProxy proxyWithFail = new ScrollManagerProxy(failingManager);

        boolean result = proxyWithFail.changeScrollId(testUser, "old", "new");
        assertFalse(result);
        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("CHANGE_SCROLL_ID_FAILED") && s.contains("Simulated change failure")));
    }

    static class NonChangingScrollManager extends ScrollManager {
        public NonChangingScrollManager(String dir) { super(dir); }

        @Override
        public boolean changeScrollId(String oldId, String newId) {
            return false; 
        }
    }

    @Test
    void testChangeScrollIdFailsNormally() {
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        ScrollManager nonChangingManager = new NonChangingScrollManager(tempDir.getAbsolutePath());
        ScrollManagerProxy proxyWithFail = new ScrollManagerProxy(nonChangingManager);

        boolean result = proxyWithFail.changeScrollId(testUser, "old", "new");
        assertFalse(result);
        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("CHANGE_SCROLL_ID_FAILED") && s.contains("Failed to change scroll ID")));
    }

    static class NonUpdatingScroll extends Scroll {
        public NonUpdatingScroll(String id) { super(id); }

        @Override
        public boolean setScrollFile(String filePath) {
            return false; 
        }
    }

    static class NonUpdatingScrollManager extends ScrollManager {
        public NonUpdatingScrollManager(String dir) { super(dir); }

        @Override
        public Scroll getScroll(String scrollId) {
            return new NonUpdatingScroll(scrollId);
        }
    }

    @Test
    void testUpdateScrollFileFailsNormally() {
        FakeEventLogManager fakeLog = new FakeEventLogManager();
        EventLogManager.setInstanceForTesting(fakeLog);

        ScrollManager nonUpdatingManager = new NonUpdatingScrollManager(tempDir.getAbsolutePath());
        ScrollManagerProxy proxyWithFail = new ScrollManagerProxy(nonUpdatingManager);

        boolean result = proxyWithFail.updateScrollFile(testUser, "scrollX", "file.txt");
        assertFalse(result);
        assertTrue(fakeLog.getLoggedEvents().stream()
            .anyMatch(s -> s.contains("UPDATE_SCROLL_FILE_FAILED") && s.contains("Failed to update file")));
    }


}
