package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Blob implements Serializable {


    /** @param fileName name of file currently in cwd
     *  Returns the id of the file with given name
     *  in CWD. */
    public static String contentID(String fileName) {
        File temp = new File(Main.CWD + "/" + fileName);
        if (!temp.exists()) {
            return null;
        }
        return Utils.sha1(fileName + Utils.readContentsAsString(temp));
    }

    /** @param fileName name of file in CWD
     * Saves a file currently in CWD to a blob folder into a blob file
     * also saves file name and id into the repository dictionary */
    public static void saveFileToBlobs(String fileName) {
        File fileToBlob = new File(Main.CWD.getPath()
                + File.separator + fileName);
        if (fileToBlob.exists()) {
            File blobFile =  new File(Repo.BLOBS.getPath()
                    + File.separator + contentID(fileName) + ".txt");
            try {
                Files.copy(fileToBlob.toPath(), blobFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Repo.putBlob(contentID(fileName), fileName);
        }
    }

    /** @param id hash of the blob
     * Removes the file from the blobs folder and removes its entry
     * in the repository dictionary
     */
    public static void removeFileOfBlob(String id) {
        File blob = new File(Repo.BLOBS.getPath()
                + File.separator + id + ".txt");
        if (blob.exists()) {
            Utils.restrictedDelete(blob);
            Repo.removeBlob(id);
        }
    }

    /** @param id name of blob
     * Brings the blob with given id into CWD, replacing
     * the current file with that name.
     */
    public static void bringBlobToCWD(String id) {
        File blob = new File(Repo.BLOBS.getPath()
                + File.separator + id + ".txt");
        if (blob.exists()) {
            String fileName = Repo.translateBlob(id);
            File currFile = new File(Main.CWD
                    + File.separator + fileName);
            try {
                Files.copy(blob.toPath(), currFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
