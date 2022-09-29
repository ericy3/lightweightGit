package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;


public class Repo {
    /** Dictionary to store commit id and commit object. */
    private static TreeMap<String, Commit> dictCom =
            new TreeMap<String, Commit>();
    /** Dictionary to store blob id and file name. */
    private static TreeMap<String, String> dictBlob =
            new TreeMap<String, String>();
    /** Dictionary to store pointer name and commit id. */
    private static TreeMap<String, String> dictPoint =
            new TreeMap<String, String>();
    /** Dictionary to store remote names and remote objects. */
    private static TreeMap<String, Remote> dictRemote =
            new TreeMap<String, Remote>();

    /** Folder that holds all the Repo info. */
    static final File REPO = new File(Main.GITLET.getPath()
            + File.separator + "Repo");
    /** Folder that holds all the commits. */
    static final File COMMITS = new File(Main.GITLET.getPath()
            + File.separator + "COMMITS");
    /** Folder that holds all the blobs. */
    static final File BLOBS = new File(Main.GITLET.getPath()
            + File.separator + "BLOBS");

    /** File that holds the commit dictionary and ensures it persists. */
    static final File COMMIT_DICT = new File(REPO.getPath()
            + File.separator + "COMMIT_DICT.txt");
    /** File that holds the pointer dictionary and ensures it persists. */
    static final File POINT_DICT = new File(REPO.getPath()
            + File.separator + "POINT_DICT.txt");
    /** File that holds the blob dictionary and ensures it persists. */
    static final File BLOB_DICT = new File(REPO.getPath()
            + File.separator + "BLOB_DICT.txt");


