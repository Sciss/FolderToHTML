lazy val root = project.in(file("."))
  .settings(
    name          := "FolderToHTML",
    version       := "0.12.1-SNAPSHOT",
    organization  := "de.sciss",
    scalaVersion  := "2.13.4",
    description   := "A simple tool to create a HTML index for a folder",
    homepage      := Some(url(s"https://github.com/Sciss/${name.value}")),
    libraryDependencies ++= Seq(
      "com.github.scopt"       %% "scopt"     % "4.0.0",
      "de.sciss"               %% "fileutil"  % "1.1.5"
    ),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint"),
  )
