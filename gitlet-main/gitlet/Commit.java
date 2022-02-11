package gitlet;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
/** make a commit.
 *  @author Xinyu Fu
 *
 *  */
public class Commit implements Serializable {
    /** commit constructor.
     * @param message commit message
     * @param  parent commit's parent
     * @param  secondParent commit's second parent.
     *
     * .*/
    public Commit(String message, String parent,
                  String secondParent) throws IOException {
        _secondParent = secondParent;
        _parent = parent;
        _message = message;
        SimpleDateFormat formatter = new
                SimpleDateFormat("E MMM dd hh:mm:ss yyyy Z");
        if (parent.equals("null")) {
            _date = formatter.format(new Date(0));
        } else {
            _date = formatter.format(new Date(System.currentTimeMillis()));
        }
        if (!parent.equals("null")) {
            File parentFile = Utils.join(".gitlet/commit/" + parent);
            Commit parentCommit = Utils.readObject(parentFile, Commit.class);
            List<String> filesToAdd =
                    Utils.plainFilenamesIn(".gitlet/additionStage");
            List<String> filesToRemove =
                    Utils.plainFilenamesIn(".gitlet/removalStage");
            this._files = parentCommit._files;
            if (filesToAdd.size() != 0) {
                for (String file: filesToAdd) {
                    File thisFile = Utils.join(".gitlet/additionStage/" + file);
                    String content = Utils.readContentsAsString(thisFile);
                    byte[] ser = Utils.serialize(content);
                    String sha1 = Utils.sha1(ser);
                    File blobFile = new File(".gitlet/blobs/" + sha1);
                    blobFile.createNewFile();
                    Utils.writeObject(blobFile, content);
                    if (hasFile(file, parentCommit)) {
                        this._files.replace(file, sha1);
                    } else {
                        this._files.put(file, sha1);
                    }
                    thisFile.delete();
                }
            }
            if (filesToRemove.size() != 0) {
                for (String files: filesToRemove) {
                    this._files.remove(files);
                    File fileToRemove =
                            Utils.join(".gitlet/removalStage/" + files);
                    fileToRemove.delete();
                }
            }
        } else {
            this._files = new HashMap<>();
        }
    }
    /** file name as key, sha 1 id of file as value.
     * @return return a hashmap.*/
    public HashMap<String, String> getFiles() {
        return _files;
    }

    /** whether this commit contains that file.
     * @param  file a file.
     * @param commit a commit.
     * @return return whether this commit has this file. */
    public boolean hasFile(String file, Commit commit) {
        return commit._files.get(file) != null;
    }
    /** get the second parent.
     * @return  return 2nd parent. */
    public String getSecondParent() {
        return _secondParent;
    }
    /** get this parent.
     * @return this parent. */
    public String getParent() {
        return this._parent;
    }
    /** get the date.
     * @return commit date. */
    public String getDate() {
        return this._date;
    }
    /** get message.
     * @return commit message. */
    public String getMessage() {
        return this._message;
    }
    /** the second parent if necessary.*/
    private String _secondParent;
    /**  file name as key, sha 1 id of file as value.*/
    private HashMap<String, String> _files;
    /** sha 1 id of its parent.*/
    private String _parent;
    /** parent of this commit.*/
    private String _date;
    /** message of the commit.*/
    private String _message;

}
