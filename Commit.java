package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.TreeMap;

public class Commit implements Serializable {

    /** Message associated with given commit. */
    private String message;
    /** ID of the parent of the given commit. */
    private String parent;
    /** ID of second parent in cases of merge. */
    private String secondParent;
    /** File version of commit for persistence. */
    private File commitFile;
    /** TreeMap to store all the tracked files.
     * No .txt for file names as keys and blob
     * ids as the values. */
    private TreeMap<String, String> tracked = new TreeMap<String, String>();
    /** Date object to get the timestamp of commit. */
    private Date date;
    /** Time of commit in desired format as String. */
    private String time;

    /** ID to ensure that serialization goes off with no issue. */
    private static final long serialVersionUID = -520652377646621239L;

    /** @param mess associated message of commit
     *  @param par String of id of parent
     * Initializes a created commit. */
    public Commit(String mess, String par) {
        this.parent = par;
        this.secondParent = null;
        this.message = mess;
        this.date = new Date();
        String formattedTime = String.format("%1$ta %1$tb %1$te "
                + "%1$tT %1$tY %1$tz", date);
        this.time = formattedTime;
        this.commitFile = new File(Repo.COMMITS.getPath()
                + File.separator + id() + ".txt");
        writeCommit();
    }

    /** Initializes the initial commit
     * after init() is called. */
    public Commit() {
        this.message = "initial commit";
        this.parent = null;
        this.date = new Date(0);
        String formattedTime = String.format("%1$ta %1$tb %1$te "
                + "%1$tT %1$tY %1$tz", date);
        this.time = formattedTime;
        this.commitFile = new File(Repo.COMMITS.getPath()
                + File.separator + id() + ".txt");
        writeCommit();
    }

    /** Returns the SHA1 id of the given commit.
     * @return String id of commit */
    public String id() {
        if (parent == null) {
            return Utils.sha1(message + time);
        } else if (secondParent != null) {
            return Utils.sha1((message  + time
                    + parent + secondParent));
        }
        return Utils.sha1(message + time + parent);
    }

    /** @param secParent commit id of second parent
     *  @param given branch name of given branch
     *  @param current branch name of current branch
     * Used for updating a second parent to
     * the given commit, before saving it.
     */
    public void updateSecond(String secParent,
                             String current, String given) {
        this.message =  "Merged " + given
                + " into " + current  + ".";
        this.secondParent = secParent;
        writeCommit();
    }

    /** Saves the commit as a file. */
    public void writeCommit() {
        try {
            commitFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(commitFile, this);
    }
    /** Reads and returns the commit from a file format. */
    public Commit readCommit() {
        return Utils.readObject(commitFile, Commit.class);
    }

    /** Gets the tracked blobs HashSet of the given commit.
     * @return the TreeMap of tracked blobs */
    public TreeMap<String, String> getTracked() {
        readCommit();
        return tracked;
    }

    /** @param fileName name of file to look for blob
     * Get tracked blob with given fileName.
     * @return the blob id corresponding to file name
     * in the given commit */
    public String getBlobHash(String fileName) {
        readCommit();
        return tracked.get(fileName);
    }

    /** @param fileName name of file to look for
     * Checks if a given file exists in commit.
     * @return boolean whether a file with given
     * name is tracked in this commit */
    public boolean fileExists(String fileName) {
        readCommit();
        return tracked.containsKey(fileName);
    }

    /** @param fileName name of file
     *  @param blob hash of blob
     * Updates the tracked blobs TreeSet of a commit, should
     * only be used right after initializing a commit. */
    public void updateTracked(String fileName, String blob) {
        readCommit();
        tracked.put(fileName, blob);
        writeCommit();
    }

    /** Returns the timestamp on the given commit. */
    public String getTime() {
        readCommit();
        return time;
    }

    /** Returns the parent commit of the given commit. */
    public Commit getParent() {
        readCommit();
        if (parent == null) {
            return null;
        }
        return Repo.translateCommit(parent);
    }

    /** Returns the parent commit id of the given commit.
     * @return string id of parent commit */
    public String getParentString() {
        readCommit();
        if (parent == null) {
            return null;
        }
        return parent;
    }

    /** Returns the parent commit id of the given commit.
     * @return string id of second parent commit */
    public String getSecParentString() {
        readCommit();
        if (secondParent == null) {
            return null;
        }
        return secondParent;
    }

    /** Returns the second parent commit of the given commit. */
    public Commit getSecondParent() {
        readCommit();
        if (secondParent == null) {
            return null;
        }
        return Repo.translateCommit(secondParent);
    }

    /** Returns the message attached to the given commit. */
    public String getMessage() {
        readCommit();
        return this.message;
    }

    /** Returns the date object attached to given commit. */
    public Date getDate() {
        readCommit();
        return this.date;
    }


}
