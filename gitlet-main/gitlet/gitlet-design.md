# Gitlet Design Document

**Name**: Xinyu Fu


## Classes and Data Structures
### Main
check 
###Commit 
a folder has files which contain all serialized commits. In each file, it has a commit object.
Name of each file is the sha1 id of each commit object. 
#### instance variable 
* Message  - contains the message of a commit written by user when input a commit command.
* Date - time at which a commit was created (by using a function)
* Parent-  commit sha 1 id of its parent commit. When the first commit is created, its parent is null. 
* Blob - sha i id of its blob. 

###Blob
a folder has files which contains all serialized blobs. In each file, it has a serialized blob content. 
(use sha 1 id of for the name of blob). 
### staging area
two folders used for addition and removal before files are committed.
the name of files in additionStage and removalStage is files'names, and contents of files in additionStage and removal Stage 
are normal unserialized strings.

### head 
a file which contains sha 1 id  of current commit

###Master 
a file which contains sha 1 id of master branch 

## Algorithms
 ### commit 
 get sha 1 id for current file, go to the head file to find the latest commit (parent commit),
 deserialize its parent commit, compare the sha 1 id between current file and parent file.
 If two sha i ids are different, create a new blob for current commit. Otherwise, save information of its parent's blob.
 ### blob
If user adds a new file, create a blob for this file. 
 
## Persistence
When user runs the program, the program will open the head file to get sha 1 id of current commit. 
It will use the sha 1 id of head to find the corresponding file in commit folder for future purposes. 
After user commits, serialize current commit. Then, create a file to store serialized contents,
and use the name of sha 1 id as the name of the file. 
Only deserialize specific commit and blob that user needs 

