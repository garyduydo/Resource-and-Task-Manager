import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.text.SimpleDateFormat;


public class UserInterface {

    private final UserManager realUserManager;  // original logic
    private final UserManagerProxy userManager; // proxy for logging
    private final ScrollManager realScrollManager;
    private final ScrollManagerProxy scrollManager;
    private User userLoggedIn;
    private boolean isGuest;
    private Scanner scanner;

    public UserInterface(UserManager realUserManager, ScrollManager realScrollManager) {
        this.realUserManager = realUserManager;         // original
        this.userManager = new UserManagerProxy(realUserManager); // proxy
        this.realScrollManager = realScrollManager; // original
        this.scrollManager = new ScrollManagerProxy(realScrollManager); // proxy
        this.scanner = new Scanner(System.in);
        this.isGuest = false;
        this.userLoggedIn = null;

        initRootAdmin();
    }

    // For testing purposes
    public void setUserLoggedIn(User user) {
        this.userLoggedIn = user;
        this.isGuest = false;
    }

    private void initRootAdmin() {
        try {
            User root = realUserManager.createUser("root");
            if (root != null) {
                root.setUsername("rootadmin");
                root.setName("root");
                root.setAdmin(true);
                userManager.changePassword(root, "rootpass");
            }
        } catch (Exception ignored) {}
    }

    // Create a start page
    public void start(){

        while(true){
            showMenu();
            String choice = scanner.nextLine().trim();
            System.out.println();

            if(userLoggedIn == null && !isGuest){
                switch (choice) {
                    case "1":
                        // register new user
                        registerUser();
                        break;
                    case "2":
                        // log into existing user
                        userLogIn();
                        break;
                    case "3":
                        // logging in as guest
                        setToGuest();
                        break;
                    case "0":
                        // exiting program
                        System.out.println("Exiting program...\n");
                        return;
                    default:
                        System.out.println("Incorrect Input. Please Enter A Valid Number \n");
                        break;
                }
            } else if (isGuest){
                // Guest cases -> manage scrolls or logout from
                guestInterface(choice);
            } else {
                if (userLoggedIn.getAdmin()) {
                    switch (choice) {
                        case "1":
                            scrollManagement();
                            break;
                        case "2":
                            updateUser();
                            break;
                        case "3":// open AdminMenu
                            ScrollManager scrollManager = new ScrollManager("vsas_data/scrolls");
                            AdminManager adminManager = new AdminManager("vsas_data/users", userLoggedIn, scrollManager);
                            AdminMenu adminMenu = new AdminMenu(adminManager, realUserManager, scrollManager, userLoggedIn);
                            adminMenu.show();
                            break;
                        case "4":
                            logOutUser();
                            break;
                        default:
                            System.out.println("Incorrect Input. Please Enter A Valid Number");
                    }
                } else {
                    // handle logged in user cases
                    switch (choice) {
                        case "1":
                            scrollManagement();
                            break;
                        case "2":
                            updateUser();
                            break;
                        case "3":
                            logOutUser();
                            break;
                        default:
                            System.out.println("Invalid option.");
                    }
                }
            }
        }
    }

    public void showMenu(){
        System.out.println("=== Welcome to VSAS ===");

        // if current user option
        if (userLoggedIn != null){
            System.out.println("Logged In As " + userLoggedIn.getName() + " (" + (userLoggedIn.getAdmin() ? "Admin" : "User") + ")");
                // admin interface
                if (userLoggedIn.getAdmin()) {
                    System.out.println("1. Manage Scrolls");
                    System.out.println("2. Change User Information");
                    System.out.println("3. Admin Menu");
                    System.out.println("4. Logout");
                } else {
                    // regular user
                    System.out.println("1. Manage Scrolls");
                    System.out.println("2. Change User Information");
                    System.out.println("3. Logout");
                }
        }

        // if guest display
        else if (isGuest == true){
            System.out.println("Logged In As Guest");
            System.out.println("1. View Scrolls");
            System.out.println("2. Logout From Guest");
        }

        // if signing in or logging in display
        else{
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Enter As Guest");
            System.out.println("0. Exit");
        }

        System.out.print("Enter Option: ");
    }

