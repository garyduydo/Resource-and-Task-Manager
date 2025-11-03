import java.util.List;

public class UserManagerProxy {
    private final UserManager userManager;
    private final EventLogManager logManager = EventLogManager.getInstance();

    public UserManagerProxy(UserManager userManager) {
        this.userManager = userManager;
    }

    public User createUser(String userId, String username, String password) throws UserAlreadyExistsException {
        try {
            User user = userManager.createUser(userId);
            if (user == null) {
                throw new RuntimeException("Failed to create user");
            }

            user.setUsername(username);
            boolean passwordSet = userManager.setPassword(user, password);

            if (passwordSet) {
                logManager.log(user.getUserId(), user.getUsername(), "CREATE_USER", "User created successfully");
            } else {
                logManager.log(user.getUserId(), user.getUsername(), "CREATE_USER_FAILED", "Failed to set password");
            }

            return user;
        } catch (UserAlreadyExistsException e) {
            throw e; 
        } catch (Exception e) {
            logManager.log("SYSTEM", "SYSTEM", "CREATE_USER_FAILED", e.getMessage());
            return null;
        }
    }

    public void logout(User user) {
        if (user != null) {
            logManager.log(user.getUserId(), user.getUsername(), "LOGOUT", "User logged out");
        }
    }

    public User login(String username, String password) {
        try {
            User user = userManager.findUserByUsername(username);
            if (user == null) {
                logManager.log("UNKNOWN", username, "LOGIN_FAILED", "User not found");
                return null;
            }

            boolean passwordCorrect = userManager.checkPassword(user, password);

            if (passwordCorrect) {
                logManager.log(user.getUserId(), user.getUsername(), "LOGIN_SUCCESS", "User logged in");
                return user;
            } else {
                logManager.log(user.getUserId(), user.getUsername(), "LOGIN_FAILED", "Incorrect password");
                return null;
            }
        } catch (Exception e) {
            logManager.log("SYSTEM", "SYSTEM", "LOGIN_ERROR", e.getMessage());
            return null;
        }
    }

    public boolean updateProfile(User user, String field, String newValue) {
        try {
            boolean updated = false;

            switch (field.toLowerCase()) {
                case "username" -> updated = user.setUsername(newValue);
                case "name" -> updated = user.setName(newValue);
                case "email" -> updated = user.setEmail(newValue);
                case "phone" -> updated = user.setPhone(newValue);
                default -> {
                    logManager.log(user.getUserId(), user.getUsername(), "UPDATE_PROFILE_FAILED", "Invalid field: " + field);
                    return false;
                }
            }

            if (updated) {
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_PROFILE", "Updated field: " + field);
            }

            return updated;

        } catch (Exception e) {
            logManager.log(user.getUserId(), user.getUsername(), "UPDATE_PROFILE_FAILED", e.getMessage());
            return false;
        }
    }

    public boolean changePassword(User user, String newPassword) {
        try {
            boolean updated = userManager.setPassword(user, newPassword);

            if (updated) {
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_PASSWORD", "Password updated");
            } else {
                logManager.log(user.getUserId(), user.getUsername(), "UPDATE_PASSWORD_FAILED", "Password update failed");
            }

            return updated;
        } catch (Exception e) {
            logManager.log(user.getUserId(), user.getUsername(), "CHANGE_PASSWORD_FAILED", e.getMessage());
            return false;
        }
    }

    public List<User> getAllUsers() {
        try {
            List<User> users = userManager.getAllUsers();
            logManager.log("SYSTEM", "SYSTEM", "VIEW_ALL_USERS", "Retrieved user list");
            return users;
        } catch (Exception e) {
            logManager.log("SYSTEM", "SYSTEM", "VIEW_ALL_USERS_FAILED", e.getMessage());
            return List.of();
        }
    }

    public boolean changeUserId(String oldId, String newId) {
        try {
            boolean changed = userManager.changeUserId(oldId, newId);

            if (changed) {
                logManager.log(oldId, newId, "CHANGE_USER_ID", "User ID changed");
            } else {
                logManager.log(oldId, newId, "CHANGE_USER_ID_FAILED", "Failed to change User ID");
            }

            return changed;
        } catch (Exception e) {
            logManager.log(oldId, newId, "CHANGE_USER_ID_FAILED", e.getMessage());
            return false;
        }
    }

}
