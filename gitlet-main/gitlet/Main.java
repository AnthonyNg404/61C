package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Xinyu Fu
 */
public class Main implements Serializable {
    /** main method.
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            String[] command = new String [1];
            command[0] = "empty";
            process(command);
        } else if (!isValidCommand(args[0])) {
            System.out.println("No command with that name exists.");
        } else {
            if (alreadyInit()) {
                process(args);
            } else {
                if (args[0].equals("init")) {
                    init();
                } else {
                    System.out.println("Not in an "
                            + "initialized Gitlet directory.");
                }
            }
        }

    }

    /** check whether the given command is a valid one.
     * @param  args all arguments.
     * @return boolean. */
    private static boolean isValidCommand(String args) {
        String [] allCommand = {"init", "add", "commit", "rm", "log",
            "global-log", "find", "status", "checkout", "branch", "rm-branch",
            "reset", "merge", "add-remote", "rm-remote", "push",
            "fetch", "pull"};
        for (String command: allCommand) {
            if (command.equals(args)) {
                return true;
            }
        }
        return false;
    }

    /** initialize getLet.*/
    private static void init()  throws IOException  {
        final File getLetFolder = new File(".gitlet");
        getLetFolder.mkdir();
        final File commit = new File(".gitlet/commit");
        commit.mkdir();
        Commit commit1 = new Commit("initial commit",
                "null", "null");
        byte[] serializedCode = Utils.serialize(commit1);
        String sha1 = Utils.sha1(serializedCode);
        final File initcommitFile = new File(".gitlet/commit/" + sha1);
        initcommitFile.createNewFile();
        Utils.writeObject(initcommitFile, commit1);
        final File branch = new File(".gitlet/branch");
        branch.mkdir();
        final File master = new File(".gitlet/branch/master");
        master.createNewFile();
        Utils.writeContents(master, sha1);
        final File stagedForAddition = new File(".gitlet/additionStage");
        stagedForAddition.mkdir();
        final File stagedForRemoval = new File(".gitlet/removalStage");
        stagedForRemoval.mkdir();
        final File blobs = new File(".gitlet/blobs");
        blobs.mkdir();
        final File activeBranch = new File(".gitlet/activeBranch");
        activeBranch.createNewFile();
        Utils.writeContents(activeBranch, "master");
        final File remoteName = new File(".gitlet/remoteDir");
        remoteName.mkdir();
    }

    /** check whether getlet is already created.
     * @return boolean.*/
    private static boolean alreadyInit() {
        File commit = Utils.join(".gitlet");
        return commit.exists();
    }

    /** process current command.
     * @param args all arguments. */
    private static void process(String[] args) throws Exception {
        String command = args[0];
        switch (command) {
        case "empty":
            System.out.println("Please enter a command."); break;
        case "init":
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            break;
        case "add": add(args[1]); break;
        case "commit":
            if (args.length == 1) {
                System.out.println("Please enter a commit message");
            } else if (args.length == 2) {
                if (args[1].length() == 0) {
                    System.out.println("Please enter a commit message");
                } else {
                    commit(args[1], "null");
                }
            }
            break;
        case "rm": remove(args[1]); break;
        case "log": log(); break;
        case "global-log": globalLog(); break;
        case "find": find(args[1]); break;
        case "status": status(); break;
        case "checkout":
            int len = args.length;
            if (len == 3) {
                checkout(args[2]);
            } else if (len == 4) {
                if (!args[2].equals("--")) {
                    System.out.println("Incorrect operands.");
                    break;
                } else {
                    checkout(args[1], args[3]);
                }
            } else if (len == 2) {
                checkoutBranch(args[1]);
            }
            break;
        case "branch":
            branch(args[1]); break;
        case "rm-branch": rmBranch(args[1]); break;
        case "reset": reset(args[1]); break;
        case "merge": merge(args[1]); break;
        case "add-remote":
            addRemote(args[1], args[2]); break;
        case "rm-remote":
            removeRemote(args[1]); break;
        case "push":
            push(args[1], args[2]); break;
        case "fetch":
            fetch(args[1], args[2]); break;
        case "pull":
            pull(args[1], args[2]); break;
        default: int a = 1;
        }

    }

