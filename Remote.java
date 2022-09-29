package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

public class Remote {

    /** Dictionary to store pointer name and commit id. */
    private TreeMap<String, String> dictPoint =
            new TreeMap<String, String>();

    /** File that holds the directory this remote is in. */
    private File directory;

    /** Name of this remote. */
    private String name;

    /** Initializes a remote object.
     * @param remoteName name of the remote
     * @param dir directory of the commit with
     * /.gitlet at the end */
    public Remote(String remoteName, String dir) {
        if (Repo.getRemote().containsKey(remoteName))  {
            Utils.message("A remote with that name already exists.");
            return;
        }
        name = remoteName;
        String dirName = dir.substring(0, dir.length() - 9);
        directory = new File(dirName + File.separator + ".gitlet");
        directory.mkdir();
    }


    /** Returns the directory of this remote. */
    public File getDirectory() {
        return directory;
    }

    /** Returns the name of this remote. */
    public String getName() {
        return name;
    }


    public void update() {
        Repo.putRemote(name, this);
    }


    /** Accessor method for point dictionary.
     * @return the dictionary of pointers */
    public TreeMap<String, String> getDictPoint() {
        return dictPoint;
    }

}
