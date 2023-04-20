# lightweightGit
lightweight version control system written in java, based on Git

**Main.java**
carries out git commands and error handling given incorrect arguments
**Blob.java**
uniquely serializes files regardless of potential name or content similarities and stores information in the centralized system
**Repo.java**
centralized "dictionary" system that stores a bunch of static mappings for commits, blobs, and pointers and saves them in files for persistence
**Staging.java**
handles actual movement of files and the effects of commands such as add, remove, commit
**Commit.java**
sets up "commits" as objects that support branching and checkout through parent pointers 
**Utils.java**
for serialization and file persistence utilities
