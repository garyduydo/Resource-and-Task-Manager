import fsam.FilesystemMemory;
import security.PasswordHasher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import java.util.List;
import java.util.ArrayList;

public class UserManager extends FilesystemMemory {
    public UserManager(String userRoot)
    {
        super(userRoot);
    }

    // Attempt to get a reference to an existing user
    // Returns null if there isn't a user with the given ID
    public User getUser(String userId)
    {
        if (! childExists(userId))
        {
            return null;
        }

        return getChild(userId);
    }

    // Attempt to create a new user and get a reference to that user
    // Returns null if a filesystem error occurred
    public User createUser(String userId) throws UserAlreadyExistsException
    {
        if (childExists(userId))
        {
            throw new UserAlreadyExistsException(String.format("User %s already exists", userId));
        }

        User newUser = getChild(userId);

        // If the user doesn't exist, try to create them with default values
        try
        {
            newUser.createSelfDir();
            newUser.setUsername("");
            newUser.setName("");
            newUser.setPhone("");
            newUser.setEmail("");
            newUser.setPasswordHash("");
            newUser.setAdmin(false);
        }
        catch (FileAlreadyExistsException e) {}
        catch (IOException e)
        {
            return null;
        }

        return newUser;
    }

    public boolean changeUserId(String oldId, String newId) throws UserAlreadyExistsException, UserDoesNotExistException
    {
        if (! childExists(oldId))
        {
            throw new UserDoesNotExistException(String.format("User %s does not exist, cannot be moved", oldId));
        }
        if (childExists(newId))
        {
            throw new UserAlreadyExistsException(String.format("User ID %s already in use, cannot move to it", newId));
        }

        try
        {
            moveChild(oldId, newId);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public List<User> getAllUsers()
    {
        ArrayList<User> users = new ArrayList<>();
        for (File f : getFileObj().listFiles())
        {
            users.add(getChild(f.getName()));
        }

        return users;
    }

    public User findUserByUsername(String username)
    {
        for (User u : getAllUsers())
        {
            if (u != null && u.getUsername().equals(username))
            {
                return u;
            }
        }

        return null;
    }

    // getChild method is not to be used directly, instead use the wrappers
    // "getUser" and "createUser"
    public User getChild(String childPath)
    {
        File userDir = new File(getFileObj(), childPath);
        User child = new User();
        child.setFileObj(userDir);
        child.setParent(getFileObj());

        return child;
    }

    public boolean setPassword(User u, String plaintext) {
        try {
            byte[] salt = PasswordHasher.generateSalt();
            byte[] derived = PasswordHasher.deriveKey(
                    plaintext.toCharArray(),
                    salt,
                    PasswordHasher.DEFAULT_ITERATIONS,
                    PasswordHasher.DEFAULT_KEY_LENGTH_BITS
            );

            boolean ok = true;
            ok &= u.setPasswordHash(PasswordHasher.toBase64(derived));
            ok &= u.setPasswordSalt(PasswordHasher.toBase64(salt));
            ok &= u.setPasswordAlgo(PasswordHasher.DEFAULT_ALGO);
            ok &= u.setPasswordIters(PasswordHasher.DEFAULT_ITERATIONS);
            return ok;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean checkPassword(User u, String candidate) {
        try {
            String algo = u.getPasswordAlgo();
            String saltB64 = u.getPasswordSalt();
            Integer iters = u.getPasswordIters();
            String storedHashB64 = u.getPasswordHash();

            if (algo == null || saltB64 == null || iters == null || storedHashB64 == null || storedHashB64.isEmpty()) {
                return false;
            }

            byte[] salt = PasswordHasher.fromBase64(saltB64);
            byte[] expected = PasswordHasher.fromBase64(storedHashB64);
            byte[] actual = PasswordHasher.deriveKey(
                    candidate.toCharArray(),
                    salt,
                    iters,
                    expected.length * 8 
            );

            return PasswordHasher.constantTimeEquals(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
