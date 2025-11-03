import fsam.FilesystemMemory;

import java.io.File;
import java.io.IOException;;

public class User extends FilesystemMemory {
    // Internal FilesystemMemory object for the underlying data
    private class UserData extends FilesystemMemory {
        private UserData()
        {
            super();
        }

        public UserData getChild(String childPath)
        {
            return null;
        }
    }

    public User()
    {
        super();
    }

    public User(String userId)
    {
        super(userId);
    }

    public String getUserId()
    {
        return getFileObj().getName();
    }

    // NOTE : Changing a user's ID should only be done with the UserManager
    //        (this will invalidate current User references, and so they should be updated or should not persist)

    public boolean setUsername(String newName)
    {
        try
        {
            setChildString("username", newName);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public String getUsername()
    {
        try
        {
            return getChildString("username");
        }
        catch (IOException e)
        {
            return null;
        }
    }


    public boolean setName(String newName)
    {
        try
        {
            setChildString("name", newName);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public String getName()
    {
        try
        {
            return getChildString("name");
        }
        catch (IOException e)
        {
            return null;
        }
    }


    public boolean setPhone(String newPhone)
    {
        try
        {
            setChildString("phone", newPhone);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public String getPhone()
    {
        try
        {
            return getChildString("phone");
        }
        catch (IOException e)
        {
            return null;
        }
    }


    public boolean setEmail(String newEmail)
    {
        try
        {
            setChildString("email", newEmail);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public String getEmail()
    {
        try
        {
            return getChildString("email");
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public boolean setPasswordHash(String newPasswordHash)
    {
        try
        {
            setChildString("password_hash", newPasswordHash);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public String getPasswordHash()
    {
        try
        {
            return getChildString("password_hash");
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public boolean setAdmin(boolean adminStatus)
    {
        try
        {
            setChildBoolean("admin", adminStatus);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    // Defaults to false if an error occurs
    public boolean getAdmin()
    {
        try
        {
            return getChildBoolean("admin");
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public UserData getChild(String dataPath)
    {
        File dataFile = new File(getFileObj(), dataPath);
        UserData data = new UserData();
        data.setFileObj(dataFile);
        data.setParent(getFileObj());

        return data;
    }

    public boolean setPasswordSalt(String saltB64) {
        try {
            setChildString("password_salt", saltB64);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public String getPasswordSalt() {
        try {
            return getChildString("password_salt");
        } catch (IOException e) {
            return null;
        }
    }

    public boolean setPasswordAlgo(String algo) {
        try {
            setChildString("password_algo", algo);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public String getPasswordAlgo() {
        try {
            return getChildString("password_algo");
        } catch (IOException e) {
            return null;
        }
    }

    public boolean setPasswordIters(int iters) {
        try {
            setChildString("password_iters", Integer.toString(iters));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public Integer getPasswordIters() {
        try {
            return Integer.parseInt(getChildString("password_iters"));
        } catch (Exception e) {
            return null;
        }
    }
}
