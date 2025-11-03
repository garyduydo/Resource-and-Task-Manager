import fsam.FilesystemMemory;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.Date;

class Scroll extends FilesystemMemory{
    // Internal FilesystemMemory object for the underlying data
    private class ScrollData extends FilesystemMemory {
        private ScrollData()
        {
            super();
        }

        public ScrollData getChild(String childPath)
        {
            return null;
        }
    }

    public Scroll()
    {
        super();
    }

    public Scroll(String scrollId)
    {
        super(scrollId);
    }


    // As with users, ID should be set by the manager
    public String getScrollId()
    {
        return getFileObj().getName();
    }


    public boolean setScrollName(String newName)
    {
        try
        {
            setChildString("scroll_name", newName);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public String getScrollName()
    {
        try
        {
            return getChildString("scroll_name");
        }
        catch (IOException e)
        {
            return null;
        }
    }


    public boolean setUploaderId(String id)
    {
        try
        {
            setChildString("uploader_id", id);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public String getUploaderId()
    {
        try
        {
            return getChildString("uploader_id");
        }
        catch (IOException e)
        {
            return null;
        }
    }


    // NOTE : Date is tracked with file timestamp, NOT with file contents
    public boolean setUploadDate(Date d)
    {
        long epoch_stamp = d.getTime();

        if (epoch_stamp < 0)
        {
            return false;
        }

        if (! childExists("upload_timestamp"))
        {
            try
            {
                createChildFile("upload_timestamp");
            }
            catch (IOException e)
            {
                return false;
            }
        }

        getChild("upload_timestamp").getFileObj().setLastModified(epoch_stamp);
        return true;
    }

    public Date getUploadDate()
    {
        long epoch_stamp = getChild("upload_timestamp").getFileObj().lastModified();

        if (epoch_stamp == 0)
        {
            return null;
        }

        return new Date(epoch_stamp);
    }

    public boolean setScrollFile(String path)
    {
        File source = new File(path);
        File target = getChild("scroll_blob").getFileObj();

        if (! source.isFile())
        {
            return false;
        }

        try
        {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public File getScrollFile()
    {
        File c = getChild("scroll_blob").getFileObj();

        if (! c.exists())
        {
            return null;
        }

        return c;
    }


    public ScrollData getChild(String dataPath)
    {
        File dataFile = new File(getFileObj(), dataPath);
        ScrollData data = new ScrollData();
        data.setFileObj(dataFile);
        data.setParent(getFileObj());

        return data;
    }
}
