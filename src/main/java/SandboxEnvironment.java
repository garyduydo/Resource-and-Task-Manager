import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class SandboxEnvironment {

    private final User sandboxUser;
    private final UserManager sandboxUserManager;
    private final ScrollManager sandboxScrollManager;

    private final String sandboxUserDir;
    private final String sandboxScrollDir;

    public SandboxEnvironment(User realUser, UserManager realUserManager, ScrollManager realScrollManager) {
        this(realUser, realUserManager, realScrollManager, "sandbox/users", "sandbox/scrolls");
    }

    public SandboxEnvironment(User realUser, UserManager realUserManager, ScrollManager realScrollManager,
                              String sandboxUserDir, String sandboxScrollDir) {

        this.sandboxUserDir = sandboxUserDir;
        this.sandboxScrollDir = sandboxScrollDir;

        sandboxUserManager = new UserManager(sandboxUserDir);
        sandboxScrollManager = new ScrollManager(sandboxScrollDir);

        try {
            sandboxUser = sandboxUserManager.createUser(realUser.getUserId());
        } catch (UserAlreadyExistsException e) {
            throw new RuntimeException("Sandbox user already exists: " + realUser.getUserId(), e);
        }

        sandboxUser.setUsername(realUser.getUsername());
        sandboxUser.setName(realUser.getName());
        sandboxUser.setEmail(realUser.getEmail());
        sandboxUser.setPhone(realUser.getPhone());
        sandboxUser.setAdmin(realUser.getAdmin());

        sandboxUserManager.setPassword(sandboxUser, "sandbox");

        List<Scroll> userScrolls = realScrollManager.searchScrolls(realUser.getUserId(), null, null, null, null);
        for (Scroll s : userScrolls) {
            try {
                Scroll copy = sandboxScrollManager.createScroll(s.getScrollId());
                copy.setScrollName(s.getScrollName());
                copy.setUploaderId(s.getUploaderId());
                copy.setUploadDate(s.getUploadDate());

                Path source = s.getFileObj().toPath().resolve("scroll_blob");
                Path dest = copy.getFileObj().toPath().resolve("scroll_blob");
                if (Files.exists(source)) {
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                System.err.println("Failed to copy scroll " + s.getScrollId() + ": " + e.getMessage());
            }
        }
    }

    public User getUser() { return sandboxUser; }
    public UserManager getUserManager() { return sandboxUserManager; }
    public ScrollManager getScrollManager() { return sandboxScrollManager; }

    public void cleanup() {
        deleteDirectory(new File(sandboxUserDir));
        deleteDirectory(new File(sandboxScrollDir));
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) deleteDirectory(file);
            else file.delete();
        }
        dir.delete();
    }
}