    public void scrollMenu(){
        System.out.println("=== Manage Scrolls ===");

        // might give option where it displays all options for user while only view for guest
        System.out.println("Logged In As " + userLoggedIn.getName() + " (" + (userLoggedIn.getAdmin() ? "Admin" : "User") + ")");
        System.out.println("1. View Scrolls");
        System.out.println("2. Add Scrolls"); // vince
        System.out.println("3. Modify Scrolls"); // vince
        System.out.println("4. Delete Scrolls"); // vince
        System.out.println("5. Search Scrolls");
        System.out.println("0. Return to " + (userLoggedIn.getAdmin() ? "Admin" : "User") + " menu");

        System.out.print("Enter Option: ");
    }


    // Registration interface -> ask for fullname, username, password, phone_num, email_addr,
    public void registerUser(){
        System.out.println("=== Sign Up ===");

        String userId, username, fullName, password, phone, email;

        while (true){
            System.out.print("Enter user ID: ");
            userId = scanner.nextLine().trim();

            if (userId.isEmpty()){
                System.out.println("User ID cannot be empty.");
                continue;
            }

            if (realUserManager.getUser(userId) != null) {
                System.out.println("User ID already exists. Please select another Id");
                continue;
            }

            break;
        }

        while (true){
            System.out.print("Enter username: ");
            username = scanner.nextLine().trim();

            if (username.isEmpty()){
                System.out.println("Username cannot be empty");
                continue;
            }

            if (!username.matches("^[A-Za-z0-9_]+$")){
                System.out.println("Invalid username. Must contain letters, digits or underscores only");
                continue;
            }

            if (realUserManager.findUserByUsername(username) != null){
                System.out.println("Username already taken. Please select another username");
                continue;
            }

            break;
        }

        while (true){
            System.out.print("Enter full name: ");
            fullName = scanner.nextLine().trim();

            if (fullName.isEmpty()){
                System.out.println("Full name cannot be empty");
                continue;
            } else if (!fullName.matches("^[A-Za-z ]+$")){
                System.out.println("Full name should only consists of english");
                continue;
            }

            break;
        }

        while (true){
            System.out.print("Enter password: ");
            Console c = System.console();
            if (c != null)
            {
                password = new String(c.readPassword()).trim();
            }
            else
            {
                password = scanner.nextLine().trim();
            }

            if (password.isEmpty()){
                System.out.println("Password cannot be empty.");
                continue;
            }

            break;
        }

        while (true) {
            System.out.print("Enter phone number: ");
            phone = scanner.nextLine().trim();

            if (!phone.matches("\\d{8,18}")){
                System.out.println("Invalid phone number. Use only digits (8-15 long)");
                continue;
            } else {
                boolean phone_used = false;
                for (User u : userManager.getAllUsers())
                {
                    if (u.getPhone().equals(phone))
                    {
                        System.out.println(String.format("Phone number %s is already in use by another user.", phone));
                        phone_used = true;
                        break;
                    }
                }
                if (! phone_used) break;
            }
        }

        while (true) {
            System.out.print("Enter email address: ");
            email = scanner.nextLine().trim();

            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")){
                System.out.println("Invalid email format. Please enter a valid English email (e.g. name@example.com)");
                continue;
            } else {
                boolean email_used = false;
                for (User u : userManager.getAllUsers())
                {
                    if (u.getEmail().equals(email))
                    {
                        System.out.println(String.format("Email address %s is already in use by another user.", email));
                        email_used = true;
                        break;
                    }
                }
                if (! email_used) break;
            }
        }

