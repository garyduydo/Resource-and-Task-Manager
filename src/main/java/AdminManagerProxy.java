import java.util.List;
import java.util.Map;

public class AdminManagerProxy {
    private final AdminManager adminManager;
    private final EventLogManager logManager = EventLogManager.getInstance();
    private final User currentUser;

    public AdminManagerProxy(AdminManager adminManager, User currentUser) {
        this.adminManager = adminManager;
        this.currentUser = currentUser;
    }

    public List<User> getAllUsers() {
        try {
            List<User> users = adminManager.getAllUsers();
            logManager.log(currentUser.getUserId(), currentUser.getUsername(), "ADMIN_VIEW_USERS", "Viewed all users");
            return users;
        } catch (SecurityException e) {
            logManager.log(currentUser.getUserId(), currentUser.getUsername(), "ADMIN_VIEW_USERS_DENIED", e.getMessage());
            return List.of();
        }
    }

    public boolean createUser(String userId, String username, String password, boolean isAdmin) {
        try {
            boolean ok = adminManager.createUser(userId, username, password, isAdmin);

            if (ok) {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "ADMIN_CREATE_USER", "Created user: " + userId);
            } else {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "ADMIN_CREATE_USER_FAILED", "Failed to create user: " + userId);
            }

            return ok;
        } catch (SecurityException e) {
            logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                    "ADMIN_CREATE_USER_DENIED", e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String userId) {
        try {
            boolean ok = adminManager.deleteUser(userId);

            if (ok) {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "ADMIN_DELETE_USER", "Deleted user: " + userId);
            } else {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "ADMIN_DELETE_USER_FAILED", "Failed to delete user: " + userId);
            }

            return ok;
        } catch (SecurityException e) {
            logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                    "ADMIN_DELETE_USER_DENIED", e.getMessage());
            return false;
        }
    }

    public boolean updateAdminStatus(String userId, boolean isAdmin) {
        try {
            boolean ok = adminManager.updateAdminStatus(userId, isAdmin);

            if (ok) {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "ADMIN_UPDATE_ROLE", "Updated admin status for: " + userId);
            } else {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "ADMIN_UPDATE_ROLE_FAILED", "Failed to update admin status for: " + userId);
            }

            return ok;
        } catch (SecurityException e) {
            logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                    "ADMIN_UPDATE_ROLE_DENIED", e.getMessage());
            return false;
        }
    }
    
    public Map<String, String> viewScrollStats(User adminUser) {
        if (!adminUser.getAdmin()) {
            logManager.log(adminUser.getUserId(), adminUser.getUsername(),
                "VIEW_SCROLL_STATS_FAILED", "User is not an admin");
            System.out.println("Access denied: Admins only.");
            return Map.of();
        }

        try {
            Map<String, String> stats = adminManager.viewScrollStats();
            logManager.log(adminUser.getUserId(), adminUser.getUsername(),
                "VIEW_SCROLL_STATS", "Accessed scroll statistics");
            return stats;
        } catch (Exception e) {
            logManager.log(adminUser.getUserId(), adminUser.getUsername(),
                "VIEW_SCROLL_STATS_FAILED", e.getMessage());
            System.out.println("Error viewing scroll statistics: " + e.getMessage());
            return Map.of();
        }
    }


    public User viewAsUser(String targetUserId) {
        try {
            User user = adminManager.viewAsUser(targetUserId);
            if (user != null) {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "VIEW_AS_USER", "Viewed data of user: " + targetUserId);
            } else {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "VIEW_AS_USER_FAILED", "Failed to view user: " + targetUserId);
            }
            return user;
        } catch (SecurityException e) {
            logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                    "VIEW_AS_USER_DENIED", e.getMessage());
            return null;
        }
    }

    public ScrollManager.ScrollTextPreview viewAsGuest(String scrollId) {
        try {
            ScrollManager.ScrollTextPreview preview = adminManager.viewAsGuest(scrollId);
            if (preview != null) {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "VIEW_AS_GUEST", "Previewed scroll: " + scrollId);
            } else {
                logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                        "VIEW_AS_GUEST_FAILED", "Failed to preview scroll: " + scrollId);
            }
            return preview;
        } catch (SecurityException e) {
            logManager.log(currentUser.getUserId(), currentUser.getUsername(),
                    "VIEW_AS_GUEST_DENIED", e.getMessage());
            return null;
        }
    }
 
}
