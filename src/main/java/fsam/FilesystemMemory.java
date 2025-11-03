package fsam;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.FileAlreadyExistsException;

public abstract class FilesystemMemory {
    /*
     * FSAM (filesystem as memory) is a persistent storage model where (between access) data exists only on disk
     * It is slower than standard object management, but guarantees atomicity (within the limits of the filesystem) and
     * removes internal state
     */

    // The filesystem object (dir/file) for this FSAM instance
    private File fileObj;
    private File parentObj = null;

    public FilesystemMemory()
    {
        this.fileObj = null;
    }

    public FilesystemMemory(String objPath)
    {
        this.setFileObj(objPath);
    }


    /*
     * Set the file object from which this FSAM system should run
     * Does not validate the object
     */
    public void setFileObj(String objPath)
    {
        fileObj = new File(objPath);
    }

    public void setFileObj(File obj)
    {
        fileObj = obj;
    }

    public void setParent(File obj)
    {
        parentObj = obj;
    }

    public File getFileObj()
    {
        return fileObj;
    }


    // ---[ File Methods ]---
    public boolean exists()
    {
        return fileObj.exists();
    }

    public boolean isDir()
    {
        return fileObj.isDirectory();
    }

    public boolean isFile()
    {
        return fileObj.isFile();
    }

    public boolean childExists(String childPath)
    {
        File childF = new File(fileObj, childPath);
        return childF.exists();
    }

    // Must be implemented per-type (resolve to a type covariant with FilesystemMemory)
    // Should set the child's parent to self
    public abstract FilesystemMemory getChild(String childPath);

    public void createSelfDir() throws IOException, NotDirectoryException
    {
        if (! fileObj.exists())
        {
            if (! fileObj.mkdirs())
            {
                throw new IOException(String.format("Failed to create self directory %s", fileObj.getPath()));
            }
        }
        else if (! fileObj.isDirectory())
        {
            throw new NotDirectoryException(fileObj.getPath());
        }
    }

    public void createSelfFile() throws IOException, FileAlreadyExistsException
    {
        if (! fileObj.exists())
        {
            try
            {
                fileObj.createNewFile();
            }
            catch (IOException e)
            {
                throw e;
            }
        }
        else
        {
            throw new FileAlreadyExistsException(fileObj.getPath());
        }
    }

    public void createChildDir(String childPath) throws IOException, NotDirectoryException
    {
        File childF = new File(fileObj, childPath);

        if (! childF.exists())
        {
            if (! childF.mkdirs())
            {
                throw new IOException(String.format("Failed to create child directory %s", childPath));
            }
        }
        // File already exists and is not a directory
        else if (! childF.isDirectory())
        {
            throw new NotDirectoryException(childF.getPath());
        }
    }

    public void createChildFile(String childPath) throws IOException, FileAlreadyExistsException
    {
        File childF = new File(fileObj, childPath);

        if (! childF.exists())
        {
            try
            {
                childF.createNewFile();
            }
            catch (IOException e)
            {
                throw e;
            }
        }
        // File already exists
        else
        {
            throw new FileAlreadyExistsException(childF.getPath());
        }
    }

    // Move the object this refers to
    public void moveSelf(File relativeTo, String newPath) throws FileAlreadyExistsException, IOException
    {
        File target = new File(relativeTo, newPath);
        if (target.exists())
        {
            throw new FileAlreadyExistsException(newPath);
        }

        if (! fileObj.renameTo(target))
        {
            throw new IOException("Failed to move object");
        }
        fileObj = target;
    }

    public void moveSelf(FilesystemMemory relativeTo, String newPath) throws FileAlreadyExistsException, IOException
    {
        moveSelf(relativeTo.getFileObj(), newPath);
    }

    public void moveSelf(String newPath) throws FileAlreadyExistsException, IOException
    {
        if (parentObj == null)
        {
            // TODO : better exception
            throw new IOException("Cannot move without a relative point unless object has a parent");
        }
        moveSelf(parentObj, newPath);
    }

    // Move a child object relative to this object
    // May invalidate existing children
    public void moveChild(String oldChild, String newChild) throws FileAlreadyExistsException, IOException
    {
        File current = new File(fileObj, oldChild);
        File target = new File(fileObj, newChild);

        if (target.exists())
        {
            throw new FileAlreadyExistsException(newChild);
        }

        if (! current.renameTo(target))
        {
            throw new IOException("Failed to move object");
        }
    }

