import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminManager {
    private final UserManager userManager;
    private final User currentUser;
    private final ScrollManager scrollManager;

    public AdminManager(String userRootPath, User currentUser, ScrollManager scrollManager) {
        this.userManager = new UserManager(userRootPath);
        this.currentUser = currentUser;
        this.scrollManager = scrollManager;
    }

    private void checkAdmin() throws SecurityException {
        if (currentUser == null || !currentUser.getAdmin()) {
            throw new SecurityException("Access denied: user is not an admin.");
        }
    }

    // Will get used for CLI, most likely
    public boolean parseBooleanInput(String input) {
        if (input == null) return false;
        input = input.trim().toLowerCase();
        return input.equals("true") || input.equals("yes") || input.equals("y") || input.equals("1");
    }

    public List<User> getAllUsers() {
        checkAdmin();
        return userManager.getAllUsers();
    }

    public User getUser(String userId) {
        checkAdmin();
        return userManager.getUser(userId);
    }

    public boolean createUser(String userId, String username, String password, boolean isAdmin) {
        checkAdmin();

        if (userId == null || userId.isBlank() || username.equalsIgnoreCase("null")) {
            System.err.println("User ID and username cannot be blank nor 'null'.");
            return false;
        }
        userId = userId.trim();

        if (!userId.matches("[A-Za-z0-9_-]+")) {
            System.err.println("Invalid user ID: " + userId);
            return false;
        }

        if (userManager.getUser(userId) != null) {
            System.err.println("User already exists: " + userId);
            return false;
        }

        try {
            User user = userManager.createUser(userId);
            if (user == null) {
                System.err.println("Failed to create user: " + userId);
                return false;
            }

            boolean buh = user.setUsername(username) && userManager.setPassword(user, password) && user.setAdmin(isAdmin);
                    // Prone to change
                    /*
                    && user.setName(name)
                    && user.setEmail(email)
                    && user.setPhone(phone)
                    */

            if (!buh) {
                File dir = user.getFileObj();
                if (dir.exists()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) f.delete();
                    }
                    dir.delete();
                }
                System.err.println("User creation failed. Rolling back changes.");
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String userId) {
        checkAdmin();

        if (userId.equals(currentUser.getUserId())) {
            System.err.println("Admins cannot delete their own account while logged in.");
            return false;
        }

        User user = userManager.getUser(userId);
        if (user == null) {
            System.err.println("User not found: " + userId);
            return false;
        }

        if (user.getUserId().equals("root")) {
            System.err.println("Cannot delete the root admin because this account is required by the system.");
            return false;
        }

        File userDir = user.getFileObj();
        if (userDir == null || !userDir.exists() || !userDir.isDirectory()) {
            System.err.println("User does not exist: " + userId);
            return false;
        }

        boolean success = true;
        File[] files = userDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.delete()) {
                    System.err.println("Failed to delete file: " + f.getName());
                    success = false;
                }
            }
        }

        if (!userDir.delete()) {
            System.err.println("Failed to delete user directory: " + userId);
            success = false;
        }

        for (Scroll s : scrollManager.getAllScrolls())
        {
            String uploaderId = s.getUploaderId();
            // Delete scrolls uploaded by the deleted user
            if (uploaderId != null && uploaderId.equals(userId))
            {
                scrollManager.deleteScroll(s.getScrollId());
            }
        }

        return success;

        // If we have subdirs, may need to create recursive delete
    }

    public boolean updateAdminStatus(String userId, boolean isAdmin) {
        checkAdmin();
        if (userId.equals("root")) {
            System.err.println("Cannot change role of root admin.");
            return false;
        }

        User user = userManager.getUser(userId);
        if (user == null) {
            System.err.println("User not found: " + userId);
            return false;
        }

        return user.setAdmin(isAdmin);
    }

    public Map<String, String> viewScrollStats() {
        checkAdmin();

        Map<String, String> stats = new HashMap<>();
        List<Scroll> scrolls = scrollManager.getAllScrolls();

        for (Scroll s : scrolls) {
            if (s == null || s.getScrollId() == null) continue;

            String scrollId = s.getScrollId();
            String scrollName = s.getScrollName() != null ? s.getScrollName() : "Unknown";
            String uploaderId = s.getUploaderId() != null ? s.getUploaderId() : "Unknown";
            Date uploadDate = s.getUploadDate();

            int uploadCount = (s.getScrollFile() != null && s.getScrollFile().exists()) ? 1 : 0;

            int downloadCount = 0;
            try {
                File downloadsFolder = scrollManager.getChild("downloads/" + scrollId).getFileObj();
                if (downloadsFolder.exists() && downloadsFolder.isDirectory()) {
                    File[] files = downloadsFolder.listFiles();
                    if (files != null) downloadCount = files.length;
                }
            } catch (Exception e) {
                downloadCount = 0;
            }

            stats.put(scrollId, String.format(
                    "Name: %s, Uploader: %s, Uploaded: %s, Uploads: %d, Downloads: %d",
                    scrollName,
                    uploaderId,
                    (uploadDate != null ? uploadDate.toString() : "Unknown"),
                    uploadCount,
                    downloadCount
            ));
        }

        return stats;
    }

    public User viewAsUser(String targetUserID) {
        checkAdmin();

        if (targetUserID == null || targetUserID.isBlank()) {
            System.err.println("User ID cannot be empty.");
            return null;
        }

        User target = userManager.getUser(targetUserID);
        if (target == null) {
            System.err.println("User not found: " + targetUserID);
            return null;
        }

        if (target.getAdmin()) {
            System.err.println("Cannot view as another admin user.");
            return null;
        }

        return target;
    }

    public ScrollManager.ScrollTextPreview viewAsGuest(String scrollId) {
        checkAdmin();

        ScrollManager.ScrollTextPreview preview = scrollManager.previewScrollText(scrollId);
        if (preview == null) {
            System.err.println("Scroll not found or empty: " + scrollId);
            return null;
        }

        return preview;
    }
}
