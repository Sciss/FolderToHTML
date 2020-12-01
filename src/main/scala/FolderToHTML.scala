// written by Hanns Holger Rutz. Placed in the public domain.

import de.sciss.file._
import scopt.OptionParser

import java.awt.image.BufferedImage
import java.io.RandomAccessFile
import java.text.DateFormat
import java.util.{Date, Locale}
import javax.imageio.ImageIO
import javax.swing.filechooser.FileSystemView
import javax.swing.{JLabel, UIManager}
import scala.collection.mutable
import scala.util.control.NonFatal

object FolderToHTML {
  case class Config(index: Option[File] = None, directory: File = file(""),
                    hidden: Boolean = false, icons: Boolean = false,
                    size: Boolean = false, date: Boolean = false, overwrite: Boolean = false,
                    recursive: Boolean = false, title: String = "")

  def main(args: Array[String]): Unit = {
    val parser = new OptionParser[Config]("FolderToHTML") {
      opt[File]('f', "index")     text "Index .html file"                        action { (x, c) => c.copy(index      = Some(x)) }
      arg[File]("directory").required() text "Directory to index"                action { (x, c) => c.copy(directory  = x) }
      opt[Unit]('a', "hidden")    text "Include hidden files"                    action { (_, c) => c.copy(hidden     = true) }
      opt[Unit]('i', "icons")     text "Generate icons"                          action { (_, c) => c.copy(icons      = true) }
      opt[Unit]('z', "size")      text "Generate column with file sizes"         action { (_, c) => c.copy(size       = true) }
      opt[Unit]('m', "date")      text "Generate column with last-modified date" action { (_, c) => c.copy(date       = true) }
      opt[Unit]('y', "overwrite") text "Force overwrite existing index"          action { (_, c) => c.copy(overwrite  = true) }
      opt[Unit]('r', "recursive") text "Recursively created sub-directory indices" action { (_, c) => c.copy(recursive= true) }
      opt[String]('t', "title") text "Title (defaults to directory name)" action { (x, c) => c.copy(title = x) }
    }
    parser.parse(args, Config()).fold(sys.exit(1))(run)
  }

  def run(config: Config): Unit =
    try {
      perform(config)
      System.exit(0)
    }
    catch {
      case NonFatal(e) =>
        e.printStackTrace()
        sys.exit(1)
    }

  def perform(config: Config): Unit = {
    import config.{index => _, _}
    val index = config.index.getOrElse(directory / "index.html")
    if (index.exists() && !overwrite) {
      Console.err.println(s"Index '$index' already exists. Not overwriting.")
      sys.exit(1)
    }

    val iconDir   = directory / "_icons"
    if (icons) {
      iconDir.mkdir()
      iconDir.children(_.ext.toLowerCase == "png").foreach(_.delete())
      // 'Ocean' has really crappy icons. Try to find 'Nimbus'.
      UIManager.getInstalledLookAndFeels.find(_.getName == "Nimbus").foreach { info =>
        try {
          val className = info.getClassName
  //        val className = UIManager.getSystemLookAndFeelClassName
          UIManager.setLookAndFeel(className)
        } catch {
          case NonFatal(_) => // ignore
        }
      }
    }

    val df        = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US)
    val fsv       = FileSystemView.getFileSystemView

//    implicit val nameComparator: Comparator[String] = (s1: String, s2: String) => compareName(s1, s2)

    val files     = fsv.getFiles(directory, !hidden).toList.sortBy(_.name).filterNot { f =>
      f == index || (icons && f == iconDir)
    }
    val images    = mutable.Map.empty[String, String]
    if (index.exists()) index.delete()
    val htmlFile  = new RandomAccessFile(index, "rw")

    val html      = new StringBuilder()
    val titleStr  = if (title.isEmpty) directory.name else title
    html.append(
      s"""<!DOCTYPE html>
         |<HTML>
         |<HEAD>
         |<TITLE>Index of "${escapeHTML(titleStr)}"</TITLE>
         |<STYLE TYPE="text/css"><!--
         |  BODY {
         |    margin:32px;
         |  }
         |  TR.head {
         |    font-weight:bold;
         |  }
         |  TD {
         |    font-family:"Liberation Sans","Lucida Grande","Helvetica","Arial","sans-serif";
         |    font-size:12px;
         |    padding-right:20px;
         |    vertical-align:baseline;
         |  }
         |  TD.size {
         |    text-align:right;
         |  }
         |  TD.date {
         |    text-align:center;
         |  }
         |  A {
         |    text-decoration:none;
         |  }
         |  A:hover {
         |    text-decoration:underline;
         |  }
         |  A IMG {
         |    border:none;
         |    padding-right:4px;
         |  }
         |-->
         |</STYLE>
         |</HEAD>
         |<BODY>
         |<TABLE>
         |""".stripMargin
    )

