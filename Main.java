package gitlet;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Eric Yang
 */
public class Main {
    /** File format of the current working directory. */
    static final File CWD = new File(".");
    /** File format of the .gitlet directory. */
    static final File GITLET = new File(CWD.getPath()
            + File.separator + ".gitlet");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0 || args[0].equals("")) {
            Utils.message("Please enter a command.");
            return;
        }
        if (args[0].equals("init")) {
            Repo.init();
        } else if (!GITLET.exists()) {
            Utils.message("Not in an initialized Gitlet directory.");
            return;
        } else if (args[0].equals("add")) {
            Staging.add(args[1]);
        } else if (args[0].equals("commit")) {
            if (args[1] == null || args[1].equals("")) {
                Utils.message("Please enter a commit message.");
                return;
            }
            Staging.commit(args[1], 0, null, null);
        } else if (args[0].equals("checkout")) {
            if (args.length == 2) {
                checkout(args[1], null, null);
            } else if (args.length == 3) {
                checkout(args[1], args[2], null);
            } else if (args.length == 4) {
                checkout(args[1], args[2], args[3]);
            }
        } else if (args[0].equals("log")) {
            log();
        } else if (args[0].equals("global-log")) {
            globalLog();
        } else if (args[0].equals("rm")) {
            Staging.rm(args[1]);
        } else if (args[0].equals("find")) {
            find(args[1]);
        } else if (args[0].equals("branch")) {
            Repo.newBranch(args[1]);
        } else if (args[0].equals("status")) {
            status();
        } else if (args[0].equals("rm-branch")) {
            Repo.removeBranch(args[1]);
        } else if (args[0].equals("reset")) {
            reset(args[1]);
        } else if (args[0].equals("merge")) {
            merge(args[1]);
        } else if (args[0].equals("add-remote")) {
            new Remote(args[1], args[2]);
        } else {
            System.out.println("No command with that name exists.");
        }
    }

    /**@param first String of first argument
     * @param second String of second argument
     * @param third String of third argument
     * Checkouts various things depending on the inputs and does so
     * based upon the predetermined git rules and procedures. */
    public static void checkout(String first, String second, String third)  {
        if (first.equals("--")) {
            String commitID = Repo.translatePoint("HEAD");
            Commit current = Repo.translateCommit(commitID);
            if (!current.getTracked().containsKey(second)) {
                Utils.message("File doesn't exist in"
                        + "that commit.");
                return;
            }
            Blob.bringBlobToCWD(current.getBlobHash(second));
        } else if (third != null) {
            if (!second.equals("--")) {
                Utils.message("Incorrect operands.");
                return;
            }
            String commitID = Repo.equivCommit(first);
            if (commitID == null) {
                Utils.message("No commit with that id exists.");
                return;
            }
            Commit desiredCommit = Repo.translateCommit(commitID);
            if (desiredCommit.fileExists(third)) {
                String blobHash = desiredCommit.getBlobHash(third);
                Blob.bringBlobToCWD(blobHash);
            } else {
                Utils.message("File does not exist in that commit.");
                return;
            }
        } else {
            String branchName = first;
            if (!Repo.getDictPoint().containsKey(branchName)) {
                Utils.message("No such branch exists.");
                return;
            } else if (Repo.currentHeadBranch()
                    .equals(branchName)) {
                Utils.message("No need to checkout "
                        + "the current branch.");
                return;
            }
            List<String> files = Utils.plainFilenamesIn
                    (CWD.getPath());
            for (int i = 0; i < files.size(); i++) {
                if (!Repo.currentHead().fileExists(files.get(i))) {
                    if (Repo.translateCommit
                            (Repo.translatePoint(branchName))
                            .fileExists(files.get(i))) {
                        Utils.message("There is an untracked file in the way "
                                + "delete it, or add and commit it first.");
                        return;
                    }
                }
            }
            Commit newBranch = Repo.translateCommit
                    (Repo.translatePoint(branchName));
            Repo.changeBranch(branchName);
            Repo.changeHead(branchName);
            checkoutBranch(newBranch);
            Staging.clearStagingArea();
        }
    }

    /** @param newBranch branch/commit to checkout
     * Checkout external method to save line count in
     * original checkout method. */
    private static void checkoutBranch(Commit newBranch) {
        Iterator<String> fileNames = newBranch.getTracked()
                .keySet().iterator();
        while (fileNames.hasNext()) {
            String currName = fileNames.next();
            File trackedVersion = new File(Repo.BLOBS.getPath()
                    + File.separator
                    + newBranch.getBlobHash(currName) + ".txt");
            File cwdVersion = new File(Main.CWD.getPath()
                    + File.separator + currName);
            try {
                Files.copy(trackedVersion.toPath(), cwdVersion.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD.getPath());
        for (int i = 0; i < cwdFiles.size(); i++) {
            if (!Repo.currentHead().fileExists(cwdFiles.get(i))) {
                Utils.restrictedDelete(cwdFiles.get(i));
            }
        }
    }

    /** Shows the commits and the corresponding information
     * from the current HEAD commit to the initial commit. */
    public static void log() {
        String everything = "";
        Commit current = Repo.translateCommit
                (Repo.translatePoint("HEAD"));
        while (current != null) {
            if (current.getSecondParent() != null) {
                everything += "===" + "\n";
                everything += "commit " + current.id() + "\n";
                everything += "Merge: " + current.getParentString()
                        .substring(0, 7) + " "
                        + current.getSecParentString()
                        .substring(0, 7) + "\n";
                everything += "Date: " + current.getTime() + "\n";
                everything += current.getMessage();
                if (!current.getMessage()
                        .equals("initial commit")) {
                    everything += "\n\n";
                }
            } else {
                everything += "===" + "\n";
                everything += "commit " + current.id() + "\n";
                everything += "Date: " + current.getTime() + "\n";
                everything += current.getMessage();
                if (!current.getMessage()
                        .equals("initial commit")) {
                    everything += "\n\n";
                }
            }
            current = current.getParent();
        }
        System.out.println(everything);
    }

    /** Shows the commits and the corresponding information
     * of all commits stored not guaranteed in order. */
    public static void globalLog() {
        String everything = "";
        File commitFolder = new File(Repo.COMMITS.getPath());
        List<String> commits =
                Utils.plainFilenamesIn(commitFolder);
        for (int i = 0; i < commits.size(); i++) {
            String commitWithTxt = commits.get(i);
            String commitNoTxt = commitWithTxt.
                    substring(0, commitWithTxt.length() - 4);
            Commit current = Repo.translateCommit
                    (commitNoTxt);
            everything += "===" + "\n";
            everything += "commit " + current.id() + "\n";
            everything += "Date: " + current.getTime() + "\n";
            everything += current.getMessage();
            if (i < commits.size() - 1) {
                everything += "\n\n";
            }
        }
        System.out.println(everything);
    }

    /** @param message to look for in commits
     * Finds all the commits with given commit
     * message and prints the ids of those commits. */
    public static void find(String message) {
        String everything = "";
        File commitFolder = new File(Repo.COMMITS.getPath());
        List<String> commits =
                Utils.plainFilenamesIn(commitFolder);
        for (int i = 0; i < commits.size(); i++) {
            String commitWithTxt = commits.get(i);
            String commitNoTxt = commitWithTxt.
                    substring(0, commitWithTxt.length() - 4);
            Commit current = Repo.translateCommit(commitNoTxt);
            if (current.getMessage().equals(message)) {
                everything = current.id() + "\n" + everything;
            }
        }
        if (everything.equals("")) {
            Utils.message("Found no commit with that message.");
            return;
        }
        System.out.println(everything);
    }

    /** Displays the existing branches, marking
     * current branch with a *. Also shows which files
     * have been staged for addition or removal. */
    public static void status() {
        String everything = "=== Branches ===\n";
        String headBranch = Repo.currentHeadBranch();
        everything += "*" + headBranch + "\n";
        Iterator<String> branch = Repo.getDictPoint().keySet().iterator();
        while (branch.hasNext()) {
            String currBranch = branch.next();
            if (!currBranch.equals(headBranch)
                    && !currBranch.equals("HEAD")
                    && !currBranch.equals("HEAD_BRANCH")) {
                everything += currBranch + "\n";
            }
        }
        everything += "\n" + "=== Staged Files ===\n";
        Iterator<String> stage = Staging.getStage().keySet().iterator();
        while (stage.hasNext()) {
            String currFile = stage.next();
            everything += currFile + "\n";
        }
        everything += "\n" + "=== Removed Files ===\n";
        Iterator<String> removal = Staging.getRemoval().keySet().iterator();
        while (removal.hasNext()) {
            String currFile = removal.next();
            everything += currFile + "\n";
        }
        everything += "\n" + "=== Modifications Not "
                + "Staged For Commit ===\n";
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        for (int i = 0; i < cwdFiles.size(); i++) {
            String currFileName = cwdFiles.get(i);
            if (Staging.getStage().containsKey(currFileName)) {
                if (!Blob.contentID(currFileName)
                        .equals(Staging.translateName(currFileName))) {
                    everything += currFileName + " (modified)\n";
                }
            } else {
                Commit head = Repo.currentHead();
                if (head.fileExists(currFileName)) {
                    if (Blob.contentID(currFileName) != null) {
                        if (!Blob.contentID(currFileName)
                                .equals(head.getBlobHash(currFileName))) {
                            everything += currFileName + " (modified)\n";
                        }
                    }
                }
            }
        }
        everything = statusDeleted(everything);
        everything += "\n" + "=== Untracked Files ===\n";
        Commit head = Repo.currentHead();
        everything = statusUntracked(cwdFiles, head, everything);
        System.out.println(everything);
    }

    /** @param everything String output to add onto
     * Handles deleted files since such will not show
     * up in the List collection from utility function
     * also in separate function for saving lines.
     * @return everything String to continue building on */
    private static String statusDeleted(String everything) {
        Iterator<String> stagedFiles = Staging.getStage()
                .keySet().iterator();
        while (stagedFiles.hasNext()) {
            String currFile = stagedFiles.next();
            if (Blob.contentID(currFile) == null) {
                everything += currFile + " (deleted)\n";
            }
        }
        Commit currHead = Repo.currentHead();
        Iterator<String> trackedFiles = currHead.getTracked()
                .keySet().iterator();
        while (trackedFiles.hasNext()) {
            String currFile = trackedFiles.next();
            if (!Staging.getRemoval()
                    .containsKey(currFile)) {
                if (Blob.contentID((currFile)) == null) {
                    everything += currFile + " (deleted)\n";
                }
            }
        }
        return everything;
    }

    /** @param cwdFiles files in cwd
     *  @param head current head commit
     *  @param everything string to build on for output
     * External method to carry out Untracked Files part of
     * status to save line number count.
     * @return everything string to use as result of status */
    private static String statusUntracked(List<String> cwdFiles,
                                          Commit head, String everything) {
        for (int i = 0; i < cwdFiles.size(); i++) {
            String currFileName = cwdFiles.get(i);
            if (!Staging.getStage().containsKey(currFileName)
                    && !head.fileExists(currFileName)) {
                if (Blob.contentID(currFileName) != null) {
                    everything += currFileName + "\n";
                }
            } else if (Staging.getRemoval().containsKey(currFileName)) {
                if (Staging.getRemoval().get(currFileName)
                        .equals(Blob.contentID(currFileName))) {
                    everything += currFileName + "\n";
                }
            }
        }
        return everything;
    }

    /** @param id String commit id to reset to
     * Essentially checkouts a commit. */
    public static void reset(String id) {
        String actualCommit = Repo.equivCommit(id);
        if (actualCommit == null) {
            Utils.message("No commit with that id exists.");
            return;
        }
        List<String> files = Utils.plainFilenamesIn
                (CWD.getPath());
        for (int i = 0; i < files.size(); i++) {
            if (!Repo.currentHead()
                    .fileExists(files.get(i))) {
                if (Repo.translateCommit(actualCommit)
                        .fileExists(files.get(i))) {
                    Utils.message("There is an untracked file "
                            + "in the way; delete it, "
                            + "or add and commit it first.");
                    return;
                }
            }
        }
        Commit prevHead = Repo.translateCommit
                (Repo.translatePoint("HEAD"));
        Repo.changePointer("HEAD", actualCommit);
        Repo.changePointer(Repo.translatePoint("HEAD_BRANCH"), actualCommit);
        Commit currHead = Repo.currentHead();
        Staging.clearStagingArea();
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD.getPath());
        int count = 0;
        while (count < cwdFiles.size()) {
            String currCWDFile = cwdFiles.get(count);
            if (prevHead.fileExists(currCWDFile)) {
                Utils.restrictedDelete(currCWDFile);
            }
            count++;
        }
        Iterator<String> headFiles = currHead.getTracked()
                .keySet().iterator();
        while (headFiles.hasNext()) {
            String currTrackFile = headFiles.next();
            checkout(actualCommit,
                    "--", currTrackFile);
        }
    }

    /** Carries out the entire process of
     * merging two branches and doing the
     * appropriate things based on merge rules.
     * @param branchName name of branch to merge
     * current branch with */
    public static void merge(String branchName) {
        List<String> files = Utils.plainFilenamesIn
                (CWD.getPath());
        for (int i = 0; i < files.size(); i++) {
            if (!Repo.currentHead().fileExists(files.get(i))) {
                if (!Staging.getStage().isEmpty()
                        || !Staging.getRemoval().isEmpty()) {
                    Utils.message("You have uncommitted changes.");
                    return;
                } else if (Repo.translateCommit
                                (Repo.translatePoint(branchName))
                        .fileExists(files.get(i))) {
                    Utils.message("There is an untracked file in the way "
                            + "delete it, or add and commit it first.");
                    return;
                }
            }
        }
        if (!Repo.getDictPoint()
                .containsKey(branchName)) {
            Utils.message("A branch with that name does not exist.");
            return;
        }
        Commit givenCommit = Repo.translateCommit
                (Repo.translatePoint(branchName));
        Commit split = splitPoint(Repo.currentHeadBranch(),
                branchName);
        Commit curr = Repo.currentHead();
        if (Repo.currentHeadBranch().equals(branchName)) {
            Utils.message("Cannot merge a branch with itself.");
            return;
        } else if (split.id().equals(givenCommit.id())) {
            Utils.message("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        } else if (split.id().equals(curr.id())) {
            checkout(branchName, null, null);
            Utils.message("Current branch fast-forwarded.");
            return;
        }
        Iterator<String> currKeys = Repo.currentHead().getTracked()
                .keySet().iterator();
        Iterator<String> givenKeys = givenCommit.getTracked()
                .keySet().iterator();
        ArrayList<String> currFiles = new ArrayList<String>();
        ArrayList<String> givenFiles = new ArrayList<String>();
        while (currKeys.hasNext()) {
            currFiles.add(currKeys.next());
        }
        while (givenKeys.hasNext())  {
            givenFiles.add(givenKeys.next());
        }
        mergeTech(currFiles, givenFiles, curr, givenCommit, split, branchName);
    }

    /** @param currFiles list of current file names
     * @param givenFiles list of given file names
     * @param curr the current commit
     * @param givenCommit the given commit
     * @param split the common ancestor commit
     * @param branchName name of the given commit branch
     * Carries out the technical work of merge with its
     * cases, separation is more for line conservation.
     */
    private static void mergeTech(ArrayList<String> currFiles,
                                  ArrayList<String> givenFiles,
                                  Commit curr, Commit givenCommit,
                                  Commit split, String branchName) {
        Staging.clearStagingArea();
        int flag = 1;
        for (int i = 0; i < currFiles.size(); i++) {
            String currFile = currFiles.get(i);
            String currBlob = curr.getBlobHash(currFile);
            if (split.fileExists(currFile)) {
                String splitBlob = split.getBlobHash(currFile);
                if (givenCommit.fileExists(currFile)) {
                    String givBlob = givenCommit.getBlobHash(currFile);
                    if (currBlob.equals(splitBlob)) {
                        givenFiles.remove(currFile);
                        if (!givBlob.equals(currBlob)) {
                            Blob.bringBlobToCWD(givBlob);
                            Staging.add(currFile);
                        }
                    } else if (!splitBlob.equals(currBlob)
                            && !splitBlob.equals(givBlob)
                            && !currBlob.equals(givBlob)) {
                        mergeConflict(currFile, curr, givenCommit);
                        givenFiles.remove(currFile);
                        flag = 2;
                    }
                } else {
                    if (currBlob.equals(splitBlob)) {
                        Staging.rm(currFile);
                    } else {
                        mergeConflict(currFile, curr, null);
                        flag = 2;
                    }
                }
            } else {
                if (curr.fileExists(currFile)
                        && givenCommit.fileExists(currFile)
                        && !curr.getBlobHash(currFile)
                        .equals(givenCommit.getBlobHash(currFile))) {
                    mergeConflict(currFile, curr, givenCommit);
                    givenFiles.remove(currFile);
                    flag = 2;
                }
            }
        }
        for (int i = 0; i < givenFiles.size(); i++)  {
            String giveFile = givenFiles.get(i);
            String giveBlob = givenCommit.getBlobHash(giveFile);
            if (!split.fileExists(giveFile)) {
                if (!curr.fileExists(giveFile)) {
                    checkout(Repo.translatePoint(branchName), "--", giveFile);
                    Staging.add(giveFile);
                }
            } else {
                if (!split.getBlobHash(giveFile).equals(giveBlob)) {
                    mergeConflict(giveFile, null, givenCommit);
                    flag = 2;
                }
            }
        }
        Staging.commit("MERGE", flag, givenCommit.id(), branchName);
    }

    /** @param fileName name of file to resolve
     * @param curr current commit
     * @param given given branch commit
     * Carries out the merge conflict, making a
     * new Blob and adding it to the dictionary
     * and blob folders. */
    private static void mergeConflict(String fileName,
                                      Commit curr, Commit given) {
        File inCWD = new File(Main.CWD.getPath()
                + File.separator + fileName);
        String everything = "<<<<<<< HEAD\n";
        if (curr != null) {
            File currBlob = new File(Repo.BLOBS.getPath()
                    + File.separator + curr.getBlobHash(fileName) + ".txt");
            everything += Utils.readContentsAsString(currBlob);
        }
        everything += "=======\n";
        if (given != null) {
            File givenBlob = new File(Repo.BLOBS.getPath()
                    + File.separator + given.getBlobHash(fileName) + ".txt");
            everything += Utils.readContentsAsString(givenBlob);
        }
        everything += ">>>>>>>\n";
        Utils.writeContents(inCWD, everything);
        Staging.add(fileName);
    }


    /**@param current string branch currently on
     * @param given string given branch
     * Sets up a list of all of given's nodes and parents
     * then searches through starting from head to find
     * optimal split point.
     * @return closest split point to head*/
    private static Commit splitPoint(String current, String given) {
        Commit givCommit = Repo.translateCommit(Repo.translatePoint(given));
        ArrayList<String> givAncestors = new ArrayList<String>();
        Commit curCommit = Repo.translateCommit(Repo.translatePoint(current));
        makeList(givCommit, givAncestors);
        Commit split = search(curCommit, givAncestors);
        return split;
    }

    /**@param commit commit to start at, should be head commit
     * @param possible split points of the head
     * Does a breadth first search from the given commit
     * and checks if a current node is in the possible
     * split points list and returns it as soon as possible.
     * @return the commit that is the optimal split point */
    public static Commit search(Commit commit, ArrayList<String> possible) {
        ArrayList<String> visited = new ArrayList<String>();
        LinkedList<Commit> queue = new LinkedList<Commit>();
        ArrayList<String> parents = new ArrayList<String>();

        Commit currCommit = commit;
        visited.add(currCommit.id());
        queue.add(currCommit);
        while (queue.size() != 0) {
            currCommit = queue.poll();
            if (possible.contains(currCommit.id())) {
                return currCommit;
            }
            if (currCommit.getParent() != null) {
                parents.add(currCommit.getParent().id());
                if (currCommit.getSecondParent() != null) {
                    parents.add(currCommit.getSecondParent().id());
                }
            }
            while (parents.size() > 0) {
                if (!visited.contains(parents.get(0))) {
                    visited.add(parents.get(0));
                    queue.add(Repo.translateCommit
                            (parents.get(0)));
                    parents.remove(0);
                }
            }
        }
        return null;
    }

    /**@param commit the commit to find parents of
     * @param parents list of ids of parent commits
     * Makes a list of the parents of a given commits. */
    public static void makeList(Commit commit, ArrayList<String> parents) {
        if (commit.getParent() == null) {
            parents.add(commit.id());
        } else  {
            parents.add(commit.id());
            if (commit.getSecondParent() != null) {
                makeList(commit.getParent(), parents);
                makeList(commit.getSecondParent(), parents);
            } else {
                makeList(commit.getParent(), parents);
            }
        }
    }

}