    /** push file to remote directory.
     * @param remoteBranchName remoteBranch name.
     * @param remoteName remote name.
     * */
    private static void push(String remoteName, String remoteBranchName)
            throws IOException {
        File remotedir = Utils.join(".gitlet/remoteDir", remoteName);
        String remotePathway = Utils.readContentsAsString(remotedir);
        File remoteGitlet = Utils.join(remotePathway);
        if (!remoteGitlet.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        File remoteBranchFile = Utils.join(remotePathway
                + "/branch", remoteBranchName);
        String remoteBranchSha1 = Utils.readContentsAsString(remoteBranchFile);
        Commit localHead = deserializedHeadCommit();
        HashMap<String, Integer> allLocalParentMap =
                allParents(localHead, 0, new HashMap<>());
        String activeBranch = readActiveBranch();
        String currLocalID = comiID(activeBranch);
        allLocalParentMap.put(currLocalID, 0);
        Set<String> allLocalParentID = allLocalParentMap.keySet();
        File localBlobFolder = Utils.join(".gitlet", "blobs");
        List<String> blobFiles = Utils.plainFilenamesIn(localBlobFolder);
        if (allLocalParentMap.get(remoteBranchSha1) != null) {
            for (String eachID: allLocalParentID) {
                File remoteCommitFile = Utils.join(remotePathway
                        + "/commit", eachID);
                if (!remoteCommitFile.exists()) {
                    File addRemoteFile = new File(remotePathway
                            + "/commit/" + eachID);
                    addRemoteFile.createNewFile();
                    File curLocalCommit = Utils.join(".gitlet/commit", eachID);
                    byte[] commitContent = Utils.readContents(curLocalCommit);
                    Utils.writeContents(addRemoteFile, commitContent);
                }
            }
            for (String eachBlob: blobFiles) {
                File eachBlobFile = Utils.join(remotePathway
                        + "/blobs", eachBlob);
                if (!eachBlobFile.exists()) {
                    File remoteBlob = new File(remotePathway
                            + "/blobs/" + eachBlob);
                    remoteBlob.createNewFile();
                    File curlocalBlob = Utils.join(".gitlet/blobs", eachBlob);
                    byte[] blobContent = Utils.readContents(curlocalBlob);
                    Utils.writeContents(remoteBlob, blobContent);
                }
            }
        } else {
            System.out.println("Please pull down "
                    + "remote changes before pushing.");
            return;
        }
        if (!remoteBranchFile.exists()) {
            File addRemoteBranchFile = new File(remotePathway
                    + "/branch/" + remoteBranchName);
            addRemoteBranchFile.createNewFile();
            File localBranch = Utils.join(".gitlet/branch", remoteBranchName);
            String localBranchID = Utils.readContentsAsString(localBranch);
            Utils.writeContents(addRemoteBranchFile, localBranchID);
        }
    }

    /** fetch files from remote dir.
     *
     * @param remoteName remote Name.
     * @param remoteBranchName remote Branch name.
     *
     */

    private static void fetch(String remoteName, String remoteBranchName)
            throws IOException {
        File remotedir = Utils.join(".gitlet/remoteDir", remoteName);
        String remotePathway = Utils.readContentsAsString(remotedir);
        File remoteGitlet = Utils.join(remotePathway);
        if (!remoteGitlet.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        File remoteBranchFile = Utils.join(remotePathway
                + "/branch", remoteBranchName);
        if (!remoteBranchFile.exists()) {
            System.out.println("That remote does not have that branch.");
            return;
        }
        String remoteBranchSha1 = Utils.readContentsAsString(remoteBranchFile);
        File remoteBranCo = Utils.join(remotePathway
                + "/commit", remoteBranchSha1);
        Commit remotebrachHead = Utils.readObject(remoteBranCo, Commit.class);
        HashMap<String, Integer> allLocalParentMap =
                allParents(remotebrachHead, 0, new HashMap<>());
        allLocalParentMap.put(remoteBranchSha1, 0);
        Set<String> allRemoteID = allLocalParentMap.keySet();
        File remoteBlobFolder = Utils.join(remotePathway, "blobs");
        List<String> blobFiles = Utils.plainFilenamesIn(remoteBlobFolder);
        for (String eachRemoteID: allRemoteID) {
            File localFile = Utils.join(".gitlet/commit", eachRemoteID);
            if (!localFile.exists()) {
                File addLocalFile = Utils.join(".gitlet/commit/"
                        + eachRemoteID);
                addLocalFile.createNewFile();
                File eachRemoteCommit = Utils.join(remotePathway
                        + "/commit", eachRemoteID);
                byte[] commitContent = Utils.readContents(eachRemoteCommit);
                Utils.writeContents(addLocalFile, commitContent);
            }
        }
        for (String eachBlob: blobFiles) {
            File eachBlobFile = Utils.join(".gitlet/blobs", eachBlob);
            if (!eachBlobFile.exists()) {
                File localBlob = new File(".gitlet/blobs/"
                        + eachBlob);
                localBlob.createNewFile();
                File remoteBlob = Utils.join(remotePathway
                        + "/blobs", eachBlob);
                byte[] blobContent = Utils.readContents(remoteBlob);
                Utils.writeContents(remoteBlob, blobContent);
            }
        }
        File remoteBranch = Utils.join(".gitlet/branch/"
                + remoteName + "-" + remoteBranchName);
        if (!remoteBranch.exists()) {
            Utils.writeContents(remoteBranch, remoteBranchSha1);
        }

    }

    /** pull commit from remote dir.
     * @
     * @param remoteName remote name.
     * @param remoteBranchName remote branchName.
     *
     */

    private static void pull(String remoteName, String remoteBranchName)
            throws Exception {
        fetch(remoteName, remoteBranchName);
        merge(remoteName + "-" + remoteBranchName);

    }
  /** remove this remote.
   * @param remoteName remotename. */
    private static void removeRemote(String remoteName) {
        File remoteFile = Utils.join(".gitlet/remoteDir", remoteName);
        if (!remoteFile.exists()) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remoteFile.delete();
    }



    /** add a remote pointer.
     * @param remoteDirName remote directory name.
     * @param  remoteName remote name. */
    private static void addRemote(String remoteName, String remoteDirName)
            throws IOException {
        File remoteFile = Utils.join(".gitlet/remoteDir", remoteName);
        if (remoteFile.exists()) {
            System.out.println("A remote with that name already exists.");
            return;
        }
        File remoteFile1 = new File(".gitlet/remoteDir/" + remoteName);
        remoteFile.createNewFile();
        Utils.writeContents(remoteFile1, remoteDirName);
    }
    /** get all parents of a commit.
     * @param commit given commit.
     * @param distance distance from head commit.
     * @param  parents all parents on that commit tree.
     * @return  all parents with distance. */
    private static HashMap<String, Integer>
        allParents(Commit commit, int distance, HashMap<String,
            Integer> parents) {
        if (commit.getParent().equals("null")) {
            if (commit.getSecondParent().equals("null")) {
                return parents;
            }
        } else if (commit.getSecondParent().equals("null")) {
            if (!commit.getParent().equals("null")) {
                String firstParent = commit.getParent();
                parents.put(firstParent, distance + 1);
                Commit firstParentCommit = deserilazedGivenCommit(firstParent);
                allParents(firstParentCommit, distance + 1, parents);
            }
        } else {
            String firstParent = commit.getParent();
            String secondParent = commit.getSecondParent();
            parents.put(firstParent, distance + 1);
            parents.put(secondParent, distance + 1);
            Commit firstParentCommit = deserilazedGivenCommit(firstParent);
            Commit secondParentCommit = deserilazedGivenCommit(secondParent);
            allParents(firstParentCommit, distance + 1, parents);
            allParents(secondParentCommit, distance + 1, parents);
        }
        return parents;
    }

    /** find the split point.
     * @param givenCommit givenCommit.
     * @param headCommit current head commit.
     * @param currID current commit ID.
     * @param givenID given commit ID.
     * @return split point sha1. */
    private static String splitPoint(Commit givenCommit, Commit headCommit,
                                     String currID, String givenID) {
        HashMap<String, Integer> currentParents =
                allParents(headCommit, 0, new HashMap<>());
        HashMap<String, Integer> givenParents =
                allParents(givenCommit, 0, new HashMap<>());
        HashMap<String, Integer> commonAncestor = new HashMap<>();
        currentParents.put(currID, 0);
        givenParents.put(givenID, 0);
        Set<String> curParentID = currentParents.keySet();
        for (String cur: curParentID) {
            if (givenParents.containsKey(cur)) {
                commonAncestor.put(cur, currentParents.get(cur));
            }
        }
        Set<String> commonAncestorID = commonAncestor.keySet();
        int shortest = WINNING_VALUE;
        String splitPoint = "";
        for (String each: commonAncestorID) {
            int dis = commonAncestor.get(each);
            if (dis < shortest) {
                shortest = dis;
                splitPoint = each;
            }
        }
        return splitPoint;
    }
    /** merge current branch to branchName.
     * @param branchName givenbranch name.*/

    private static void merge(String branchName) throws Exception {
        boolean isConflict = false;
        String cwd = System.getProperty("user.dir");
        List<String> fAdd = Utils.plainFilenamesIn(".gitlet/additionStage");
        String activeBranchID1 = readGivenBranch(readActiveBranch());
        String actBranStr = readActiveBranch();
        List<String> cwdFile = Utils.plainFilenamesIn(cwd);
        File branch = Utils.join(".gitlet/branch", branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        String giveID = comiID(branchName); String currID = comiID(actBranStr);
        Commit givenCommit = deserializedGivenCommit(branchName);
        Commit headCommit = deserializedHeadCommit();
        HashMap<String, String> giveFiles = givenCommit.getFiles();
        HashMap<String, String> currentFiles = headCommit.getFiles();
        Set<String> givenFileNames = giveFiles.keySet();
        Set<String> currentFileNames = currentFiles.keySet();
        String splitPoint = splitPoint(givenCommit, headCommit, currID, giveID);
        for (String eachCwdFile: cwdFile) {
            if (!currentFileNames.contains(eachCwdFile)) {
                if (!fAdd.contains(eachCwdFile)) {
                    System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                    return;
                }
            }
        }
        List<String> filesToRe = Utils.plainFilenamesIn(".gitlet/removalStage");
        if (fAdd.size() != 0 || filesToRe.size() != 0) {
            System.out.println("You have uncommitted changes."); return;
        }
        if (branchName.equals(actBranStr)) {
            System.out.println("Cannot merge a branch with itself."); return;
        }
        if (splitPoint.equals(activeBranchID1)) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branchName); return;
        }
        String givenBrID = readGivenBranch(branchName);
        if (splitPoint.equals(givenBrID)) {
            System.out.println("Given branch is an "
                + "ancestor of the current branch.");
            return;
        }
        File splitpointFile = Utils.join(".gitlet/commit", splitPoint);
        Commit splitCommit = Utils.readObject(splitpointFile, Commit.class);
        HashMap<String, String> splitFiles = splitCommit.getFiles();
        Set<String> splitSet = splitFiles.keySet();
        isConflict = whetherConflictnornot(isConflict, cwd,
                giveFiles, currentFiles, givenFileNames, splitFiles);
        isConflict = whetherConflict(isConflict, cwd, giveFiles,
                currentFiles, currentFileNames, splitFiles);
        functionThree(giveFiles, currentFiles, splitFiles, splitSet);
        commit("Merged " + branchName + " into " + actBranStr + ".", givenBrID);
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** merge fuction3.
     *
     * @param giveFiles giveFiles.
     * @param currentFiles currentFiles.
     * @param splitFiles splitFiles.
     * @param splitSet splitSet.
     *
     */
    private static void functionThree(HashMap<String, String> giveFiles,
                                      HashMap<String, String> currentFiles,
                                      HashMap<String, String> splitFiles,
                                      Set<String> splitSet)
            throws IOException {
        for (String eachSplitFile: splitSet) {
            String splitSHA1 = splitFiles.get(eachSplitFile);
            String givenSHA1 = giveFiles.get(eachSplitFile);
            String currentSHA1 = currentFiles.get(eachSplitFile);
            if (givenSHA1 == null) {
                if (splitSHA1.equals(currentSHA1)) {
                    remove(eachSplitFile);
                }
            } else {
                if (currentSHA1 == null) {
                    if (splitSHA1.equals(givenSHA1)) {
                        int g = 0;
                    }
                }
            }
        }
    }

    /** whether confict.
     *
     * @param  isConflict isConflict.
     * @param  cwd cwd.
     * @param  giveFiles giveFiles.
     * @param currentFiles  currentFiles.
     * @param givenFileNames givenFileNames.
     * @param splitFiles splitFiles.
     * @return boolean.
     *
     */
    private static boolean whetherConflictnornot(boolean isConflict, String cwd,
                                                 HashMap<String, String>
                                                         giveFiles,
                                                 HashMap<String, String>
                                                         currentFiles,
                                                 Set<String>
                                                         givenFileNames,
                                                 HashMap<String, String>
                                                         splitFiles)
            throws Exception {
        for (String givenFile: givenFileNames) {
            String givenSha1 = giveFiles.get(givenFile);
            String splitSha1 = splitFiles.get(givenFile);
            String currentSha1 = currentFiles.get(givenFile);
            if (splitSha1 != null) {
                if (currentSha1 != null) {
                    if (!givenSha1.equals(splitSha1)) {
                        if (splitSha1.equals(currentSha1)) {
                            File giveBlob = Utils.join(".gitlet/blobs",
                                    givenSha1);
                            String givenBlobContent = Utils.readObject(giveBlob,
                                    String.class);
                            File thisFile = new File(cwd + "/" + givenFile);
                            thisFile.createNewFile();
                            Utils.writeContents(thisFile, givenBlobContent);
                            add(givenFile);
                        } else {
                            if (givenSha1.equals(currentSha1)) {
                                int t = 1;
                            }
                        }
                    } else {
                        if (!splitSha1.equals(currentSha1)) {
                            int g  = 1;
                        }
                    }
                } else {
                    if (!givenSha1.equals(splitSha1)) {
                        File givenBlob = Utils.join(".gitlet/blobs", givenSha1);
                        String givenBlobContent = Utils.readObject(givenBlob,
                                String.class);
                        String content = "<<<<<<< HEAD\n" + "=======\n"
                                + givenBlobContent + ">>>>>>>\n";
                        File fileinCwd = Utils.join(cwd, givenFile);
                        Utils.writeContents(fileinCwd, content);
                        add(givenFile);
                        isConflict = true;

                    }
                }
            } else {
                if (currentSha1 == null) {
                    if (givenSha1 != null) {
                        File thisFile = new File(cwd + "/" + givenFile);
                        File givenBlob = Utils.join(".gitlet/blobs", givenSha1);
                        String givenBlobContent = Utils.readObject(givenBlob,
                                String.class);
                        thisFile.createNewFile();
                        Utils.writeContents(thisFile, givenBlobContent);
                        add(givenFile);
                    }
                }
            }
        }
        return isConflict;
    }

    /** check whether conflict.
     *
     * @param isConflict isConflict
     * @param cwd cwd
     * @param giveFiles giveFiles
     * @param currentFiles currentFiles
     * @param currentFileNames currentFileNames
     * @param splitFiles  splitFiles
     * @return boolean.
     */
    private static boolean whetherConflict(boolean isConflict, String cwd,
                                           HashMap<String, String> giveFiles,
                                           HashMap<String, String> currentFiles,
                                           Set<String> currentFileNames,
                                           HashMap<String, String> splitFiles)
            throws Exception {
        for (String eachcurrFile: currentFileNames) {
            String splitsha1 = splitFiles.get(eachcurrFile);
            String gisha = giveFiles.get(eachcurrFile);
            String cursha1 = currentFiles.get(eachcurrFile);
            if (splitsha1 == null) {
                if (gisha == null) {
                    int f = 0;
                } else {
                    if (!gisha.equals(cursha1)) {
                        File givenBlob = Utils.join(".gitlet/blobs", gisha);
                        String givenBlobContent = Utils.readObject(givenBlob,
                                String.class);
                        File currBlob = Utils.join(".gitlet/blobs",
                                cursha1);
                        String currContent =  Utils.readObject(currBlob,
                                String.class);
                        String content = "<<<<<<< HEAD\n" + currContent
                                + "=======\n"
                                + givenBlobContent + ">>>>>>>\n";
                        File fileinCwd = Utils.join(cwd, eachcurrFile);
                        Utils.writeContents(fileinCwd, content);
                        add(eachcurrFile); isConflict = true;
                    }
                }
            } else {
                if (gisha == null) {
                    if (!splitsha1.equals(cursha1)) {
                        File curBl = Utils.join(".gitlet/blobs", cursha1);
                        String currCon =  Utils.readObject(curBl, String.class);
                        String content = "<<<<<<< HEAD\n" + currCon
                                + "=======\n" + ">>>>>>>\n";
                        File fileinCwd = Utils.join(cwd, eachcurrFile);
                        Utils.writeContents(fileinCwd, content);
                        add(eachcurrFile); isConflict = true;
                    }
                } else {
                    if (!gisha.equals(cursha1)) {
                        if (!gisha.equals(splitsha1)) {
                            if (!cursha1.equals(splitsha1)) {
                                File giBlo = Utils.join(".gitlet/blobs", gisha);
                                String giveBCon = Utils.readObject(giBlo,
                                        String.class);
                                File currBlob = Utils.join(".gitlet/blobs",
                                        cursha1);
                                String currContent =  Utils.readObject(currBlob,
                                        String.class);
                                String content = "<<<<<<< HEAD\n" + currContent
                                        + "=======\n" + giveBCon + ">>>>>>>\n";
                                File fileinCwd = Utils.join(cwd, eachcurrFile);
                                Utils.writeContents(fileinCwd, content);
                                add(eachcurrFile); isConflict = true;
                            }
                        }
                    }
                }
            }
        }
        return isConflict;
    }

    /** reset a certain commit.
     *
     * @param commitID commit Sha1 ID.
     *
     */
    private static void reset(String commitID) throws IOException {
        String cwd = System.getProperty("user.dir");
        File givenCommitFile = Utils.join(".gitlet/commit", commitID);
        if (!givenCommitFile.exists()) {
            System.out.println("No commit with that id exists."); return;
        }
        Commit givenCommit = Utils.readObject(givenCommitFile, Commit.class);
        Commit headCommit = deserializedHeadCommit();
        HashMap<String, String> allFilesGivenCommit = givenCommit.getFiles();
        HashMap<String, String> allFilesCurCommit = headCommit.getFiles();
        Set<String> currentFileNames = allFilesCurCommit.keySet();
        Set<String> givenFilesNames = allFilesGivenCommit.keySet();
        for (String eachCurrentFile: currentFileNames) {
            File fileCWD = Utils.join(cwd, eachCurrentFile);
            String fileCWDSha1 = getFileSHA1(fileCWD);
            String fileInCurrentSha1 = allFilesCurCommit.get(eachCurrentFile);
            String fileInGivenSha1 = allFilesGivenCommit.get(eachCurrentFile);
            if (fileInGivenSha1 == null) {
                fileCWD.delete();
            } else {
                File givenBlob = Utils.join(".gitlet/blobs", fileInGivenSha1);
                String blobContent = Utils.readObject(givenBlob, String.class);
                if (fileCWDSha1.equals(fileInCurrentSha1)) {
                    if (givenFilesNames.contains(eachCurrentFile)) {
                        Utils.writeContents(fileCWD, blobContent);
                    } else {
                        fileCWD.delete();
                    }
                }
            }
        }
        for (String givenFiles: givenFilesNames) {
            File givenFileNamesCWD = Utils.join(cwd, givenFiles);
            if (!givenFileNamesCWD.exists()) {
                File givenFileCWD = new File(cwd + "/", givenFiles);
                givenFileCWD.createNewFile();
                String sha1 = allFilesGivenCommit.get(givenFiles);
                File blob = Utils.join(".gitlet/blobs", sha1);
                String content = Utils.readObject(blob, String.class);
                Utils.writeContents(givenFileCWD, content);
            } else {
                if (!currentFileNames.contains(givenFiles)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    return;
                }
            }
        }
        String activeBranch = readActiveBranch();
        File head = Utils.join(".gitlet/branch", activeBranch);
        Utils.writeContents(head, commitID);
        List<String> addiFile = Utils.plainFilenamesIn(".gitlet/additionStage");
        for (String fileadd: addiFile) {
            removefromStage(fileadd);
        }
        List<String> remolFile = Utils.plainFilenamesIn(".gitlet/removalStage");
        for (String fileRemove: remolFile) {
            removefromStage(fileRemove);
        }
    }
    /** read active branch.
     * @return activeBranchName.*/
    private static String readActiveBranch() {
        File activeBranchFileString = Utils.join(".gitlet", "activeBranch");
        return Utils.readContentsAsString(activeBranchFileString);
    }
    /** read given branch.
     * @param givenBranch givenBranch.
     * @return return the content of given branch.*/
    private static String readGivenBranch(String givenBranch) {
        File givenBranchFile = Utils.join(".gitlet/branch", givenBranch);
        return Utils.readContentsAsString(givenBranchFile);
    }
    /** remove givenBranch from branch.
     * @param branchName a branchName. */
    private static void rmBranch(String branchName) {
        File branchID = Utils.join(".gitlet/branch", branchName);
        String activeBranch = readActiveBranch();
        if (activeBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        if (!branchID.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        branchID.delete();

    }
    /** find head file in cwd.
     * @return return a file. */
    private static File headFile() {
        String activeBranch = readActiveBranch();
        File head = Utils.join(".gitlet/branch", activeBranch);
        return head;
    }

    /** create a branch.
     * @param branchName given BranchName.*/
    private static void branch(String branchName) throws IOException {
        File branchExist = Utils.join(".gitlet/branch", branchName);
        if (branchExist.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        File newBranch = new File(".gitlet/branch/" + branchName);
        newBranch.createNewFile();
        File head = headFile();
        String headSha1 = Utils.readContentsAsString(head);
        Utils.writeContents(newBranch, headSha1);
    }

    /** overwrite a file in CWD based on contents in current commit.
     * @param  commit given commit.
     * @param fileName given File name. */
    private static void overwriteCwdFile(Commit commit, String fileName) {
        String cwd = System.getProperty("user.dir");
        HashMap<String, String> blobs = commit.getFiles();
        String sha1 = blobs.get(fileName);
        if (sha1 == null) {
            System.out.println("File does not exist in that commit.");
        } else {
            File blob = Utils.join(".gitlet/blobs/" + sha1);
            String content = Utils.readObject(blob, String.class);
            File fileCWD = Utils.join(cwd + "/" + fileName);
            if (fileCWD.exists()) {
                Utils.writeContents(fileCWD, content);

            } else {
                File newFileCWD = new File(cwd, fileName);
                Utils.writeContents(newFileCWD, content);
            }
        }
    }
    /** checkout this file.
     * @param fileName checkout file name.*/
    private static void checkout(String fileName) {
        String cwd = System.getProperty("user.dir");
        Commit head = deserializedHeadCommit();
        HashMap<String, String> blobs  = head.getFiles();
        String sha1 = blobs.get(fileName);
        if (sha1 == null) {
            System.out.println("File does not exist in that commit.");
        } else {
            File blob = Utils.join(".gitlet/blobs/" + sha1);
            String content =  Utils.readObject(blob, String.class);
            File fileCWD = Utils.join(cwd + "/" + fileName);
            if (fileCWD.exists()) {
                Utils.writeContents(fileCWD, content);
            } else {
                File newFileCWD = new File(cwd, fileName);
                Utils.writeContents(newFileCWD, content);
            }
        }
    }
    /** checkout given commit.
     * @param commitID given commit id.
     * @param  fileName given file name.*/
    private static void checkout(String commitID, String fileName) {
        int len = commitID.length();
        boolean exist = false;
        List<String> allcommit = Utils.plainFilenamesIn(".gitlet/commit");
        for (String each: allcommit) {
            String abrra = each.substring(0, len);
            if (abrra.equals(commitID)) {
                File commitFile = Utils.join(".gitlet/commit", each);
                Commit commit = Utils.readObject(commitFile, Commit.class);
                overwriteCwdFile(commit, fileName);
                exist = true;
            }
        }
        if (!exist) {
            System.out.println("No commit with that id exists.");
        }
    }
    /** get sha1 of this file.
     * @param  thisFile this file.
     * @return  return a sha1. */
    private static String getFileSHA1(File thisFile) {
        String content = Utils.readContentsAsString(thisFile);
        byte[] ser = Utils.serialize(content);
        String sha1 = Utils.sha1(ser);
        return sha1;
    }
    /** checkout given branch.
     * @param branchName given branch name.*/
    private static void checkoutBranch(String branchName) throws IOException {
        boolean contains = false;

        branchName = branchName.replaceAll("\\/", "-");
        String cwd = System.getProperty("user.dir");


        File branchID = Utils.join(".gitlet/branch", branchName);
        if (!branchID.exists()) {
            System.out.println("No such branch exists.");
            return;
        }
        String branchContent = Utils.readContentsAsString(branchID);
        String activeBranch = readActiveBranch();
        if (branchName.equals(activeBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        File givenCommitFile = Utils.join(".gitlet/commit", branchContent);
        if (!givenCommitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit givenCommit = Utils.readObject(givenCommitFile, Commit.class);
        Commit headCommit = deserializedHeadCommit();
        HashMap<String, String> allFilesGivenCommit = givenCommit.getFiles();
        HashMap<String, String> allFilesCurCommit = headCommit.getFiles();
        Set<String> currentFileNames = allFilesCurCommit.keySet();
        Set<String> givenFilesNames = allFilesGivenCommit.keySet();
        currentfiles(cwd, allFilesGivenCommit,
                allFilesCurCommit, currentFileNames, givenFilesNames);
        for (String givenFiles: givenFilesNames) {
            File givenFileNamesCWD = Utils.join(cwd, givenFiles);
            if (!givenFileNamesCWD.exists()) {
                File givenFileCWD = new File(cwd + "/", givenFiles);
                givenFileCWD.createNewFile();
                String sha1 = allFilesGivenCommit.get(givenFiles);
                File blob = Utils.join(".gitlet/blobs", sha1);
                String content = Utils.readObject(blob, String.class);
                Utils.writeContents(givenFileCWD, content);
            } else {
                if (!currentFileNames.contains(givenFiles)) {
                    System.out.println("There is an untracked file in the "
                            + "way; delete it, or add and commit it first.");
                    return;
                }
            }
        }
        File activeBranchFileString = Utils.join(".gitlet", "activeBranch");
        File head = Utils.join(".gitlet/branch", activeBranch);
        Utils.writeContents(activeBranchFileString, branchName);
        List<String> addFile = Utils.plainFilenamesIn(".gitlet/additionStage");
        for (String fileadd: addFile) {
            removefromStage(fileadd);
        }
        List<String> remoFile = Utils.plainFilenamesIn(".gitlet/removalStage");
        for (String fileRemove: remoFile) {
            removefromStage(fileRemove);
        }
    }

    /** find current files in cwd.
     * @param cwd cwd
     * @param allFilesGivenCommit  allFilesGivenCommit.
     * @param allFilesCurCommit  allFilesCurCommit.
     * @param currentFileNames currentFileNames.
     * @param givenFilesNames givenFilesNames.
     */
    private static void currentfiles(String cwd,
                                     HashMap<String, String>
                                             allFilesGivenCommit,
                                     HashMap<String, String> allFilesCurCommit,
                                     Set<String> currentFileNames,
                                     Set<String> givenFilesNames) {
        for (String eachCurrentFile: currentFileNames) {
            File fileCWD = Utils.join(cwd, eachCurrentFile);
            String fileCWDSha1 = getFileSHA1(fileCWD);
            String fileInCurrentSha1 = allFilesCurCommit.get(eachCurrentFile);
            String fileInGivenSha1 = allFilesGivenCommit.get(eachCurrentFile);
            if (fileInGivenSha1 == null) {
                fileCWD.delete();
            } else {
                File givenBlob = Utils.join(".gitlet/blobs", fileInGivenSha1);
                String blobContent = Utils.readObject(givenBlob, String.class);
                if (fileCWDSha1.equals(fileInCurrentSha1)) {
                    if (givenFilesNames.contains(eachCurrentFile)) {
                        Utils.writeContents(fileCWD, blobContent);
                    } else {
                        fileCWD.delete();
                        System.out.println("should not enter here to delete");
                    }
                }
            }
        }
    }

    /** print out current status.*/
    private static void status() {
        Set<String> untracked = new HashSet<>();
        String cwd = System.getProperty("user.dir");
        Set<String>  modificationsNotStaged = new HashSet<>();
        Commit currentCommit = deserializedHeadCommit();
        HashMap<String, String> currentCommitFiles = currentCommit.getFiles();
        Set<String> currentCommitFileNames = currentCommitFiles.keySet();
        String activeBranch = readActiveBranch();
        System.out.println("=== Branches ===");
        System.out.println("*" + activeBranch);
        List<String> allBranch = Utils.plainFilenamesIn(".gitlet/branch");
        if (allBranch.size() > 1) {
            for (String eachBranch: allBranch) {
                if (!eachBranch.equals(activeBranch)) {
                    System.out.println(eachBranch);
                }
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        List<String> additionStage =
                Utils.plainFilenamesIn(".gitlet/additionStage");
        for (String add: additionStage) {
            File fileInAddition = Utils.join(".gitlet/additionStage", add);
            String addSha1 = getFileSHA1(fileInAddition);
            File fileInCWD = Utils.join(cwd, add);
            if (fileInCWD.exists()) {
                String sha1FileCWD  = getFileSHA1(fileInCWD);
                if (addSha1.equals(sha1FileCWD)) {
                    System.out.println(add);
                } else {
                    modificationsNotStaged.add(add + " (modified)");
                }
            } else {
                modificationsNotStaged.add(add + " (deleted)");
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        List<String> removal = Utils.plainFilenamesIn(".gitlet/removalStage");
        for (String remove: removal) {
            System.out.println(remove);
            File removelStillCWD = Utils.join(cwd, remove);
            if (removelStillCWD.exists()) {
                untracked.add(remove);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        modification(untracked, cwd, modificationsNotStaged,
                currentCommitFiles, currentCommitFileNames,
                additionStage, removal);
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String eachUntracked: untracked) {
            System.out.println(eachUntracked);
        }
        System.out.println();
    }

    /** try to find mofidification .
     *
     * @param untracked untracked set.
     * @param cwd current wd.
     * @param modificationsNotStaged mofication.
     * @param currentCommitFiles  current commit file.
     * @param currentCommitFileNames current commut file names.
     * @param additionStage  addtion stage.
     * @param removal  removal stage.
     */
    private static void modification(Set<String> untracked, String cwd,
                                     Set<String> modificationsNotStaged,
                                     HashMap<String, String> currentCommitFiles,
                                     Set<String> currentCommitFileNames,
                                     List<String> additionStage,
                                     List<String> removal) {
        for (String eachCurFile:currentCommitFileNames) {
            File fileCWD = Utils.join(cwd, eachCurFile);
            if (!fileCWD.exists()) {
                if (!removal.contains(eachCurFile)) {
                    modificationsNotStaged.add(eachCurFile + " (deleted)");
                }
            } else {
                String fileCWDSha1 = getFileSHA1(fileCWD);
                String fileInCurrentSha1 = currentCommitFiles.get(eachCurFile);
                if (!fileCWDSha1.equals(fileInCurrentSha1)) {
                    if (!additionStage.contains(eachCurFile)) {
                        modificationsNotStaged.add(eachCurFile + " (modified)");
                    }
                }
            }
        }
        for (String eachModified: modificationsNotStaged) {
            System.out.println(eachModified);
        }
        List<String> cwdFiles = Utils.plainFilenamesIn(cwd);
        for (String eachCwdFile: cwdFiles) {
            if (!currentCommitFileNames.contains(eachCwdFile)) {
                if (!additionStage.contains(eachCwdFile)) {
                    untracked.add(eachCwdFile);
                }
            }
        }
    }

    /** find commit based on message.
     * @param findMessage find a message. .*/
    private static void find(String findMessage) {
        List<String> allcommit = Utils.plainFilenamesIn(".gitlet/commit");
        boolean exist = false;
        for (String each: allcommit) {
            File commit  = Utils.join(".gitlet/commit/" + each);
            Commit eachCommit = Utils.readObject(commit, Commit.class);
            String message = eachCommit.getMessage();
            if (message.equals(findMessage)) {
                System.out.println(each);
                exist = true;
            }
        }
        if (!exist) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** print out all commit.
     *   ===
     *      commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
     *      Date: Thu Nov 9 20:00:05 2017 -0800
     *      A commit message.
     */

    private static void globalLog() {
        List<String>  allCommit = Utils.plainFilenamesIn(".gitlet/commit");
        for (String each: allCommit) {
            File commit  = Utils.join(".gitlet/commit/" + each);
            Commit eachCommit = Utils.readObject(commit, Commit.class);
            System.out.println("===");
            System.out.println("commit " + each);
            System.out.println("Date: " + eachCommit.getDate());
            System.out.println(eachCommit.getMessage());
            System.out.println();
        }
    }
    /** print out log info.*/
    private static void log() {
        String cwd = System.getProperty("user.dir");
        String activeBranch = readActiveBranch();
        File head = Utils.join(cwd + "/.gitlet/branch", activeBranch);
        String parentSha1 = Utils.readContentsAsString(head);
        File currentCommit = Utils.join(cwd + "/.gitlet/commit", parentSha1);
        Commit current = Utils.readObject(currentCommit, Commit.class);
        while (true) {
            System.out.println("===");
            System.out.println("commit " + parentSha1);
            System.out.println("Date: " + current.getDate());
            System.out.println(current.getMessage());
            System.out.println();
            parentSha1 = current.getParent();
            if (parentSha1.equals("null")) {
                break;
            }
            File parent = Utils.join(".gitlet/commit", parentSha1);
            current = Utils.readObject(parent, Commit.class);
        }
    }
    /** remove command.
     * @param file a file.*/

    private static void remove(String file) throws IOException {
        boolean neitherStageNorHead = true;
        String cwd = System.getProperty("user.dir");
        List<String> filesToAdd =
                Utils.plainFilenamesIn(".gitlet/additionStage");
        for (String eachFile: filesToAdd) {
            if (eachFile.equals(file)) {
                File thisFile = Utils.join(".gitlet/additionStage/" + file);
                thisFile.delete();
                neitherStageNorHead = false;
            }
        }
        Commit headCommit = deserializedHeadCommit();
        HashMap<String, String> files = headCommit.getFiles();
        if (files.get(file) != null) {
            String sha1 = files.get(file);
            File blob = Utils.join(".gitlet/blobs/" + sha1);
            String content = Utils.readObject(blob, String.class);
            File thisBlob = new File(cwd + "/.gitlet/removalStage/" + file);
            thisBlob.createNewFile();
            Utils.writeContents(thisBlob, content);
            File cwdFile = Utils.join(cwd + "/" + file);
            if (cwdFile.exists()) {
                cwdFile.delete();
            }
            neitherStageNorHead = false;
        }
        if (neitherStageNorHead) {
            System.out.println("No reason to remove the file");
        }
    }
    /** find headcommit sha1.
     * @return commit sha1. */

    private static String findHeadCommitSHA1() {
        String cwd = System.getProperty("user.dir");
        String activeBranch = readActiveBranch();
        File head = Utils.join(cwd + "/.gitlet/branch", activeBranch);
        head.exists();
        String headCommitSha1 = Utils.readContentsAsString(head);
        return headCommitSha1;
    }
    /** a commit method.
     * @param  message  commit message.
     * @param  secondPa potential second parent.
     */
    private static void commit(String message,
                               String secondPa) throws IOException {
        if (alreadyInit()) {
            List<String> filesToAdd =
                    Utils.plainFilenamesIn(".gitlet/additionStage");
            List<String> filesToRemove =
                    Utils.plainFilenamesIn(".gitlet/removalStage");
            if (filesToAdd.size() == 0 && filesToRemove.size() == 0) {
                System.out.println("No changes added to the commit.");
                return;
            }
            String activeBranch = readActiveBranch();
            String cwd = System.getProperty("user.dir");
            File head = Utils.join(cwd + "/.gitlet/branch", activeBranch);
            String headCommit = findHeadCommitSHA1();
            Commit commit = new Commit(message, headCommit, secondPa);
            byte[] ser = Utils.serialize(commit);
            String shA = Utils.sha1(ser);
            File commitFile = new File(cwd + "/.gitlet/commit/" + shA);
            commitFile.createNewFile();
            Utils.writeObject(commitFile, commit);
            Utils.writeContents(head, shA);
            File activebranch = Utils.join(".gitlet/branch", activeBranch);
            Utils.writeContents(activebranch, shA);
        }
    }
    /** deserialzed a given commit.
     * @param  commitID a commitID.
     * @return commit a commit*/
    private static Commit deserilazedGivenCommit(String commitID) {
        File givenCommit = Utils.join(".gitlet/commit", commitID);
        Commit commit = Utils.readObject(givenCommit, Commit.class);
        return commit;
    }
    /** deserialized GivenCommit.
     * @param branchName a branchname.
     * @return a commit.*/
    private static Commit deserializedGivenCommit(String branchName) {
        File givenBranch = Utils.join(".gitlet/branch", branchName);
        String givenCommitID =  Utils.readContentsAsString(givenBranch);
        File givenCommit = Utils.join(".gitlet/commit", givenCommitID);
        Commit commit = Utils.readObject(givenCommit, Commit.class);
        return commit;
    }
    /** return a commitID of a branch.
     * @param branchName a branchName.*/
    private static String comiID(String branchName) {
        File givenBranch = Utils.join(".gitlet/branch", branchName);
        String givenCommitID =  Utils.readContentsAsString(givenBranch);
        return givenCommitID;
    }
    /** deserilazed a head commit.
     * @return return a head commit*/
    private static Commit deserializedHeadCommit() {
        String cwd = System.getProperty("user.dir");
        String activeBranch = readActiveBranch();
        File head = Utils.join(cwd + "/.gitlet/branch", activeBranch);
        String headCommit = Utils.readContentsAsString(head);
        File currentCommit = Utils.join(cwd + "/.gitlet/commit", headCommit);
        Commit current = Utils.readObject(currentCommit, Commit.class);
        return current;
    }
    /**Adds a copy of the file as it currently exists to the staging area
     *the command.
     @param file a file. */
    private static void add(String file) throws Exception {
        String cwd = System.getProperty("user.dir");
        File fileContent = Utils.join(cwd, file);
        String stringContent = "";
        if (fileContent.exists()) {
            stringContent = Utils.readContentsAsString(fileContent);
        } else {
            System.out.println("File does not exist."); return;
        }
        String content = Utils.readContentsAsString(fileContent);
        byte []ser = Utils.serialize(content);
        String fileSha1 = Utils.sha1(ser);
        Commit current = deserializedHeadCommit();

        HashMap<String, String> getFiles = current.getFiles();
        boolean isSame = false;
        if (getFiles.get(file) != null) {
            String id = getFiles.get(file);
            if (fileSha1.equals(id)) {
                isSame = true;
            }
        }
        if (isInStageArea(file)) {
            removefromStage(file);
        }
        if (isSame) {
            return;
        }
        File fileToAdd = new File(cwd + "/.gitlet/additionStage/" + file);
        fileToAdd.createNewFile();
        Utils.writeContents(fileToAdd, stringContent);
    }
    /** remove this file from stage area.
     * @param file a file.*/
    public static void removefromStage(String file) {
        File thisfileAdd = Utils.join(".gitlet/additionStage", file);
        if (thisfileAdd.exists()) {
            thisfileAdd.delete();
        }
        File thisfileRemove = Utils.join(".gitlet/removalStage", file);
        if (thisfileRemove.exists()) {
            thisfileRemove.delete();
        }
    }

    /** check whether a file is int stage Area.
     * @param  file a file.
     * @return return a boolean.*/
    public static boolean isInStageArea(String file) {
        List<String> filesToAdd =
                Utils.plainFilenamesIn(".gitlet/additionStage");
        for (String each: filesToAdd) {
            if (file.equals(each)) {
                return true;
            }
        }
        List<String> filesToRemove =
                Utils.plainFilenamesIn(".gitlet/removalStage");
        for (String each: filesToRemove) {
            if (file.equals(each)) {
                return true;
            }
        }
        return false;
    }
}