    html.append("""<TR CLASS="head"><TD CLASS="name">Name</TD>""")
    if (size) html.append("""<TD CLASS="size">Size</TD>""")
    if (date) html.append("""<TD CLASS="date">Last modified</TD>""")
    html.append("</TR>\n")

    files.foreach { f =>
      html.append("<TR>")
      val nameStr = escapeHTML(f.name)
      html.append(s"""<TD CLASS="name"><A HREF="$nameStr">""")
      if (icons) {
        val imgName = getIconName(f, fsv, images, iconDir)
        html.append(s"""<IMG SRC="${iconDir.name}/$imgName" ALT="icon"/>""")
      }
      html.append(s"""$nameStr</TD>""")
      if (size) {
        val szStr = if (f.isFile) {
          val bytes = f.length
          s"${bytes / 1024} KB"
        } else {
          ""
        }
        html.append(s"""<TD CLASS="size">$szStr</TD>""")
      }
      if (date) {
        val dateStr = df.format(new Date(f.lastModified))
        html.append(s"""<TD CLASS="date">$dateStr</TD>""")
      }
      html.append("</TR>\n")

      if (f.isDirectory && recursive)
        perform(config.copy(index = Some(index.parent / f.name / index.name), directory = directory / f.name))
    }
    html.append(
      """</TABLE>
        |</BODY>
        |</HTML>""".stripMargin
    )
    htmlFile.write(html.result().getBytes("UTF-8"))
    htmlFile.close()
  }

  // cf. https://stackoverflow.com/questions/1265282/recommended-method-for-escaping-html-in-java
  def escapeHTML(s: String): String = {
    val out = new StringBuilder(math.max(16, s.length()))
    s.foreach { c =>
      if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
        out.append("&#")
        out.append(c.toInt)
        out.append(';')
      } else {
        out.append(c)
      }
    }
    out.result()
  }

  def getIconName(file: File, fsv: FileSystemView, cache: mutable.Map[String, String], directory: File): String = {
    val icon      = fsv.getSystemIcon(file)
    val typ       = icon.hashCode.toString
    val iconName  = s"$typ.png"
    if (!cache.contains(typ)) {
      val img     = new BufferedImage(icon.getIconWidth, icon.getIconHeight, BufferedImage.TYPE_INT_ARGB)
      val g       = img.createGraphics()
      val c       = new JLabel
      c.setSize(img.getWidth, img.getHeight)
      icon.paintIcon(c, g, 0, 0)
      val iconFile = directory / iconName
      ImageIO.write(img, "png", iconFile)
      cache += typ -> iconName
    }
    iconName
  }

  // compares strings insensitive to case but sensitive to integer numbers
  def compareName(s1: String, s2: String): Int = {
    // this is a quite ugly direct translation from a Java snippet I wrote,
    // could use some scala'fication

    val n1  = s1.length
    val n2  = s2.length
    val min = math.min(n1, n2)

    var i = 0
    while (i < min) {
      var c1 = s1.charAt(i)
      var c2 = s2.charAt(i)
      var d1 = Character.isDigit(c1)
      var d2 = Character.isDigit(c2)

      if (d1 && d2) {
        // Enter numerical comparison
        var c3, c4 = ' '
        do {
          i += 1
          c3 = if (i < n1) s1.charAt(i) else 'x'
          c4 = if (i < n2) s2.charAt(i) else 'x'
          d1 = Character.isDigit(c3)
          d2 = Character.isDigit(c4)
        }
        while (d1 && d2 && c3 == c4)

        if (d1 != d2) return if (d1) 1 else -1
        if (c1 != c2) return c1 - c2
        if (c3 != c4) return c3 - c4
        i -= 1

      }
      else if (c1 != c2) {
        c1 = Character.toUpperCase(c1)
        c2 = Character.toUpperCase(c2)

        if (c1 != c2) {
          c1 = Character.toLowerCase(c1)
          c2 = Character.toLowerCase(c2)

          if (c1 != c2) {
            // No overflow because of numeric promotion
            return c1 - c2
          }
        }
      }

      i += 1
    }
    n1 - n2
  }
}