    /** Initializes .gitlet folder and all appropriate folders and commit. */
    static void init() {
        if (Main.GITLET.exists()) {
            Utils.message("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return;
        }
        setUpFolders();
        setUpRepo();
        Staging.init();
        Commit initial = new Commit();
        dictCom.put(initial.id(), initial);
        dictPoint.put("HEAD", initial.id());
        dictPoint.put("master", initial.id());
        dictPoint.put("HEAD_BRANCH", "master");
        writeRepo();
    }


    /** Helper method for setting up the folders. */
    public static void setUpFolders() {
        Main.GITLET.mkdir();
        REPO.mkdir();
        COMMITS.mkdir();
        BLOBS.mkdir();
    }

    /** Helper method for setting up the Repo files. */
    public static void setUpRepo() {
        try {
            COMMIT_DICT.createNewFile();
            BLOB_DICT.createNewFile();
            POINT_DICT.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Use after making changes in any of
     * files below and saves changes into file. */
    public static void writeRepo() {
        Utils.writeObject(COMMIT_DICT, dictCom);
        Utils.writeObject(BLOB_DICT, dictBlob);
        Utils.writeObject(POINT_DICT, dictPoint);
    }

    /** Reads the files to make sure data
     * structures properly updated and persisted.
     * @SuppressWarnings since guarantee
     * read objects is appropriate ones. */
    @SuppressWarnings("unchecked")
    public static void readRepo() {
        dictCom = Utils.readObject(COMMIT_DICT, TreeMap.class);
        dictBlob = Utils.readObject(BLOB_DICT, TreeMap.class);
        dictPoint = Utils.readObject(POINT_DICT, TreeMap.class);
    }

    /** @param id commit id
     * Translate a commit id into the commit object.
     * @return a commit with given id */
    public static Commit translateCommit(String id) {
        readRepo();
        return dictCom.get(id);
    }

    /** @param id blob id
     * Translates a blob id into the blob name.
     * @return the file name of given blob id */
    public static String translateBlob(String id) {
        readRepo();
        return dictBlob.get(id);
    }

    /** @param point name of pointer
     * Translates name of pointer into commit id.
     * @return the commit id of given pointer */
    public static String translatePoint(String point) {
        readRepo();
        return dictPoint.get(point);
    }

    /**
     * Finds the remote object based on given name.
     * @param name of the remote object
     * @return the actual remote object */
    public static Remote translateRemote(String name) {
        readRepo();
        return dictRemote.get(name);
    }

    public static void newBranch(String name) {
        readRepo();
        if (dictPoint.containsKey(name)) {
            Utils.message("A branch with that name already exists.");
            return;
        }
        changePointer(name, translatePoint("HEAD"));
        writeRepo();
    }

    /** Returns the Commit obj of the current head commit.
     * @return the current head commit */
    public static Commit currentHead() {
        readRepo();
        return translateCommit(translatePoint("HEAD"));
    }

    /** Returns the name of the branch HEAD is on.
     * @return String of current branch HEAD on */
    public static String currentHeadBranch() {
        readRepo();
        return translatePoint("HEAD_BRANCH");
    }

    /** @param newBranch name of new branch to point at
     * Changes the HEAD_BRANCH to point at given branch name.
     * and changes the */
    public static void changeBranch(String newBranch) {
        readRepo();
        changePointer("HEAD_BRANCH", newBranch);
        writeRepo();
    }

    /**@param newBranch name of new branch to point at
     * Changes the HEAD to point at the right
     * commit id.*/
    public static void changeHead(String newBranch) {
        readRepo();
        String id = translatePoint(newBranch);
        changePointer("HEAD", id);
        writeRepo();
    }

    /** @param branchName name of the branch
     * Removes the branch pointer with given name */
    public static void removeBranch(String branchName) {
        readRepo();
        if (!getDictPoint().containsKey(branchName)) {
            Utils.message("A branch with that name does not exist.");
            return;
        }
        if (currentHeadBranch().equals(branchName)) {
            Utils.message("Cannot remove the current branch.");
            return;
        }
        getDictPoint().remove(branchName);
        writeRepo();
    }

    /** @param id hash of the commit
     *  @param item commit object associated with id
     * Puts commit into commit dictionary with proper persistence */
    public static void putCommit(String id, Commit item) {
        readRepo();
        dictCom.put(id, item);
        writeRepo();
    }

    /** @param id hash of the blob
     *  @param name file name associated with id
     * Puts blob into blob dictionary with proper persistence protocol. */
    public static void putBlob(String id, String name) {
        readRepo();
        dictBlob.put(id, name);
        writeRepo();
    }

    /** @param id blob id to remove
     * Removes blob from blob dictionary with proper persistence protocol. */
    public static void removeBlob(String id) {
        readRepo();
        dictBlob.remove(id);
        writeRepo();
    }

    /** Puts the remote object into the collection.
     * @param name of the remote
     * @param remote object
     */
    public static void putRemote(String name, Remote remote) {
        readRepo();
        dictRemote.put(name, remote);
        writeRepo();
    }

    /** @param pointer pointer name to change
     *  @param id commit id to point at
     * Changes or adds pointers to the correct
     * commit w/ proper persistence protocol. */
    public static void changePointer(String pointer, String id) {
        readRepo();
        dictPoint.put(pointer, id);
        writeRepo();
    }

    /** Accessor method for commit dictionary.
     * @return the dictionary of commits */
    public static TreeMap<String, Commit> getDictCom() {
        readRepo();
        return dictCom;
    }

    /** Accessor method for blob dictionary.
     * @return the dictionary of blobs */
    public static TreeMap<String, String> getDictBlob() {
        readRepo();
        return dictBlob;
    }

    /** Accessor method for point dictionary.
     * @return the dictionary of pointers */
    public static TreeMap<String, String> getDictPoint() {
        readRepo();
        return dictPoint;
    }

    /** Accessor method for remote dictionary.
     * @return the dictionary of remotes */
    public static TreeMap<String, Remote> getRemote() {
        readRepo();
        return dictRemote;
    }

    /** Removes the remote from the dictionary
     * and deletes the .gitlet folder.
     * @param name of remote to remove */
    public static void removeRemote(String name) {
        readRepo();
        dictRemote.get(name);
        dictRemote.remove(name);
        writeRepo();
    }

    /** @param id commit id to check
     *  Checks a commit ID to see which one
     *  it matches with in the repository.
     *  @return the full commit id that matches */
    public static String equivCommit(String id) {
        readRepo();
        Iterator<String> commitIDs = getDictCom()
                .keySet().iterator();
        while (commitIDs.hasNext()) {
            String commitID = commitIDs.next();
            if (commitID.contains(id)) {
                return commitID;
            }
        }
        return null;
    }

}
