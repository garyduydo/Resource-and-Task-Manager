import java.io.File;
import java.util.Date;
import java.util.List;

public class ScrollManagerProxy {
    private final ScrollManager scrollManager;
    private final EventLogManager logManager = EventLogManager.getInstance();

    public ScrollManagerProxy(ScrollManager scrollManager) {
        this.scrollManager = scrollManager;
    }

    public Scroll createScroll(User user, String scrollId) {
        try {
            Scroll scroll = scrollManager.createScroll(scrollId);
            logManager.log(user.getUserId(), user.getUsername(), "ADD_SCROLL", "Created scroll ID: " + scrollId);
            return scroll;
        } catch (ScrollAlreadyExistsException e) {
            logManager.log(user.getUserId(), user.getUsername(), "ADD_SCROLL_FAILED", e.getMessage());
            return null;
        }
    }

    public boolean updateScrollName(User user, String scrollId, String newName) {
        try {
            Scroll scroll = scrollManager.getScroll(scrollId);
            if (scroll == null) {
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_SCROLL_FAILED",
                        "Scroll not found: " + scrollId);
                return false;
            }

            boolean success = scroll.setScrollName(newName);
            if (success) {
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_SCROLL",
                        "Updated scroll ID: " + scrollId + " to new name: " + newName);
            } else {
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_SCROLL_FAILED",
                        "Failed to set new name for scroll ID: " + scrollId);
            }
            return success;

        } catch (Exception e) {
            logManager.log(user.getUserId(), user.getUsername(), "UPDATE_SCROLL_FAILED", e.getMessage());
            return false;
        }
    }


    public boolean deleteScroll(User user, String scrollId) {
        try {
            boolean deleted = scrollManager.deleteScroll(scrollId);
            if (deleted) {
                logManager.log(user.getUserId(), user.getUsername(), "DELETE_SCROLL", "Deleted scroll ID: " + scrollId);
            } else {
                logManager.log(user.getUserId(), user.getUsername(), "DELETE_SCROLL_FAILED",
                    "Scroll not found: " + scrollId);
            }
            return deleted;
        } catch (Exception e) {
            logManager.log(user.getUserId(), user.getUsername(), "DELETE_SCROLL_FAILED", e.getMessage());
            return false;
        }
    }

    public File downloadScroll(User user, String scrollId) {
        try {
            Scroll scroll = scrollManager.getScroll(scrollId);
            if (scroll == null) {
                logManager.log(user.getUserId(), user.getUsername(), "DOWNLOAD_SCROLL_FAILED",
                    "Scroll not found: " + scrollId);
                return null;
            }

            File file = scroll.getScrollFile();
            if (file != null && file.exists()) {
                logManager.log(user.getUserId(), user.getUsername(), "DOWNLOAD_SCROLL",
                    "Downloaded scroll ID: " + scrollId);
                return file;
            } else {
                logManager.log(user.getUserId(), user.getUsername(), "DOWNLOAD_SCROLL_FAILED",
                    "No scroll file found for ID: " + scrollId);
                return null;
            }
        } catch (Exception e) {
            logManager.log(user.getUserId(), user.getUsername(), "DOWNLOAD_SCROLL_FAILED", e.getMessage());
            return null;
        }
    }

    public boolean changeScrollId(User user, String oldId, String newId) {
        try {
            boolean success = scrollManager.changeScrollId(oldId, newId);
            if (success) {
                logManager.log(user.getUserId(), user.getUsername(), "CHANGE_SCROLL_ID",
                        "Changed scroll ID from " + oldId + " to " + newId);
            } else {
                logManager.log(user.getUserId(), user.getUsername(), "CHANGE_SCROLL_ID_FAILED",
                        "Failed to change scroll ID from " + oldId + " to " + newId);
            }
            return success;
        } catch (ScrollAlreadyExistsException | ScrollDoesNotExistException e) {
            logManager.log(user.getUserId(), user.getUsername(), "CHANGE_SCROLL_ID_FAILED", e.getMessage());
            return false;
        }
    }

    public boolean updateScrollFile(User user, String scrollId, String filePath) {
        try {
            Scroll scroll = scrollManager.getScroll(scrollId);
            if (scroll == null) {
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_SCROLL_FILE_FAILED",
                        "Scroll not found: " + scrollId);
                return false;
            }

            boolean success = scroll.setScrollFile(filePath);
            if (success) {
                scroll.setUploadDate(new Date());
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_SCROLL_FILE",
                        "Updated file for scroll ID: " + scrollId);
            } else {
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_SCROLL_FILE_FAILED",
                        "Failed to update file for scroll ID: " + scrollId);
            }
            return success;

        } catch (Exception e) {
            logManager.log(user.getUserId(), user.getUsername(), "UPDATE_SCROLL_FILE_FAILED", e.getMessage());
            return false;
        }
    }

    // Gray area
    public Scroll viewScroll(User user, String scrollId) {
        Scroll scroll = scrollManager.getScroll(scrollId);
        if (scroll != null) {
            logManager.log(user.getUserId(), user.getUsername(), "VIEW_SCROLL", "Viewed scroll ID: " + scrollId);
        }
        return scroll;
    }

    public Scroll findScrollByName(User user, String scrollName) {
        Scroll scroll = scrollManager.findScrollByScrollName(scrollName);
        if (scroll != null) {
            logManager.log(user.getUserId(), user.getUsername(), "SEARCH_SCROLL",
                    "Searched scroll name: " + scrollName);
        }
        return scroll;
    }

    public List<Scroll> getAllScrolls(User user) {
        List<Scroll> scrolls = scrollManager.getAllScrolls();
        logManager.log(user.getUserId(), user.getUsername(), "VIEW_ALL_SCROLLS", "Listed all scrolls");
        return scrolls;
    } 

}
