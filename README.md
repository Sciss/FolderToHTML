# FolderToHTML

[![Build Status](https://travis-ci.org/Sciss/FolderToHTML.svg?branch=master)](https://travis-ci.org/Sciss/FolderToHTML)

(C)opyright 2010&ndash;2016 Hanns Holger Rutz. This is in the public domain.

A simple command line tool to create an HTML index for a given folder. No bells or whistles.

This builds with sbt against Scala 2.11.

To run the executable:

    $ sbt
    > run [-f <htmlIndexFile>] <targetFolderToIndex>
     
Or `sbt 'run -f <htmlIndexFile>] <targetFolderToIndex>'`. To get all options use `sbt run` without extra arguments.
    
If the `-f` argument is omitted, the `index.html` file will be created inside the target folder.

__This software comes as is, absolute without warranties. Use at own risk.__

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

