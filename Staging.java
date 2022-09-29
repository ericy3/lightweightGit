package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.TreeMap;
import java.nio.file.Files;
import java.util.Iterator;


public class Staging {

    /** TreeMap that represents information about
     * staging folder w/ file names and blob names. */
    private static TreeMap<String, String> stage =
            new TreeMap<String, String>();
    /** TreeMap that represents information about
     * removal folder w/ file names and blob names. */
    private static TreeMap<String, String> removal =
            new TreeMap<String, String>();

    /** Holds all the necessary files of the staging area. */
    static final File STAGING_FOLDER = new File(Main.GITLET.getPath()
            + File.separator + "staging");
    /** Serialized version of stage variable for persistence. */
    static final File STAGE_FILE = new File(STAGING_FOLDER.getPath()
            + File.separator + "stage.txt");
    /** Serialized version of removal variable for persistence. */
    static final File REMOVAL_FILE = new File(STAGING_FOLDER.getPath()
            + File.separator + "removal.txt");
    /** Folder to physically hold staged blobs copied over. */
    static final File STAGE_FOLDER = new File(STAGING_FOLDER.getPath()
            + File.separator + "stage_folder");
    /** Folder to physically hold blobs staged for removal. */
    static final File REMOVE_FOLDER = new File(STAGING_FOLDER.getPath()
            + File.separator + "remove_folder");

    /** Initializes the proper folder and files. */
    public static void init() {
        STAGING_FOLDER.mkdir();
        STAGE_FOLDER.mkdir();
        REMOVE_FOLDER.mkdir();
        writeStaging();
    }

    /** @param fileName name of file to add (has .txt)
     * Adds a file in CWD to staging and adds blob ID and
     * file name to Repo dictionary
     * also removing it if it is necessary. */
    public static void add(String fileName) {
        readStaging();
        File temp = new File(Main.CWD.getPath()
                + File.separator + fileName);
        if (!temp.exists()) {
            Utils.message("File does not exist.");
            return;
        }
        Commit head = Repo.currentHead();
        String cwdBlobID = Blob.contentID(fileName);
        if (head.fileExists(fileName)) {
            String headContent = head.getBlobHash(fileName);
            if (headContent.equals(cwdBlobID)) {
                removeStaged(fileName);
                if (removal.containsValue(cwdBlobID)) {
                    removeRemoval(fileName);
                }
                writeStaging();
                return;
            }
        }
        if (removal.containsKey(fileName)) {
            removeRemoval(fileName);
        }
        if (stage.containsKey(fileName)) {
            removeStaged(fileName);
        }
        addStaged(fileName, cwdBlobID);
        Blob.saveFileToBlobs(fileName);
        writeStaging();
    }

    /** Creates a new commit and sets head to it, with given
     * information in staging folder and removal folder.
     * Suppresses iterator type warnings with generics.
     * @param message for new commit
     * @param flag for merge situations
     * @param secondParent id of second parent
     * @param givenBranch name of given branch
     */
    @SuppressWarnings("unchecked")
    public static void commit(String message, int flag,
                              String secondParent, String givenBranch) {
        readStaging();
        if (stage.isEmpty() && removal.isEmpty()) {
            Utils.message("No changes added to the commit.");
            return;
        }
        Commit currentHead = Repo.translateCommit
                (Repo.translatePoint(Repo.currentHeadBranch()));
        Commit next = new Commit(message, currentHead.id());
        TreeMap parent = next.getParent().getTracked();
        Iterator<String> parentBlobs = parent.values().iterator();
        while (parentBlobs.hasNext()) {
            String currParentBlobID = parentBlobs.next();
            String parentBlobName = Repo.translateBlob(currParentBlobID);
            if (!removal.containsKey(parentBlobName)) {
                if (stage.containsKey(parentBlobName)) {
                    next.updateTracked(parentBlobName,
                            stage.get(parentBlobName));
                    Repo.putBlob(stage.get(parentBlobName), parentBlobName);
                } else {
                    next.updateTracked(parentBlobName,
                            currParentBlobID);
                }
                removeStaged(parentBlobName);
            } else {
                removeRemoval(parentBlobName);
            }
        }
        if (!stage.isEmpty()) {
            Iterator<String> leftOverFileNames = stage.keySet().iterator();
            while (leftOverFileNames.hasNext()) {
                String current = leftOverFileNames.next();
                next.updateTracked(current, stage.get(current));
                removeStaged(current);
            }
        }
        if (!removal.isEmpty()) {
            Iterator<String> leftOverFileNames =
                    removal.keySet().iterator();
            while (leftOverFileNames.hasNext()) {
                String current = leftOverFileNames.next();
                removeRemoval(current);
            }
        }
        if (flag == 1 || flag == 2) {
            next.updateSecond(secondParent,
                    Repo.currentHeadBranch(), givenBranch);
            if (flag == 2) {
                Utils.message("Encountered a merge conflict.");
            }
        }
        Repo.putCommit(next.id(), next);
        Repo.changePointer("HEAD", next.id());
        Repo.changePointer(Repo.currentHeadBranch(), next.id());
        writeStaging();
    }

