# FolderToHTML

(C)opyright 2010&ndash;2012 Hanns Holger Rutz. This is in the public domain.

A simple command line tool to create an HTML index for a given folder. No thrills.

This builds with sbt 0.11.3 against Scala 2.9.2 / Java SE 6.

To run the executable:

    $ sbt
    > run -C <targetFolderToIndex> [-f <htmlIndexFile>]
    
If the `-f` argument is omitted, the `index.html` file will be created (possibly overwriting!) inside the target folder.

__This software comes as is, absolute without warranties. Use at own risk.__

