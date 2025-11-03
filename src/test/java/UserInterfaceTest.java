import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserInterfaceTest {

    private UserManager userManager;
    private ScrollManager scrollManager;
    private UserInterface userInterface;
    private ByteArrayOutputStream outputContent = new ByteArrayOutputStream();
    private PrintStream originalOut = System.out;

    @BeforeEach
    public void setUp() {
        String userRoot = "test";
        String scrollRoot = "testScrolls";
        userManager = new UserManager(userRoot);
        scrollManager = new ScrollManager(scrollRoot);
        userInterface = new UserInterface(userManager, scrollManager);
        System.setOut(new PrintStream(outputContent));
    }

    @AfterEach
    public void tearDown() throws IOException{
        System.setIn(System.in);
        System.setOut(originalOut);
        outputContent.reset();

        Path users = Paths.get("test");
        if (Files.exists(users)) {
            Files.walk(users)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> path.toFile().delete());
        }

        Path scrolls = Paths.get("testScrolls");
        if (Files.exists(scrolls)) {
            Files.walk(scrolls)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> path.toFile().delete());
        }
    }

    private void simulateInput(String... lines) {
        String input = String.join(System.lineSeparator(), lines);
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
    }

    private User createOrGetUser(String userId, String username, String password) {
        try {
            User u = userManager.createUser(userId);
            assertNotNull(u, "createUser returned null");
            u.setUsername(username);
            // if your tests rely on passwords, set it here if needed:
            userManager.setPassword(u, password); // or changePassword via proxy if required in your codebase
            return u;
        } catch (UserAlreadyExistsException e) {
            // User already exists from a previous test run: just fetch it
            User u = userManager.getUser(userId);
            assertNotNull(u, "Expected existing user '" + userId + "' to be retrievable");
            return u;
        }
    }

    private static java.util.Date ymd(int y, int m, int d) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.YEAR, y);
        c.set(java.util.Calendar.MONTH, m - 1);
        c.set(java.util.Calendar.DAY_OF_MONTH, d);
        c.set(java.util.Calendar.HOUR_OF_DAY, 12);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Scroll seedScroll(
        String id, String name, String uploaderId, String text, java.util.Date uploadDate
    ) {
        try {
            Scroll s = scrollManager.createScroll(id);
            s.setScrollName(name);
            s.setUploaderId(uploaderId);
            s.setUploadDate(uploadDate);
            Path blob = s.getFileObj().toPath().resolve("scroll_blob");
            Files.writeString(blob, text == null ? "" : text);
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    public void testUserInterfaceCreation() {
        assertNotNull(userInterface);
    }

    @Test
    public void testStart() {
        simulateInput(
            "1",             // register
            "test",          // userID
            "test",          // username
            "John Doe",      // name
            "test",          // password
            "0412345678",    // phone
            "john@example.com", // email
            "3",             // log out
            "0"              // exit program
        );
    
        outputContent.reset();
        userInterface.start();
    
        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("New User Successfully Registered"));
        assertTrue(output.contains("Logging out. Returning to start page..."));
        assertTrue(output.contains("Exiting program..."));
    }
    

    @Test
    public void testGuestLogin(){
        String simulatedInput = String.join(System.lineSeparator(),
            "3",        // log in as guest
            "1",        // view scrolls (enters viewScrolls loop)
            "B",        // back out of the view screen
            "2",        // logout guest
            "0"         // exit app
        );

        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.start();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Logged In As Guest"), "Should display successful login as guest");
        assertTrue(output.contains("1. View Scrolls"), "Should display guest options");
        assertTrue(output.contains("2. Logout"), "Should display guest options");
        assertTrue(
            output.contains("(No scrolls found)") || output.contains("ID\t\tNAME"),
            "View screen should show a list header or '(No scrolls found)'."
        );
        assertTrue(output.contains("Logging out. Returning to start page..."), "Should display a prompt that you are logging out from start page");
        assertTrue(output.contains("Exiting program..."), "Should exit the system successfully");

    }

    @Test
    public void testShowMenuNotLoggedIn() {
        userInterface.showMenu();
        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("1. Register"));
        assertTrue(output.contains("2. Login"));
        assertTrue(output.contains("3. Enter As Guest"));
        assertTrue(output.contains("0. Exit"));
    }

    @Test
    public void testExistingUserId_noLoop() {
        String firstInput = String.join(System.lineSeparator(),
            "test",          // userId
            "testUser1",     // username
            "John Doe",      // full name
            "passwordTest",  // password
            "0412345678",    // phone
            "john@example.com" // email
        );
        System.setIn(new ByteArrayInputStream(firstInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        userInterface.registerUser();

        userInterface.logOutUser();

        String secondInput = String.join(System.lineSeparator(),
            "test",           // existing userId
            "test2",          // new userId
            "testUser2",      // username
            "Jane Doe",       // full name
            "pass2",          // password
            "0400000000",     // phone
            "jane@example.com" // email
        );
        System.setIn(new ByteArrayInputStream(secondInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));

        outputContent.reset();
        userInterface.registerUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("User ID already exists. Please select another Id"),
            "Should prompt user that inputted userId exists. They should input a unique Id.");
    }


    @Test
    public void testLoginDisplay() {
        // register user
        String registerInput = String.join(System.lineSeparator(),
            "loginUser",        // userId
            "loginUser",        // username
            "John Doe",         // full name
            "password123",      // password
            "0412345678",       // phone
            "john@example.com"  // email
        );
        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();
        userInterface.registerUser();

        // log out to test login
        userInterface.logOutUser();

        // simulate login with user info
        String loginInput = String.join(System.lineSeparator(),
            "loginUser",       // username
            "password123"      // password
        );
        System.setIn(new ByteArrayInputStream(loginInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        // perform login
        userInterface.userLogIn();

        // check if login output is correct
        String loginOutput = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(loginOutput.contains("User Successfully Logged In"));
    }

    @Test
    public void testUserDisplay() {
        // Register user
        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.registerUser();

        outputContent.reset();
        userInterface.showMenu();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Logged In As John Doe (User)"));
        assertTrue(output.contains("1. Manage Scrolls"), "Menu should include 'Manage Scrolls'");
        assertTrue(output.contains("2. Change User Information"), "Menu should include 'Change User Information'");
        assertTrue(output.contains("3. Logout"), "Menu should include 'Logout'");
    }

    @Test
    public void testUserUpdate(){
        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.registerUser();
    }

    @Test
    public void testInvalidUserId(){
        String registerInput = String.join(System.lineSeparator(),
            "", "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.registerUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("User ID cannot be empty."));
    }
    
    @Test
    public void testInvalidUsername() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "", "****", "existsName", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.registerUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Username cannot be empty"));
        assertTrue(output.contains("Invalid username. Must contain letters, digits or underscores only"));
        assertTrue(output.contains("Username already taken. Please select another username"));
    }

    @Test
    public void testInvalidUserFullname() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "", "122313132", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.registerUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Full name cannot be empty"));
        assertTrue(output.contains("Full name should only consists of english"));
    }

    @Test
    public void testInvalidUserPassword() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.registerUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Password cannot be empty."));
    }

    @Test
    public void testInvalidUserPhone() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        u.setPhone("0412345678");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "13910","0412345678", "0412345673", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.registerUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Invalid phone number. Use only digits (8-15 long)"));
        assertTrue(output.contains("Phone number 0412345678 is already in use by another user."));
    }

    @Test
    public void testInvalidUserEmail() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        u.setEmail("john@example.com");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345673", "invalidEmail", "john@example.com", "correct@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.registerUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Invalid email format. Please enter a valid English email (e.g. name@example.com)"));
        assertTrue(output.contains("Email address john@example.com is already in use by another user."));
    }

    @Test
    public void testUpdateUserInfo(){
        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        userInterface.registerUser();
        outputContent.reset();

        String updateInput = String.join(System.lineSeparator(),
            "1", "newUser",
            "2", "Jane Doe",
            "3", "0400000000",
            "4", "jane@example.com",
            "0"
        );

        System.setIn(new ByteArrayInputStream(updateInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));

        userInterface.updateUser();

        User updatedUser = userInterface.getUserLoggedIn();
        assertEquals("newUser", updatedUser.getUsername());
        assertEquals("Jane Doe", updatedUser.getName());
        assertEquals("0400000000", updatedUser.getPhone());
        assertEquals("jane@example.com", updatedUser.getEmail());
    }

    @Test
    public void testUserUpdatePassword(){
        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        userInterface.registerUser();
        outputContent.reset();

        String updateInput = String.join(System.lineSeparator(),
            "5", "updatePassword", // update password
            "0" // exit of update interface
        );

        System.setIn(new ByteArrayInputStream(updateInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));

        userInterface.updateUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Password updated successfully!"));
    }

    @Test
    public void testInvalidUpdateUsername() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        userInterface.registerUser();
        outputContent.reset();

        String incorrectUpdate = String.join(System.lineSeparator(),
            "1", "", "*****", "testUser1", "existsName", // incorrect update username
            "0", // exit updateUsername
            "0" // exit of update interface
        );

        System.setIn(new ByteArrayInputStream(incorrectUpdate.getBytes()));
        userInterface.setScanner(new Scanner(System.in));

        userInterface.updateUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Username cannot be empty."));
        assertTrue(output.contains("Invalid username. Use only letters, digits, or underscores."));
        assertTrue(output.contains("You entered the same username as before."));
        assertTrue(output.contains("That username is already taken. Please choose another."));
        assertTrue(output.contains("Returning to Profile..."));
    }

    @Test
    public void testInvalidUpdateFullName() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        userInterface.registerUser();
        outputContent.reset();

        String incorrectUpdate = String.join(System.lineSeparator(),
            "2", "", "123141", "John Doe", // incorrect update fullnamne
            "0", // return from updateFullname
            "0" // exit of update interface
        );

        System.setIn(new ByteArrayInputStream(incorrectUpdate.getBytes()));
        userInterface.setScanner(new Scanner(System.in));

        userInterface.updateUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Full name cannot be empty."));
        assertTrue(output.contains("Full name should only contain English letters and spaces."));
        assertTrue(output.contains("You entered the same full name as before."));
        assertTrue(output.contains("Returning to Profile..."));
    }

    @Test
    public void testInvalidUpdatePhone() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        u.setPhone("000000000");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        userInterface.registerUser();
        outputContent.reset();

        String incorrectUpdate = String.join(System.lineSeparator(),
            "3", "12313", "0412345678", "000000000",// incorrect update phone
            "0", 
            "0" // exit of update interface
        );

        System.setIn(new ByteArrayInputStream(incorrectUpdate.getBytes()));
        userInterface.setScanner(new Scanner(System.in));

        userInterface.updateUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Invalid phone number. Use only digits (8–18 long)."));
        assertTrue(output.contains("You entered the same phone number as before."));
        assertTrue(output.contains("Phone number 000000000 is already in use by another user."));
        assertTrue(output.contains("Returning to Profile..."));
    }

    @Test
    public void testInvalidUpdateEmail() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        u.setEmail("test2@gmail.com");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        userInterface.registerUser();
        outputContent.reset();

        String incorrectUpdate = String.join(System.lineSeparator(),
            "4", "incorrect", "john@example.com", "test2@gmail.com",// incorrect update phone
            "0", 
            "0" // exit of update interface
        );

        System.setIn(new ByteArrayInputStream(incorrectUpdate.getBytes()));
        userInterface.setScanner(new Scanner(System.in));

        userInterface.updateUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Invalid email format. Please use a valid format (e.g., name@example.com)."));
        assertTrue(output.contains("You entered the same email as before."));
        assertTrue(output.contains("Email address test2@gmail.com is already in use by another user."));
        assertTrue(output.contains("Returning to Profile..."));
    }

    @Test
    public void testInvalidUpdatePassword() throws Exception{
        User u = userManager.createUser("harry");
        u.setUsername("existsName");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        String registerInput = String.join(System.lineSeparator(),
            "testUser1", "testUser1", "John Doe", "password123", "0412345678", "john@example.com"
        );

        System.setIn(new ByteArrayInputStream(registerInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        userInterface.registerUser();
        outputContent.reset();

        String incorrectUpdate = String.join(System.lineSeparator(),
            "5", "", // incorrect update phone
            "NewPassword", 
            "0" // exit of update interface
        );

        System.setIn(new ByteArrayInputStream(incorrectUpdate.getBytes()));
        userInterface.setScanner(new Scanner(System.in));

        userInterface.updateUser();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Password cannot be empty."));
        assertTrue(output.contains("Password updated successfully!"));
    }

    @Test
    public void testGuestNameDisplay() {
        userInterface.setToGuest();
        userInterface.showMenu();
        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Logged In As Guest"));
        assertTrue(output.contains("1. View Scrolls"));
        assertTrue(output.contains("2. Logout From Guest"));
    }

    @Test
    public void testGuestScrollView(){
        userInterface.setToGuest();
        simulateInput("B"); // feed one command to exit the view loop
        outputContent.reset();

        userInterface.guestInterface("1");
        String output = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(
            output.contains("(No scrolls found)") || output.contains("ID\t\tNAME"),
            "View screen should show a list header or '(No scrolls found)'."
        );
    }

    @Test
    public void testGuestLogout(){
        userInterface.setToGuest();
        outputContent.reset();

        userInterface.guestInterface("2");
        String output = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(output.isEmpty() || !output.contains("Incorrect"), "Should not print error on logout option");
        assertNull(userInterface.getUserLoggedIn(), "Guest should be logged out after choosing option 2");
    }

    @Test
    public void testGuestInvalidOption(){
        userInterface.setToGuest();
        outputContent.reset();

        userInterface.guestInterface("3");
        String output = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(output.contains("Incorrect Input. Please Enter A Valid Number."), "Should display error message and suggestion");
    }

    @Test
    public void testInvalidMenuOption(){
        String invalidInput = String.join(System.lineSeparator(),
            "5",        // menu choice
            "0"
        );

        System.setIn(new ByteArrayInputStream(invalidInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.start();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Incorrect Input. Please Enter A Valid Number"), "Should display an error message. Prompting for a valid number");
    }

    @Test
    public void testInvalidUserMenuOption() {
        simulateInput(
            "1",             // register
            "test",          // userID
            "test",          // username
            "John Doe",      // name
            "test",          // password
            "0412345678",    // phone
            "john@example.com", // email
            "100",           // invalid menu choice
            "3",             // log out
            "0"              // exit program
        );
    
        outputContent.reset();
        userInterface.start();
    
        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Invalid option."));
    }

    @Test
    public void testInvalidUsernameLogin() {
        simulateInput(
            "1",               // register
            "test",            // userID
            "test",            // username
            "John Doe",        // name
            "test",            // password
            "0412345678",      // phone
            "john@example.com",// email
            "3",               // logout
            "2",               // login
            "wrongUsername",   // invalid username
            "password123",     // any password
            "0"                // exit
        );
    
        outputContent.reset();
        userInterface.start();
    
        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Error: User does not exist."));
    }

    @Test
    public void testInvalidPasswordLogin() {
        simulateInput(
            "1",               // register
            "test",            // userID
            "test",            // username
            "John Doe",        // name
            "correctPass",     // correct password
            "0412345678",      // phone
            "john@example.com",// email
            "3",               // logout
            "2",               // login
            "test",            // username
            "wrongPass",       // incorrect password
            "0"                // exit
        );
    
        outputContent.reset();
        userInterface.start();
    
        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Error: Incorrect password."));
    }

    @Test
    public void testInvalidUserUpdateOption() {
        simulateInput(
            "1",             // register
            "test",          // userID
            "test",          // username
            "John Doe",      // name
            "test",          // password
            "0412345678",    // phone
            "john@example.com", // email
            "2",             // choose "Change User Information" (regular user)
            "10",            // invalid update choice
            "0",             // exit update menu
            "3",             // log out
            "0"              // exit program
        );
    
        outputContent.reset();
        userInterface.start();
    
        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(output.contains("Incorrect Input. Please Enter A Valid Number"));
    }

    @Test
    public void testExistingUserId() {
        String input = String.join(System.lineSeparator(),
            "1",              // register
            "test",           // userID
            "testUser1",      // username
            "John Doe",       // full name
            "password1",      // password
            "0412345678",     // phone
            "john@example.com", // email
            "3",              // Logout
            "1",              // register again
            "test",           // existing userID -> triggers "User ID already exists" message
            "test2",          // unique userID -> accepted after the error message
            "testUser2",      // username
            "Jane Doe",       // full name
            "pass2",          // password
            "0400000000",     // phone
            "jane@example.com", // email
            "3",              // Logout
            "0"               // Exit program to end the loop
        );

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.start();

        String output = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(
            output.contains("User ID already exists. Please select another Id"),
            "Should prompt user that the entered userId already exists and ask for a unique one."
        );
    }


    @Test
    public void testAddScrolls() throws Exception{
        User u = userManager.createUser("harry");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        Path temp = Files.createTempFile("scroll_add", ".txt");
        Files.writeString(temp, "Test scrolls! Contains random info!");

        simulateInput(
            "2",                 // "Add new scroll" in scroll management menu
            "scrollTest",        // scroll ID
            "Test Scroll!",   // scroll name
            temp.toString(),     // path to upload file
            "0"                  // exit after adding
        );
        
        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out.contains("Scroll successfully added!"), "Should confirm addition");
        assertNotNull(scrollManager.getScroll("scrollTest"), "Test scroll should now exist in manager");
    }

    @Test
    public void testInvalidAddScrollId() throws Exception{
        User u = userManager.createUser("harry");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        seedScroll("test001", "Testing101", "johnDoe", "Testing once again...", ymd(2025,10,20));

        Path temp = Files.createTempFile("scroll_add", ".txt");
        Files.writeString(temp, "Test scrolls! Contains random info!");

        simulateInput(
            "2",                 // "Add new scroll" in scroll management menu
            "",        // invalid Id
            "test001",   // duplicate Id
            "test002",          // enter existing scroll name
            "ScrollAdd",            // enter scroll name
            temp.toString(),     // path to upload file
            "0"                  // exit after adding
        );
        
        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out.contains("Scroll ID cannot be empty."), "Should catch null input");
        assertTrue(out.contains("A scroll with that ID already exists. Try another."), "Should catch existing scroll with same same");
        assertTrue(out.contains("Scroll successfully added!"), "Should confirm addition");
        assertNotNull(scrollManager.getScroll("test002"), "Test scroll should now exist in manager");
    }

    @Test
    public void testInvalidAddScrollName() throws Exception{
        User u = userManager.createUser("harry");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        seedScroll("test001", "Testing101", "johnDoe", "Testing once again...", ymd(2025,10,20));

        Path temp = Files.createTempFile("scroll_add", ".txt");
        Files.writeString(temp, "Test scrolls! Contains random info!");

        simulateInput(
            "2",                 // "Add new scroll" in scroll management menu
            "scrollTest",        // scroll ID
            "",   // empty scroll name
            "Testing101",          // enter existing scroll name
            "ScrollAdd",            // enter scroll name
            temp.toString(),     // path to upload file
            "0"                  // exit after adding
        );
        
        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out.contains("Scroll name cannot be empty."), "Should catch null input");
        assertTrue(out.contains("A scroll with that name already exists. Please enter a different name."), "Should catch existing scroll with same same");
        assertTrue(out.contains("Scroll successfully added!"), "Should confirm addition");
        assertNotNull(scrollManager.getScroll("scrollTest"), "Test scroll should now exist in manager");
    }

    @Test 
    public void testModifyScrollsId() throws Exception {
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        seedScroll("test001", "Testing101", "johnDoe", "Testing once again...", ymd(2025,10,20));

        simulateInput(
            "3",          // "Modify scroll"
            "1",          // choose scroll #1
            "ChangeId",           // change Id
            "",// keep name
            "",           // keep same file
            "0"           // exit
        );
        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(out.contains("Scroll ID updated successfully.") ||
                out.contains("Scroll successfully modified"), 
                "Should confirm scroll modification");

        Scroll modified = scrollManager.getScroll("ChangeId");
        assertNotNull(modified, "Scroll should still exist");
        assertEquals("ChangeId", modified.getScrollId(), "Scroll name should be updated");
    }

    @Test 
    public void testModifyScrollsName() throws Exception {
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        seedScroll("test001", "Testing101", "johnDoe", "Testing once again...", ymd(2025,10,20));

        simulateInput(
            "3",          // "Modify scroll"
            "1",          // choose scroll #1
            "",           // keep same ID
            "ChangedName",// new scroll name
            "",           // keep same file
            "0"           // exit
        );
        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(out.contains("Scroll Name updated successfully.") ||
                out.contains("Scroll successfully modified"), 
                "Should confirm scroll modification");

        Scroll modified = scrollManager.getScroll("test001");
        assertNotNull(modified, "Scroll should still exist");
        assertEquals("ChangedName", modified.getScrollName(), "Scroll name should be updated");
    }

    @Test 
    public void testModifyEmptyScrolls() throws Exception {
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        simulateInput(
            "3",          // "Modify scroll"
            "0"           // exit
        );
        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(out.contains("You have no scrolls to modify."), "Should display you have no scrolls to modify");
    }

    @Test 
    public void testInvalidChoiceModifyScrolls() throws Exception {
        // Arrange
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        // Seed a single scroll for this user
        seedScroll("testId", "testName", "johnDoe", "Sample content", ymd(2025, 10, 25));

        // Simulate user navigating to "Modify Scrolls"
        simulateInput(
            "2",    // Invalid choice (only 1 scroll exists)
            "abc",  // Invalid non-numeric input
            "0"     // Cancel / Exit
        );

        outputContent.reset();
        userInterface.modifyScrolls();

        // Act
        String out = outputContent.toString().replaceAll("\\s+", " ");

        // Assert
        assertTrue(out.contains("Invalid choice. Try again."),
            "Should display message when user selects an invalid scroll number.");
        assertTrue(out.contains("Please enter a valid number."),
            "Should prompt user to enter a valid numeric choice when non-numeric input is entered.");
        assertFalse(out.contains("Scroll modification complete."),
            "Should not reach modification completion when user exits early.");
    }

    @Test 
    public void testModifyScrollsPath() throws Exception {
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        Scroll original = seedScroll("test001", "Testing101", "johnDoe", "Old content", ymd(2025,10,20));
        Path originalBlob = original.getFileObj().toPath().resolve("scroll_blob");
        assertTrue(Files.exists(originalBlob), "Original blob file should exist");

        Path newFile = Files.createTempFile("newScrollContent", ".txt");
        Files.writeString(newFile, "This is the NEW content");

        simulateInput(
            "3",                // Modify Scrolls
            "1",                // choose scroll #1
            "",                 // keep same ID
            "",                 // keep same name
            newFile.toString(), // new file path
            "0"                 // exit scrollManagement
        );

        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(out.contains("File replaced successfully.") ||
                out.contains("Scroll modification complete"),
                "Should confirm file replacement");

        String blobContent = Files.readString(originalBlob);
        assertEquals("This is the NEW content", blobContent.trim(),
                    "Blob content should be replaced with new file content");

        Files.deleteIfExists(newFile);
    }

    @Test
    public void testDeleteScrolls() throws Exception{
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        seedScroll("test001", "Testing101", "johnDoe", "Delete content", ymd(2025,10,20));

        simulateInput(
            "4",                // delete Scrolls
            "1",                // choose scroll #1
            "y",                 // confirm deletion
            "0"                 // exit scrollManagement
        );

        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(out.contains("Scroll successfully deleted"), "Should confirm successful deletion");

        // check if scroll is deleted
        Scroll deleted = scrollManager.getScroll("test001");
        assertNull(deleted, "Scroll should be removed after deletion");
    }

    @Test
    public void testDontDeleteScrolls() throws Exception{
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        seedScroll("test002", "Testing102", "johnDoe", "Delete content", ymd(2025,10,20));

        simulateInput(
            "4",                // delete Scrolls
            "1",                // choose scroll #1
            "n",                 // deletion cancelled
            "0"                 // exit scrollManagement
        );

        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(out.contains("Deletion cancelled."), "Should confirm deletion cancelled");

        // check if scroll is deleted
        Scroll notDeleted = scrollManager.getScroll("test002");
        assertNotNull(notDeleted, "Scroll should still be in scrolls.");
    }

    @Test
    public void testErrorHandlingDeleteScroll() throws Exception{
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        seedScroll("test002", "Testing102", "johnDoe", "Delete content", ymd(2025,10,20));

        simulateInput(
            "4",                // delete Scrolls
            "1",                // choose scroll #1
            "FAIL",                 // input invalid confirmation
            "n",                 // cancel deletion
            "0"                 // exit scroll manager
        );

        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(out.contains("Please enter 'y' for yes or 'n' for no."), "Show suggested user input.");
        assertTrue(out.contains("Deletion cancelled."), "Should confirm deletion cancelled");

        // check if scroll is deleted
        Scroll notDeleted = scrollManager.getScroll("test002");
        assertNotNull(notDeleted, "Scroll should still be in scrolls.");
    }

    @Test
    public void testErrorHandlingDeleteScrollMenu() throws Exception {
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        seedScroll("test002", "Testing102", "johnDoe", "Delete content", ymd(2025,10,20));

        simulateInput(
            "4",     // select delete scrolls
            "100",   // invalid index (too high)
            "abc",   // invalid format
            "0",      // 
            "0"
        );

        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        // Assertions for invalid entry handling
        assertTrue(out.contains("Invalid choice. Try again."), "Should prompt user for invalid scroll index");
        assertTrue(out.contains("Please enter a valid number."), "Should handle non-numeric input gracefully");

        // Check scroll is still present
        Scroll notDeleted = scrollManager.getScroll("test002");
        assertNotNull(notDeleted, "Scroll should still exist after invalid operations");
    }

    @Test
    public void testDeleteEmptyScrolls() throws Exception{
        User u = userManager.createUser("johnDoe");
        u.setName("John");
        userInterface.setUserLoggedIn(u);

        outputContent.reset();
        userInterface.deleteScrolls();

        String out = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(out.contains("You have no scrolls to delete."), "Should show no scrolls that can be deleted");
    }

    @Test
    public void testAdminMenuOptions() {
        User adminUser = new User("admin1") {
            @Override public boolean getAdmin() { return true; }
            @Override public String getUsername() { return "admin1"; }
            @Override public String getName() { return "Admin User"; }
        };
        userInterface.setUserLoggedIn(adminUser);

        String simulatedInput = String.join(System.lineSeparator(),
            "2", // Admin Menu
            "0", // Exit AdminMenu immediately
            "4", // Logout
            "0"  // Exit program
        );

        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        userInterface.setScanner(new Scanner(System.in));
        outputContent.reset();

        userInterface.start();

        String output = outputContent.toString().replaceAll("\\s+", " ");

        assertTrue(output.contains("Admin Menu") || output.contains("=== Profile ==="),
            "Choice 2 should enter AdminMenu or profile update");
        assertTrue(output.contains("Logging out"), "Choice 4 should log out admin");
    }

    @Test
    public void testGuest_ViewAndPreview() {
        seedScroll("s001", "Intro to Cats", "u-minerva", "Cats were domesticated...", ymd(2025,10,20));
        seedScroll("s002", "Ancient Runes 101", "u-albus", "The elder paw runes...", ymd(2025,10,21));

        userInterface.setToGuest();

        simulateInput(
            "P s002",
            "B"
        );
        outputContent.reset();
        userInterface.guestInterface("1"); 

        String out = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out.contains("ID") && out.contains("NAME"), "Should print table header");
        assertTrue(out.contains("s002") && out.contains("Ancient Runes 101"), "Should show seeded scroll");

        assertTrue(out.contains("Preview") && out.contains("Name: Ancient Runes 101"), "Should preview title");
        assertTrue(out.contains("Scroll ID: s002"), "Should preview scroll id");
        assertTrue(out.contains("Uploader ID: u-albus"), "Should preview uploader");
        assertTrue(out.contains("Date:"), "Should show date line");
        assertTrue(out.contains("The elder paw runes"), "Should print textual preview");
        assertTrue(!out.contains("Download"), "Guest view should not show Download option");
    }

    @Test
    public void testGuest_PreviewTruncatedIndicator() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 600; i++) big.append('x');
        seedScroll("sLong", "Long Text", "u-luna", big.toString(), ymd(2025,10,22));

        userInterface.setToGuest();

        simulateInput(
            "P sLong",
            "B"
        );
        outputContent.reset();
        userInterface.guestInterface("1");

        String out = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out.contains("Name: Long Text"), "Shows preview meta");
        assertTrue(out.contains("(preview truncated)"), "Shows truncation marker for >500 chars");
    }

    @Test
    public void testUser_ViewAndDownload() throws Exception {
        String body = "Hello, whisker world!";
        seedScroll("s003", "Whisker Wisdom", "u-luna", body, ymd(2025,10,22));

        User u = userManager.createUser("harry");
        u.setName("Harry");
        userInterface.setUserLoggedIn(u);

        Path dest = Paths.get("test_download.txt");
        simulateInput(
            "1",
            "D s003",
            dest.toString(),
            "B",
            "0"
        );

        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out.contains("Downloaded to: " + dest), "Download success message should appear");
        assertTrue(Files.exists(dest), "Downloaded file should exist");
        assertEquals(body, Files.readString(dest), "Downloaded content should match blob");
    }

    @Test
    public void testUser_SearchFilters_AndBlankMeansAny() {
        seedScroll("s001", "Intro to Cats",      "u-minerva", "Cats...", ymd(2025,10,20));
        seedScroll("s002", "Ancient Runes 101",  "u-albus",   "Runes...", ymd(2025,10,21));
        seedScroll("s003", "Whisker Wisdom",     "u-luna",    "Wisdom...", ymd(2025,10,22));

        User u = createOrGetUser("ron", "ron", "pass123");
        u.setName("Ron");
        userInterface.setUserLoggedIn(u);

        simulateInput(
            "5",
            "u-albus",
            "",
            "runes",
            "2025-10-01",
            "",
            "0"
        );

        outputContent.reset();
        userInterface.scrollManagement();

        String out = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out.contains("s002") && out.contains("Ancient Runes 101"));
        assertTrue(!out.contains("s001"));
        assertTrue(!out.contains("s003"));

        simulateInput(
            "5",
            "",
            "",
            "", 
            "", 
            "", 
            "0"
        );
        outputContent.reset();
        userInterface.scrollManagement();
        String out2 = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out2.contains("s001"));
        assertTrue(out2.contains("s002"));
        assertTrue(out2.contains("s003"));
    }

    @Test
    public void testPreview_NullCasesAndBadId() {
        userInterface.setToGuest();
        simulateInput("P no_such_id", "B");
        outputContent.reset();
        userInterface.guestInterface("1");
        String out = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out.contains("No preview available") || out.contains("scroll not found"),
                "Should show a friendly message for missing scroll");

        Scroll s = seedScroll("sEmpty", "Empty Blob", "u-x", "", ymd(2025,10,10));
        try {
            Files.deleteIfExists(s.getFileObj().toPath().resolve("scroll_blob"));
        } catch (Exception ignored) {}

        simulateInput("P sEmpty", "B");
        outputContent.reset();
        userInterface.guestInterface("1");
        String out2 = outputContent.toString().replaceAll("\\s+", " ");
        assertTrue(out2.contains("No preview available") || out2.contains("scroll not found"),
                "Should handle missing blob gracefully");
    }

}
