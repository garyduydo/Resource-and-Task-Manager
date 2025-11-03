public class App {
    public static void main(String[] args) {
        String userRoot = "vsas_data/users";
        String scrollRoot = "vsas_data/scrolls";

        UserManager userManager = new UserManager(userRoot);
        ScrollManager scrollManager = new ScrollManager(scrollRoot);
        UserInterface userInteface = new UserInterface(userManager, scrollManager);
        userInteface.start();
    }
}