        try{
            User newUser = userManager.createUser(userId, username, password);

            newUser.setName(fullName);
            newUser.setPhone(phone);
            newUser.setEmail(email);

            if (!userManager.changePassword(newUser, password)){
                System.out.println("Error: Failed to securely set password.\n");
                return;
            }

            userLoggedIn = newUser;
            System.out.println("New User Successfully Registered!\n");

        } catch (UserAlreadyExistsException e) {
            System.out.println("User with entered 'userId' already exists. Enter a unique Id.\n");
        } catch (Exception e) {
            System.out.println("Error: Could not create user. " + e.getMessage());
        }
    }

    // User login
    public void userLogIn(){
        System.out.println("=== Log Into User Account ===");

        // Log in using username and password
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        User u = realUserManager.findUserByUsername(username);
        if (u == null){
            System.out.println("Error: User does not exist.\n");

            userManager.login(username, "");
            return;
        }

        String password;
        System.out.print("Enter password: ");
        Console c = System.console();
        if (c != null)
        {
            password = new String(c.readPassword()).trim();
        }
        else
        {
            password = scanner.nextLine().trim();
        }

        User loggedInUser = userManager.login(username, password);
        if (loggedInUser == null) {
            System.out.println("Error: Incorrect password.\n");
            return;
        }

        userLoggedIn = loggedInUser;
        System.out.println("User Successfully Logged In!\n");
    }

    // guest page -> able to view scrolls (may not upload or download)
    public void guestInterface(String choice){
        switch (choice) {
            case "1": // will implement scrolls display later
                viewScrolls();
                break;
            case "2": // log out from guest and bring back to start page
                logOutUser();
                break;
            default:
                System.out.println("Incorrect Input. Please Enter A Valid Number.\n");
                break;
        }
    }

    // display profile
    public void updateUser(){

        String choice = "null";

        do{
            System.out.println("=== Profile ===");
            System.out.println("1. Username: " + userLoggedIn.getUsername());
            System.out.println("2. Fullname: " + userLoggedIn.getName());
            System.out.println("3. Phone Number: " + userLoggedIn.getPhone());
            System.out.println("4. Email Address: " + userLoggedIn.getEmail());
            System.out.println("5. Update Password");
            System.out.println("0. Exit\n");

            System.out.print("Enter number to update: ");
            choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1": {
                    while (true) {
                        System.out.print("Enter new username (0 to cancel): ");
                        String newUsername = scanner.nextLine().trim();

                        if (newUsername.isEmpty()) {
                            System.out.println("Username cannot be empty.\n");
                            continue;
                        }
                        if (!newUsername.matches("^[A-Za-z0-9_]+$")) {
                            System.out.println("Invalid username. Use only letters, digits, or underscores.\n");
                            continue;
                        }
                        if (newUsername.equals(userLoggedIn.getUsername())) {
                            System.out.println("You entered the same username as before.\n");
                            continue;
                        }
                        if (realUserManager.findUserByUsername(newUsername) != null) {
                            System.out.println("That username is already taken. Please choose another.\n");
                            continue;
                        }

                        if(newUsername.equals("0")){
                            System.out.println("Returning to Profile...\n");
                            break;
                        }

                        userLoggedIn.setUsername(newUsername);
                        System.out.println("Username updated successfully!\n");
                        break;
                    }
                    break;
                }

                case "2": {
                    while (true) {
                        System.out.print("Enter new full name (0 to cancel): ");
                        String newName = scanner.nextLine().trim();

                        if (newName.equals("0")){
                            System.out.println("Returning to Profile...\n");
                            break;
                        }

                        if (newName.isEmpty()) {
                            System.out.println("Full name cannot be empty.\n");
                            continue;
                        }
                        if (!newName.matches("^[A-Za-z ]+$")) {
                            System.out.println("Full name should only contain English letters and spaces.\n");
                            continue;
                        }
                        if (newName.equals(userLoggedIn.getName())) {
                            System.out.println("You entered the same full name as before.\n");
                            continue;
                        }

                        userLoggedIn.setName(newName);
                        System.out.println("Full name updated successfully!\n");
                        break;
                    }
                    break;
                }

                case "3": {
                    while (true) {
                        System.out.print("Enter new phone number (0 to cancel): ");
                        String newPhone = scanner.nextLine().trim();

                        if (newPhone.equals("0")){
                            System.out.println("Returning to Profile...\n");
                            break;
                        }

                        if (!newPhone.matches("\\d{8,18}")) {
                            System.out.println("Invalid phone number. Use only digits (8–18 long).\n");
                            continue;
                        }
                        if (newPhone.equals(userLoggedIn.getPhone())) {
                            System.out.println("You entered the same phone number as before.\n");
                            continue;
                        }

                        boolean phoneUsed = false;
                        for (User u : userManager.getAllUsers()) {
                            if (!u.equals(userLoggedIn) && u.getPhone().equals(newPhone)) {
                                System.out.println("Phone number " + newPhone + " is already in use by another user.\n");
                                phoneUsed = true;
                                break;
                            }
                        }
                        if (phoneUsed) continue;

                        userLoggedIn.setPhone(newPhone);
                        System.out.println("Phone number updated successfully!\n");
                        break;
                    }
                    break;
                }

                case "4": {
                    while (true) {
                        System.out.print("Enter new email address (0 to cancel): ");
                        String newEmail = scanner.nextLine().trim();

                        if (newEmail.equals("0")){
                            System.out.println("Returning to Profile...\n");
                            break;
                        }

                        if (!newEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                            System.out.println("Invalid email format. Please use a valid format (e.g., name@example.com).\n");
                            continue;
                        }
                        if (newEmail.equals(userLoggedIn.getEmail())) {
                            System.out.println("You entered the same email as before.\n");
                            continue;
                        }

                        boolean emailUsed = false;
                        for (User u : userManager.getAllUsers()) {
                            if (!u.equals(userLoggedIn) && u.getEmail().equalsIgnoreCase(newEmail)) {
                                System.out.println("Email address " + newEmail + " is already in use by another user.\n");
                                emailUsed = true;
                                break;
                            }
                        }
                        if (emailUsed) continue;

                        userLoggedIn.setEmail(newEmail);
                        System.out.println("Email updated successfully!\n");
                        break;
                    }
                    break;
                }

                case "5": {
                    while (true) {
                        System.out.print("Enter new password (0 to cancel): ");
                        String newPassword = scanner.nextLine().trim();

                        if (newPassword.equals("0")){
                            System.out.println("Returning to Profile...\n");
                            break;
                        }

                        if (newPassword.isEmpty()) {
                            System.out.println("Password cannot be empty.\n");
                            continue;
                        }
                        if (!userManager.changePassword(userLoggedIn, newPassword)) {
                            System.out.println("Error: Failed to securely update password.\n");
                            continue;
                        }

                        System.out.println("Password updated successfully!\n");
                        break;
                    }
                    break;
                }

                case "0":
                    System.out.println("Exiting User Profile...\n");
                    break;

                default:
                    System.out.println("Incorrect Input. Please Enter A Valid Number \n");
                    break;
            }
        } while (!choice.equals("0"));
    }

    public void scrollManagement(){
        String choice;
        do {
            scrollMenu();
            choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    // View Scrolls
                    viewScrolls();
                    break;
                case "2":
                    // Add scrolls
                    addScrolls();
                    break;
                case "3":
                    // Modify scrolls
                    modifyScrolls();
                    break;
                case "4":
                    // Delete scrolls
                    deleteScrolls();
                    break;
                case "5":
                    // Search scrolls
                    searchScrolls();
                    break;
                case "0":
                    // exit menu
                    System.out.println("Returning to " + (userLoggedIn.getAdmin() ? "Admin" : "User") + " menu\n");
                    break;
                default:
                    System.out.println("Invalid Choice. Please Enter A Valid Choice \n");
                    break;
            }
        } while (!choice.equals("0")); // loop until user chooses 0
    }

    public void addScrolls() {
        System.out.println("=== Adding Scrolls ===");
        System.out.println("Logged In As " + userLoggedIn.getName() + " (" + (userLoggedIn.getAdmin() ? "Admin" : "User") + ")");

        String scrollId, scrollName;
        while (true) {
            System.out.print("Enter a unique Scroll ID: ");
            scrollId = scanner.nextLine().trim();

            if (scrollId.isEmpty()) {
                System.out.println("Scroll ID cannot be empty.");
                continue;
            }
            if (realScrollManager.getScroll(scrollId) != null) {
                System.out.println("A scroll with that ID already exists. Try another.");
                continue;
            }
            break;
        }

        while(true){
            System.out.print("Enter Scroll Name: ");
            scrollName = scanner.nextLine().trim();

            if (scrollName.isEmpty()) {
                System.out.println("Scroll name cannot be empty.");
                continue;
            }

            if (scrollManager.findScrollByName(userLoggedIn, scrollName) != null) {
                System.out.println("A scroll with that name already exists. Please enter a different name.");
                continue;
            }

            break;
        }

        System.out.print("Enter path to the text file to upload: ");
        String filePath = scanner.nextLine().trim();
        Path sourcePath = Path.of(filePath);

        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            System.out.println("File does not exist or is not a valid file.");
            return;
        }

        try {
            Scroll newScroll = scrollManager.createScroll(userLoggedIn,scrollId);
            if (newScroll == null) {
                throw new ScrollAlreadyExistsException("Scroll with ID " + scrollId + " already exists.");
            }

            newScroll.setScrollName(scrollName);
            newScroll.setUploaderId(userLoggedIn.getUserId());
            newScroll.setUploadDate(new Date());

            // Get path and copy contents of file
            Path blobPath = newScroll.getFileObj().toPath().resolve("scroll_blob");
            Files.copy(sourcePath, blobPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Scroll successfully added!\n");
            System.out.println("Upload timestamp: " + newScroll.getUploadDate() + "\n");

        } catch (ScrollAlreadyExistsException e) {
            System.out.println("A scroll with that ID already exists. Try again.\n");
        } catch (IOException e) {
            System.out.println("Error: Failed to upload file. " + e.getMessage());
        }
    }

    public void modifyScrolls() {
        System.out.println("=== Modifying Scrolls ===");
        System.out.println("Logged In As " + userLoggedIn.getName() + " (" + (userLoggedIn.getAdmin() ? "Admin" : "User") + ")\n");

        // Get all scrolls uploaded by the logged-in user
        List<Scroll> myScrolls = realScrollManager.searchScrolls(userLoggedIn.getUserId(), null, null, null, null);

        if (myScrolls.isEmpty()) {
            System.out.println("You have no scrolls to modify.\n");
            return;
        }

        System.out.println("Your scrolls:");
        for (int i = 0; i < myScrolls.size(); i++) {
            Scroll scroll = myScrolls.get(i);
            System.out.println((i + 1) + ". " + scroll.getScrollId() + " - " + scroll.getScrollName());
        }

        // Choose scroll to modify
        int choice = 0;
        while (true) {
            System.out.print("\nEnter the number of the scroll to modify (0 to cancel): ");
            String input = scanner.nextLine().trim();
            try {
                choice = Integer.parseInt(input);
                if (choice == 0) return;
                if (choice < 1 || choice > myScrolls.size()) {
                    System.out.println("Invalid choice. Try again.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        Scroll modifyingScroll = myScrolls.get(choice - 1);

        // Modify Scroll ID
        while (true) {
            System.out.print("Enter new Scroll ID (or press Enter to keep '" + modifyingScroll.getScrollId() + "'): ");
            String newId = scanner.nextLine().trim();
            if (newId.isEmpty() || newId.equals(modifyingScroll.getScrollId())) {
                break;
            }
            if (realScrollManager.getScroll(newId) != null) {
                System.out.println("Scroll ID already exists. Try another.");
                continue;
            }
            try {
                boolean success = scrollManager.changeScrollId(userLoggedIn, modifyingScroll.getScrollId(), newId);
                if (success) {
                    modifyingScroll = realScrollManager.getScroll(newId);
                    System.out.println("Scroll ID updated successfully.");
                } else {
                    System.out.println("Failed to change Scroll ID.");
                }
                break;
            } catch (Exception e) {
                System.out.println("Failed to change Scroll ID: " + e.getMessage());
            }
        }

        // Modify Scroll Name
        while (true) {
            System.out.print("Enter new Scroll Name (or press Enter to keep '" + modifyingScroll.getScrollName() + "'): ");
            String newName = scanner.nextLine().trim();
            if (newName.isEmpty() || newName.equals(modifyingScroll.getScrollName())) {
                break;
            }
            if (scrollManager.findScrollByName(userLoggedIn,newName) != null) {
                System.out.println("A scroll with that name already exists. Try another.");
                continue;
            }
            boolean success = scrollManager.updateScrollName(userLoggedIn, modifyingScroll.getScrollId(), newName);
            if (success) {
                System.out.println("Scroll Name updated successfully.");
            } else {
                System.out.println("Failed to update Scroll Name.");
            }
            break;
        }

        // Upload New File
        while (true) {
            System.out.print("Enter path to new file to replace existing scroll (or press Enter to keep current file): ");
            String filePath = scanner.nextLine().trim();
            if (filePath.isEmpty()) {
                break;
            }

            Path sourcePath = Path.of(filePath);
            if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
                System.out.println("File does not exist or is not valid. Try again.");
                continue;
            }

            Path blobPath = modifyingScroll.getFileObj().toPath().resolve("scroll_blob");
            try {
                Files.copy(sourcePath, blobPath, StandardCopyOption.REPLACE_EXISTING);
                boolean success = scrollManager.updateScrollFile(userLoggedIn, modifyingScroll.getScrollId(), blobPath.toString());
                if (success) {
                    System.out.println("File replaced successfully. Updated timestamp: " + modifyingScroll.getUploadDate());
                } else {
                    System.out.println("Failed to update scroll file.");
                }
                break;
            } catch (IOException e) {
                System.out.println("Failed to replace file: " + e.getMessage());
            }
        }

        // timestamps not being recorded with changes
        // System.out.println("Upload timestamp: " + modifyingScroll.getUploadDate() + "\n");
        System.out.println("\nScroll modification complete.\n");
    }

    public void deleteScrolls(){
        System.out.println("=== Deleting Scrolls ===");
        System.out.println("Logged In As " + userLoggedIn.getName() + " (" + (userLoggedIn.getAdmin() ? "Admin" : "User") + ")\n");

        // View all scrolls user has made
        List<Scroll> myScrolls = realScrollManager.searchScrolls(userLoggedIn.getUserId(), null, null, null, null);

        if (myScrolls.isEmpty()) {
            System.out.println("You have no scrolls to delete.\n");
            return;
        }

        System.out.println("Your scrolls: ");
        for(int i=0; i < myScrolls.size(); i++){
            Scroll scroll = myScrolls.get(i);
            System.out.println((i + 1) + ". " + scroll.getScrollId() + " - " + scroll.getScrollName());
        }

        int choice = 0;
        while (true){
            System.out.print("\nEnter the number of the scroll you want to delete (0 to cancel):");
            String input = scanner.nextLine().trim();

            try {
                choice = Integer.parseInt(input);
                if (choice == 0) {
                    return;
                }
                if (choice < 1 || choice > myScrolls.size()) {
                    System.out.println("Invalid choice. Try again.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        Scroll deletingScroll = myScrolls.get(choice - 1);

        while (true) {
            System.out.print("Are you sure you want to delete scroll '" + deletingScroll.getScrollName() + "'? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (confirm.equals("y")) {
                if (scrollManager.deleteScroll(userLoggedIn,deletingScroll.getScrollId())) {
                    System.out.println("\nScroll successfully deleted.\n");
                } else {
                    System.out.println("\nError: Failed to delete scroll.\n");
                }
                break;
            } else if (confirm.equals("n")) {
                System.out.println("\nDeletion cancelled.\n");
                break;
            } else {
                System.out.println("Please enter 'y' for yes or 'n' for no.");
            }
        }

    }

    private void viewScrolls()
    {
        boolean canDownload = (userLoggedIn != null) && !isGuest;

        while (true)
        {
            // Always re-query so the list reflects the latest state each loop
            List<Scroll> rows = realScrollManager.getAllScrollsSorted();
            if (rows == null || rows.isEmpty())
            {
                System.out.println("(No scrolls found)\n");
            }
            else
            {
                printScrollList(rows);
            }

            System.out.println("[P <id>] Preview   "
                + (canDownload ? "[D <id>] Download   " : "")
                + "[B]ack");
            System.out.print("Enter command: ");
            String line = scanner.nextLine().trim();

            if (line.equalsIgnoreCase("B"))
            {
                System.out.println();
                return; // back to menu
            }
            else if (line.toUpperCase().startsWith("P "))
            {
                String id = line.substring(2).trim();
                doPreview(id);
            }
            else if (canDownload && line.toUpperCase().startsWith("D "))
            {
                String id = line.substring(2).trim();
                doDownload(id);
            }
            else
            {
                System.out.println("Unknown command.\n");
            }
        }
    }

    private void printScrollList(List<Scroll> rows)
    {
        System.out.println("\nID\t\tNAME");
        for (Scroll s : rows)
        {
            if (s == null) { continue; }
            String id = s.getScrollId();
            String nm = s.getScrollName();
            System.out.println((id == null ? "-" : id) + "\t\t" + (nm == null ? "-" : nm));
        }
        System.out.println();
    }

    private void doPreview(String id)
    {
        if (id == null || id.isEmpty())
        {
            System.out.println("Provide an ID: P <id>\n");
            return;
        }

        ScrollManager.ScrollTextPreview p = realScrollManager.previewScrollText(id);
        if (p == null)
        {
            System.out.println("(No preview available or scroll not found)\n");
            return;
        }

        System.out.println("\nPreview");
        System.out.println("=======");
        System.out.println("Name: " + p.scrollName);
        System.out.println("Scroll ID: " + p.scrollId);
        System.out.println("Uploader ID: " + p.uploaderId);
        System.out.println("Date: " + nd(p.uploadDate));
        System.out.println("\n--- Text (first 500 chars) ---");
        System.out.println(p.textPreview == null ? "" : p.textPreview);
        if (p.truncated) { System.out.println("\n… (preview truncated)"); }
        System.out.println();
    }

    private void doDownload(String id)
    {
        if (id == null || id.isEmpty())
        {
            System.out.println("Provide an ID: D <id>\n");
            return;
        }

        // guests cannot download
        if (isGuest || userLoggedIn == null)
        {
            System.out.println("Guests cannot download. Please log in.\n");
            return;
        }

        System.out.print("Save as (destination path): ");
        String dest = scanner.nextLine().trim();
        if (dest.isEmpty())
        {
            System.out.println("No destination given.\n");
            return;
        }

        boolean ok = realScrollManager.downloadScroll(id, dest);
        if (ok)
        {
            System.out.println("Downloaded to: " + dest + "\n");
        }
        else
        {
            System.out.println("Download failed (bad id or missing file).\n");
        }
    }

    private void searchScrolls()
    {
        System.out.println("=== Search Scrolls ===");

        System.out.print("Uploader ID (blank = any): ");
        String uploader = scanner.nextLine().trim();
        if (uploader.isEmpty()) { uploader = null; }

        System.out.print("Scroll ID (blank = any): ");
        String scrollId = scanner.nextLine().trim();
        if (scrollId.isEmpty()) { scrollId = null; }

        System.out.print("Name contains (blank = any): ");
        String nameContains = scanner.nextLine().trim();
        if (nameContains.isEmpty()) { nameContains = null; }

        System.out.print("Uploaded after (yyyy-MM-dd, blank = none): ");
        String afterS = scanner.nextLine().trim();
        Date after = parseDayOrNull(afterS);

        System.out.print("Uploaded before (yyyy-MM-dd, blank = none): ");
        String beforeS = scanner.nextLine().trim();
        Date before = parseDayOrNull(beforeS);

        List<Scroll> rows = realScrollManager.searchScrolls(uploader, scrollId, nameContains, after, before);
        if (rows == null || rows.isEmpty())
        {
            System.out.println("\n(No matches)\n");
            return;
        }

        System.out.println("\nID\t\tNAME");
        for (Scroll s : rows)
        {
            if (s == null) { continue; }
            String id = s.getScrollId();
            String nm = s.getScrollName();
            System.out.println((id == null ? "-" : id) + "\t\t" + (nm == null ? "-" : nm));
        }
        System.out.println();
    }

    private String nd(Date d)
    {
        if (d == null) {
            return "-";
        }
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            df.setLenient(false);
            return df.format(d);
        } catch (Exception e) {
            return d.toString(); // safe fallback
        }
    }

    private Date parseDayOrNull(String ymd)
    {
        if (ymd == null || ymd.isEmpty()) { return null; }
        try
        {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
            df.setLenient(false);
            return df.parse(ymd);
        }
        catch (Exception e)
        {
            System.out.println("Invalid date, ignoring.\n");
            return null;
        }
    }


    public void logOutUser() {
        System.out.println("Logging out. Returning to start page...\n");
        userLoggedIn = null;
        isGuest = false;
    }

    public void setToGuest() {
        this.userLoggedIn = null;
        this.isGuest = true;
    }

    public User getUserLoggedIn() {
        return userLoggedIn;
    }

    public UserManagerProxy getUserManager() {
        return userManager;
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }
}