    /** @param fileName of file to remove
     * Carries out proper removal procedure
     * inside the staging area. */
    public static void rm(String fileName) {
        readStaging();
        int stageFlag = 0;
        int trackedFlag = 0;
        if (stage.containsKey(fileName)) {
            removeStaged(fileName);
            stageFlag = 1;
        }
        if (Repo.currentHead().fileExists(fileName)) {
            String id = Blob.contentID(fileName);
            addRemoval(fileName, id);
            trackedFlag = 1;
        }
        if (stageFlag == 0 && trackedFlag == 0) {
            Utils.message("No reason to remove the file.");
            return;
        }
        writeStaging();
    }

    private static void addRemoval(String fileName, String id) {
        readStaging();
        File temp = new File(Main.CWD.getPath()
                + File.separator + fileName);
        File dest = new File(REMOVE_FOLDER.getPath()
                + File.separator + id + ".txt");
        if (temp.exists()) {
            removal.put(fileName, id);
            try {
                Files.copy(temp.toPath(), dest.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Commit currHead = Repo.currentHead();
            removal.put(fileName, currHead.getBlobHash(fileName));
            File blob = new File(Repo.BLOBS.getPath()
                    + File.separator + currHead.getBlobHash(fileName) + ".txt");
            try {
                Files.copy(blob.toPath(), dest.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.restrictedDelete(fileName);
        writeStaging();
    }

    private static void removeRemoval(String fileName) {
        readStaging();
        String removeId = removal.get(fileName);
        File remove = new File(REMOVE_FOLDER.getPath()
                + File.separator + removeId + ".txt");
        remove.delete();
        removal.remove(fileName);
        writeStaging();
    }

    /** @param fileName name of the file in cwd
     * @param id blob's id
     * Adds a file to the stage naming it off its id
     * and placing fileName/id relationship in
     * staging TreeMap. */
    private static void addStaged(String fileName, String id) {
        readStaging();
        stage.put(fileName, id);
        File temp = new File(Main.CWD.getPath()
                + File.separator + fileName);
        File dest = new File(STAGE_FOLDER.getPath()
                + File.separator + id + ".txt");
        try {
            Files.copy(temp.toPath(), dest.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeStaging();
    }

    /** @param fileName name of the file to remove
     *  Removes the file from the stage TreeMap
     *  and also physically deletes file version
     *  copy inside the staging folder.
     */
    private static void removeStaged(String fileName) {
        readStaging();
        File temp = new File(STAGE_FOLDER.getPath() + File.separator
                + translateName(fileName) + ".txt");
        temp.delete();
        stage.remove(fileName);
        writeStaging();
    }

    /** Clears all the staging area. */
    public static void clearStagingArea()  {
        readStaging();
        Iterator<String> stageFiles = stage.keySet().iterator();
        while (stageFiles.hasNext()) {
            String currFile = stageFiles.next();
            String blobID = stage.get(currFile);
            File blob = new File(Staging.STAGING_FOLDER.getPath()
                    + File.separator + blobID + ".txt");
            blob.delete();
        }
        stage.clear();
        Iterator<String> removalFiles = removal.keySet().iterator();
        while (removalFiles.hasNext()) {
            String currFile = removalFiles.next();
            String blobID = removal.get(currFile);
            File blob = new File(Staging.REMOVE_FOLDER.getPath()
                    + File.separator + blobID + ".txt");
            blob.delete();
        }
        removal.clear();
        writeStaging();
    }

    /** @param fileName name of file
     * Converts fileName to corresponding blob id.
     * @return blob id corresponding to the name of file*/
    public static String translateName(String fileName) {
        readStaging();
        return stage.get(fileName);
    }

    /** @param fileName name of file
     * Converts fileName to corresponding blob id in removal.
     * @return blob id corresponding to the name of file*/
    public static String translateNameR(String fileName) {
        readStaging();
        return removal.get(fileName);
    }

    /** Accessor Method for stage.
     * @return the TreeMap that represents stage */
    public static TreeMap<String, String> getStage()  {
        readStaging();
        return stage;
    }

    /** Accessor Method for removal.
     * @return the TreeMap that represents removal */
    public static TreeMap<String, String> getRemoval()  {
        readStaging();
        return removal;
    }

    /** Persistence setup for the TreeMaps
     * in this class, stage and removal. */
    public static void writeStaging() {
        try {
            if (!STAGE_FILE.exists()) {
                STAGE_FILE.createNewFile();
            } else {
                REMOVAL_FILE.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(STAGE_FILE, stage);
        Utils.writeObject(REMOVAL_FILE, removal);
    }

    /** Reads the current files stored of the TreeMaps
     * of removal and staged so it has the most
     * up-to-date information about the staging area.
     * @SuppressWarning exists to block unchecked cast
     * warnings since reading the object will always
     * guarantee what is needed and there is no need for warnings. */
    @SuppressWarnings("unchecked")
    public static void readStaging() {
        removal = (TreeMap<String, String>)
                Utils.readObject(REMOVAL_FILE, TreeMap.class);
        stage = (TreeMap<String, String>)
                Utils.readObject(STAGE_FILE, TreeMap.class);
    }
}
