import fsam.FilesystemMemory;

import java.util.Date;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;


public class ScrollManager extends FilesystemMemory {

    public ScrollManager(String scrollRoot)
    {
        super(scrollRoot);

        // added this because it wasn't making a root folder for scrolls
        File root = getFileObj();
        if (!root.exists()) {
            root.mkdirs();
        }
    }

    // Attempt to get a reference to an existing scroll
    // Returns null if there isn't a scroll with the given ID
    public Scroll getScroll(String scrollId)
    {
        if (!childExists(scrollId))
        {
            return null;
        }
        return getChild(scrollId);
    }

    // Attempt to create a new scroll and get a reference to that scroll
    // Returns null if a filesystem error occurred
    public Scroll createScroll(String scrollId) throws ScrollAlreadyExistsException
    {
        if (childExists(scrollId))
        {
            throw new ScrollAlreadyExistsException(String.format("Scroll %s already exists", scrollId));
        }

        Scroll newScroll = getChild(scrollId);

        // If the scroll doesn't exist, try to create it with default values
        try
        {
            newScroll.createSelfDir();
            newScroll.setScrollName("");
            newScroll.setUploaderId("");
            newScroll.setUploadDate(new Date());
            // Create empty blob file
            newScroll.createChildFile("scroll_blob");
        }
        catch (FileAlreadyExistsException e) {}
        catch (IOException e)
        {
            return null;
        }

        return newScroll;
    }