    // ---[ Data I/O ]---
    private static String readSerialisedValue(File target) throws IOException
    {
        if (! target.exists())
        {
            throw new IOException("Read target does not exist");
        }
        if (! target.isFile())
        {
            throw new IOException("Read target is not a file");
        }

        BufferedReader r = new BufferedReader(new FileReader(target));
        StringBuilder res = new StringBuilder(64);
        String line = r.readLine();
        while (line != null)
        {
            res.append(line);
            line = r.readLine();
        }
        r.close();

        return res.toString();
    }

    private static void writeSerialisedValue(File target, String data) throws IOException, FileAlreadyExistsException
    {
        // Try to create the file if it doesn't exist
        if (! target.isFile())
        {
            if (! target.exists())
            {
                try
                {
                    target.createNewFile();
                }
                catch (IOException e)
                {
                    throw e;
                }
            }
            else
            {
                // The target is a directory
                throw new FileAlreadyExistsException(target.getPath());
            }
        }

        // Write to the file (overwrite)
        BufferedWriter w = new BufferedWriter(new FileWriter(target));
        w.write(data);
        w.close();
    }

    // =[ Self ]=
    public String getSelfString() throws IOException
    {
        return readSerialisedValue(fileObj);
    }

    public void setSelfString(String v) throws IOException
    {
        writeSerialisedValue(fileObj, v);
    }

    public boolean getSelfBoolean() throws IOException
    {
        String boolS = readSerialisedValue(fileObj);
        return boolS.equals("true");
    }

    public void setSelfBoolean(boolean v) throws IOException
    {
        if (v)
        {
            writeSerialisedValue(fileObj, "true");
        }
        else
        {
            writeSerialisedValue(fileObj, "false");
        }
    }

    public Integer getSelfInt() throws IOException, NumberFormatException
    {
        String intS = readSerialisedValue(fileObj);
        return Integer.parseInt(intS);
    }

    public void setSelfInt(Integer v) throws IOException
    {
        String intS = Integer.toString(v);
        writeSerialisedValue(fileObj, intS);
    }

    public Float getSelfFloat() throws IOException, NumberFormatException
    {
        String floatS = readSerialisedValue(fileObj);
        return Float.parseFloat(floatS);
    }

    public void setSelfFloat(Float v) throws IOException
    {
        String floatS = v.toString();
        writeSerialisedValue(fileObj, floatS);
    }

    // =[ Child ]=
    public String getChildString(String childPath) throws IOException
    {
        File childF = new File(fileObj, childPath);
        return readSerialisedValue(childF);
    }

    public void setChildString(String childPath, String v) throws IOException
    {
        File childF = new File(fileObj, childPath);
        writeSerialisedValue(childF, v);
    }

    public boolean getChildBoolean(String childPath) throws IOException
    {
        File childF = new File(fileObj, childPath);
        String boolS = readSerialisedValue(childF);
        return boolS.equals("true");
    }

    public void setChildBoolean(String childPath, boolean v) throws IOException
    {
        File childF = new File(fileObj, childPath);
        if (v)
        {
            writeSerialisedValue(childF, "true");
        }
        else
        {
            writeSerialisedValue(childF, "false");
        }
    }

    public Integer getChildInt(String childPath) throws IOException, NumberFormatException
    {
        File childF = new File(fileObj, childPath);
        String intS = readSerialisedValue(childF);
        return Integer.parseInt(intS);
    }

    public void setChildInt(String childPath, Integer v) throws IOException
    {
        File childF = new File(fileObj, childPath);
        String intS = Integer.toString(v);
        writeSerialisedValue(childF, intS);
    }

    public Float getChildFloat(String childPath) throws IOException, NumberFormatException
    {
        File childF = new File(fileObj, childPath);
        String floatS = readSerialisedValue(childF);
        return Float.parseFloat(floatS);
    }

    public void setChildFloat(String childPath, Float v) throws IOException
    {
        File childF = new File(fileObj, childPath);
        String floatS = v.toString();
        writeSerialisedValue(childF, floatS);
    }
}

/*
    NOTES
        - Will be implemented into classes that wrap current methods;
            - users : root directory of <id>/<attrs>
                - user : directory <id> containing <attrs>
            - scrolls : root directory of <id>/<attrs>
                - scroll : directory <id> containing <attrs>
*/
