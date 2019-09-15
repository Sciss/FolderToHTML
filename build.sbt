name          := "FolderToHTML"
version       := "0.12.1-SNAPSHOT"
organization  := "de.sciss"
scalaVersion  := "2.12.9"
description   := "A simple tool to create a HTML index for a folder"
homepage      := Some(url(s"https://github.com/Sciss/${name.value}"))

libraryDependencies ++= Seq(
  "com.github.scopt"       %% "scopt"     % "3.7.1",
  "de.sciss"               %% "fileutil"  % "1.1.3"
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")