    public boolean changeScrollId(String oldId, String newId) throws ScrollAlreadyExistsException, ScrollDoesNotExistException
    {
        if (! childExists(oldId))
        {
            throw new ScrollDoesNotExistException(String.format("Scroll %s does not exist, cannot be moved", oldId));
        }
        if (childExists(newId))
        {
            throw new ScrollAlreadyExistsException(String.format("Scroll ID %s already in use, cannot move to it", newId));
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

    public boolean deleteScroll(String scrollId)
    {
        Scroll s = getChild(scrollId);

        if (! s.exists())
        {
            return false;
        }

        File scrollF = s.getFileObj();

        File[] scrollData = scrollF.listFiles();

        if (scrollData != null)
        {
            for (File dataF : scrollData)
            {
                dataF.delete();
            }
        }

        scrollF.delete();

        return true;
    }

    public List<Scroll> getAllScrolls()
    {
        ArrayList<Scroll> scrolls = new ArrayList<>();
        for (File f : getFileObj().listFiles())
        {
            scrolls.add(getChild(f.getName()));
        }

        return scrolls;
    }

    public Scroll findScrollByScrollName(String scrollName)
    {
        if (scrollName == null) {
            return null;
        }

        for (Scroll u : getAllScrolls())
        {
            if (u == null) {
                continue;
            }
            String n = u.getScrollName();
            if (n != null && n.equals(scrollName))
            {
                return u;
            }
        }

        return null;
    }

    public List<Scroll> getAllScrollsSorted()
    {
        ArrayList<Scroll> scrolls = new ArrayList<>(getAllScrolls());
        Collections.sort(scrolls, new Comparator<Scroll>() {
            public int compare(Scroll a, Scroll b)
            {
                Date da = a.getUploadDate();
                Date db = b.getUploadDate();

                long la;
                if (da == null) {
                    la = 0L;
                } else {
                    la = da.getTime();
                }

                long lb;
                if (db == null) {
                    lb = 0L;
                } else {
                    lb = db.getTime();
                }

                if (la == lb) {
                    String ida = a.getScrollId();
                    String idb = b.getScrollId();

                    if (ida == null && idb == null) {
                        return 0;
                    }
                    if (ida == null) {
                        return 1;
                    }
                    if (idb == null) {
                        return -1;
                    }
                    return ida.compareTo(idb);
                }

                if (lb < la) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        return scrolls;
    }

    public boolean downloadScroll(String scrollId, String destPath)
    {
        Scroll s = getScroll(scrollId);
        if (s == null)
        {
            return false;
        }

        File f = s.getScrollFile();
        if (f == null || !f.exists())
        {
            return false;
        }
        try
        {
            java.nio.file.Files.copy(
                f.toPath(),
                java.nio.file.Path.of(destPath),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
        }
        catch (java.io.IOException e)
        {
            return false;
        }

        return true;
    }

    public List<Scroll> searchScrolls(String uploaderId,
                                  String scrollId,
                                  String nameContains,
                                  Date uploadedAfter,
                                  Date uploadedBefore)
    {
        ArrayList<Scroll> search = new ArrayList<>();

        List<Scroll> all = getAllScrollsSorted();
        for (Scroll s : all)
        {
            if (s == null)
            {
                continue;
            }
            if (uploaderId != null && !uploaderId.isBlank())
            {
                String u = s.getUploaderId();
                if (u == null || !u.equals(uploaderId))
                {
                    continue;
                }
            }

            if (scrollId != null && !scrollId.isBlank())
            {
                String id = s.getScrollId();
                if (id == null || !id.equals(scrollId))
                {
                    continue;
                }
            }

            if (nameContains != null && !nameContains.isBlank())
            {
                String n = s.getScrollName();
                if (n == null)
                {
                    continue;
                }

                String nLower = n.toLowerCase();
                String qLower = nameContains.toLowerCase();

                if (!nLower.contains(qLower))
                {
                    continue;
                }
            }

            Date d = s.getUploadDate();
            if (uploadedAfter != null)
            {
                Date afterNorm = startOfDay(uploadedAfter);
                if (d == null || d.before(afterNorm))
                {
                    continue;
                }
            }
            if (uploadedBefore != null)
            {
                Date beforeNorm = endOfDay(uploadedBefore);
                if (d == null || d.after(beforeNorm))
                {
                    continue;
                }
            }
            search.add(s);
        }
        return search;
    }

    private Date startOfDay(Date d)
    {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(d);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date endOfDay(Date d)
    {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(d);
        c.set(java.util.Calendar.HOUR_OF_DAY, 23);
        c.set(java.util.Calendar.MINUTE, 59);
        c.set(java.util.Calendar.SECOND, 59);
        c.set(java.util.Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    public ScrollTextPreview previewScrollText(String scrollId)
    {
        Scroll s = getScroll(scrollId);
        if (s == null)
        {
            return null;
        }

        File f = s.getScrollFile();
        if (f == null || !f.exists())
        {
            return null;
        }

        ReadTextResult r = readText(f, 500);
        if (r == null)
        {
            return null;
        }

        ScrollTextPreview p = new ScrollTextPreview();
        p.scrollId = s.getScrollId();
        p.scrollName = s.getScrollName();
        p.uploaderId = s.getUploaderId();
        p.uploadDate = s.getUploadDate();
        p.textPreview = r.text;
        p.truncated = r.truncated;

        return p;
    }

    private static class ReadTextResult {
        String text;
        boolean truncated;
    }

    private ReadTextResult readText(File f, int maxChars)
    {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        boolean truncated = false;

        try (java.io.InputStream in = new java.io.FileInputStream(f);
            java.io.InputStreamReader isr = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8);
            java.io.BufferedReader br = new java.io.BufferedReader(isr))
        {
            int remaining = maxChars;
            char[] buf = new char[512];
            int n = br.read(buf, 0, Math.min(buf.length, remaining));

            while (n > 0 && remaining > 0)
            {
                int take = n;
                if (take > remaining)
                {
                    take = remaining;
                }

                sb.append(buf, 0, take);
                remaining -= take;

                if (remaining <= 0)
                {
                    truncated = true;
                    break;
                }

                n = br.read(buf, 0, Math.min(buf.length, remaining));
            }
        }
        catch (java.io.IOException e)
        {
            return null;
        }

        ReadTextResult r = new ReadTextResult();
        r.text = sb.toString();
        r.truncated = truncated;
        return r;
    }

    public static class ScrollTextPreview {
        public String scrollId;
        public String scrollName;
        public String uploaderId;
        public Date uploadDate;
        public String textPreview;
        public boolean truncated;
    }

    // getChild method is not to be used directly, instead use the wrappers
    // "getScroll" and "createScroll"
    public Scroll getChild(String childPath)
    {
        File scrollDir = new File(getFileObj(), childPath);
        Scroll child = new Scroll();
        child.setFileObj(scrollDir);
        child.setParent(getFileObj());

        return child;
    }
}
