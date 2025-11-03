import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    public final AdminManagerProxy adminProxy;
    public final AdminManager adminManager;
    public final UserManagerProxy userProxy;
    public final ScrollManagerProxy scrollProxy;
    private final UserManager realUserManager;
    private final ScrollManager realScrollManager;
    public final User currentUser;
    public final Scanner scanner = new Scanner(System.in);

    public AdminMenu(AdminManager adminManager, UserManager userManager, ScrollManager scrollManager, User currentUser) {
        this.currentUser = currentUser;
        this.adminManager = adminManager;
        this.adminProxy = new AdminManagerProxy(adminManager, currentUser);
        this.userProxy = new UserManagerProxy(userManager);
        this.scrollProxy = new ScrollManagerProxy(scrollManager);
        this.realUserManager = userManager;
        this.realScrollManager = scrollManager;
    }

    public void show() {
        while (true) {
            System.out.println("\n=== Admin Menu ===");
            System.out.println("1. View all users");
            System.out.println("2. Create new user");
            System.out.println("3. Delete user");
            System.out.println("4. Update admin status");
            System.out.println("5. View event logs");
            System.out.println("6. View scroll statistics");
            System.out.println("7. Spectate as Guest/Regular User");
            System.out.println("8. Exit admin menu");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> createUser();
                case "3" -> deleteUser();
                case "4" -> updateAdminStatus();
                case "5" -> viewEventLogs();
                case "6" -> viewScrollStats();
                case "7" -> viewAsGuestOrRegularUser();
                case "8" -> {
                    System.out.println("Exiting admin menu");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public void viewAllUsers() {
        try {
            List<User> users = adminProxy.getAllUsers();

            if (users.isEmpty()) {
                System.out.println("(No users found)\n");
                return;
            }

            System.out.printf("%-10s %-20s %-10s%n", "USER ID", "USERNAME", "ROLE");

            for (User u : users) {
                if (u == null) continue;
                String role = u.getAdmin() ? "Admin" : "User";
                System.out.printf("%-10s %-20s %-10s%n", 
                    u.getUserId(), 
                    u.getUsername(), 
                    role
                );
            }
            System.out.println();
        } catch (SecurityException e) {
            System.out.println(e.getMessage());
        }
    }


    public void createUser() {
        System.out.print("Enter user ID: ");
        String userId = scanner.nextLine().trim();

        if (userId.isBlank()) {
            System.out.println("User ID cannot be empty.");
            return;
        }
        
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Is admin (yes/true or no/false): ");
        String adminInput = scanner.nextLine().trim().toLowerCase();
        boolean isAdmin = adminManager.parseBooleanInput(adminInput);

        try {
            if (adminProxy.createUser(userId, username, password, isAdmin)) {
                System.out.println("User created successfully.");
            } else {
                System.out.println("Failed to create user.");
            }
        } catch (SecurityException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteUser() {
        System.out.print("Enter user ID to delete: ");
        String userId = scanner.nextLine().trim();
        if (userId.isBlank()) {
            System.out.println("User ID cannot be empty.");
            return;
        }

        System.out.print("Are you sure you want to delete user '" + userId + "'? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        try {
            if (adminProxy.deleteUser(userId)) {
                System.out.println("User deleted.");
            } else {
                System.out.println("Failed to delete user.");
            }
        } catch (SecurityException e) {
            System.out.println(e.getMessage());
        }
    }

    public void updateAdminStatus() {
        System.out.print("Enter user ID: ");
        String userId = scanner.nextLine().trim();
        if (userId.isBlank()) {
            System.out.println("User ID cannot be empty.");
            return;
        }

        System.out.print("Set as admin (yes/true or no/false): ");
        String input = scanner.nextLine().trim().toLowerCase();
        boolean isAdmin = adminManager.parseBooleanInput(input);

        try {
            if (adminProxy.updateAdminStatus(userId, isAdmin)) {
                System.out.println("Admin status updated.");
            } else {
                System.out.println("Failed to update admin status.");
            }
        } catch (SecurityException e) {
            System.out.println(e.getMessage());
        }
    }

    public void viewEventLogs() {
        EventLogManager logManager = EventLogManager.getInstance();
        System.out.println("\n=== Event Logs ===");

        for (String log : logManager.getAllLogs()) {
            System.out.println(log);
        }

        System.out.println("\nDo you want to filter the logs? (yes/no): ");
        String filterChoice = scanner.nextLine().trim().toLowerCase();
        if (!filterChoice.equals("yes") && !filterChoice.equals("y")) {
            return;
        }

        System.out.print("Filter by User ID (leave empty to skip): ");
        String userIdFilter = scanner.nextLine().trim();

        System.out.print("Filter by Username (leave empty to skip): ");
        String usernameFilter = scanner.nextLine().trim();

        System.out.println("\n=== Filtered Event Logs ===");
        for (String log : logManager.getAllLogs()) {
            String logUserId = log.replaceAll(".*userId=(\\S+).*", "$1");
            String logUsername = log.replaceAll(".*user=(\\S+).*", "$1");

            boolean matchesUserId = userIdFilter.isEmpty() || logUserId.equals(userIdFilter);
            boolean matchesUsername = usernameFilter.isEmpty() || logUsername.equals(usernameFilter);

            if (matchesUserId && matchesUsername) {
                System.out.println(log);
            }
        }
    }

    public void viewScrollStats() {
        System.out.println("\n=== Scroll Statistics ===");

        adminProxy.viewScrollStats(currentUser);
        
        var stats = adminManager.viewScrollStats();
        if (stats.isEmpty()) {
            System.out.println("No scrolls available.");
            return;
        }

        for (var entry : stats.entrySet()) {
            System.out.println("Scroll ID: " + entry.getKey());
            System.out.println("  " + entry.getValue());
            System.out.println();
        }
    }

    public void viewAsGuestOrRegularUser() {
        System.out.print("Spectate as (guest/user): ");
        String choice = scanner.nextLine().trim().toLowerCase();

        if (choice.equals("guest")) {
            System.out.println("\nEntering guest view.\n");

            // Temporary UI for guest
            UserInterface guestUI = new UserInterface(realUserManager, realScrollManager);
            guestUI.setToGuest(); 
            guestUI.start();      

            System.out.println("Exiting guest view. Returning to admin menu...\n");

        } else if (choice.equals("user")) {
            System.out.print("Enter target user ID: ");
            String userId = scanner.nextLine().trim();
            var user = adminProxy.viewAsUser(userId);

            if (user != null) {
                System.out.println("Viewing as user: " + user.getUserId() + " (" + user.getName() + ")");

                // Temporary UI for regular user
                SandboxEnvironment sandbox = new SandboxEnvironment(
                    user, realUserManager, realScrollManager
                );

                UserInterface tempUI = new UserInterface(
                    sandbox.getUserManager(),
                    sandbox.getScrollManager()
                );
                tempUI.setUserLoggedIn(sandbox.getUser());
                tempUI.start();

                sandbox.cleanup();

                System.out.println("Exiting sandbox view (no real data affected). Returning to admin menu...");

            } else {
                System.out.println("Cannot view user.");
            }

        } else {
            System.out.println("Invalid choice.");
        }
    }

}
