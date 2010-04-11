import collection.mutable.{HashMap, ListBuffer}
import java.awt.image.BufferedImage
import java.awt.{Component, Toolkit}
import java.io.{IOException, RandomAccessFile, File}
import java.text.DateFormat
import java.util.{Date, Locale}
import javax.imageio.ImageIO
import javax.swing.filechooser.FileSystemView
import javax.swing.{JLabel, JComponent, Icon}

/**
 *    @version 0.10, 11-Apr-10
 */
object FolderToHTML {
   /**
    *    Options
    *    -C    <listed folder> (optional)
    *    -f    <target index file> (optional)
    *    -a    include hidden files
    *    -i    include icons
    */
   def main( args: Array[ String ]) {
      val argsL      = ListBuffer( args: _* )
      val folder     = new File( getStringArg( argsL, "-C" ) getOrElse "" )
      val file       = getStringArg( argsL, "-f" ).map( new File( _ )) getOrElse new File( folder, "index.html" )
      val hidden     = getSwitchArg( argsL, "-a" )
      val useIcons   = getSwitchArg( argsL, "-i" )
//      val recursive  = ...
      if( argsL.nonEmpty ) {
         println( argsL.mkString( "WARNING: Unprocessed args : ", ", ", " !" ))
      }
      try {
         perform( file, folder, hidden, useIcons )
         System.exit( 0 )
      }
      catch { case e => {
         e.printStackTrace()
         System.exit( 1 )
      }}
   }

   def getStringArg( argsL: ListBuffer[ String ], switch: String ) : Option[ String ] = {
      val idx = argsL.indexOf( switch )
      if( (idx >= 0) && (idx + 1 < argsL.size) ) {
         val res = Some( argsL( idx + 1 ))
         argsL.remove( idx, 2 )
         res
      } else None
   }

   def getSwitchArg( argsL: ListBuffer[ String ], switch: String ) : Boolean = {
      val idx = argsL.indexOf( switch )
      if( idx >= 0 ) {
         argsL.remove( idx )
         true
      } else false
   }

//   def createIconFolder( folder: File ) : File = {
//      var test = new File( folder, ".icons" )
//      var cnt  = 0
//      while( test.exists ) {
//         cnt += 1
//         test = new File( folder, ".icons" + cnt )
//      }
//      test
//   }

   def perform( file: File, folder: File, hidden: Boolean, useIcons: Boolean ) {
      val df      = DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US )
      val fsv     = FileSystemView.getFileSystemView
      val files   = ListBuffer( fsv.getFiles( folder, !hidden ): _* )
      if( file.exists && !file.delete ) throw new IOException( "Could not overwrite file '" + file + "'" )
      val images  = new HashMap[ String, String ]
      val htmlFile = new RandomAccessFile( file, "rw" )
      var html    =
"""<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"
   "http://www.w3.org/TR/REC-html40/loose.dtd">
<HTML>
<HEAD>
   <TITLE>Index of """ + folder.getName + """</TITLE>
   <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=utf-8">
	<STYLE TYPE="text/css"><!--
BODY {
   margin:32px;
}
TR {
   vertical-align:center
}
TR.head {
   font-weight:bold;
}
TD {
	font-family:"Lucida Grande","Helvetica","Arial","sans-serif";
	font-size:12px;
   padding-right:20px;
}
TD.size {
    text-align:right;
}
TD.date {
    text-align:center;
}
A {
	text-decoration:none;
}
a:hover {
	text-decoration:underline;
}
A IMG {
   border:none;
   padding-right:4px;
   vertical-align:center;
}
	-->
	</STYLE>
</HEAD>
<BODY>
<TABLE>
<TR CLASS="head"><TD CLASS="name">Name</TD><TD CLASS="size">Size</TD><TD CLASS="date">Last modified</TD></TR>
"""
      val iconFolder = new File( folder, "_icons" )
      if( useIcons ) iconFolder.mkdir
      files --= List( iconFolder, file )
      files.foreach( f => {
         val img  = if( useIcons ) iconFolder.getName + File.separator + getIconName( f, fsv, images, iconFolder ) else ""
         val name = f.getName
         val size = if( f.isFile ) {
            val bytes = f.length
            (bytes / 1024).toString + " KB"
         } else {
            ""
         }
         val date = df.format( new Date( f.lastModified ))
         html += (<TR><TD CLASS="name"><A HREF={name}>{if( useIcons ) <IMG SRC={img} ALT="icon"/> else xml.Null}{name}</A></TD><TD CLASS="size">{size}</TD><TD CLASS="date">{date}</TD></TR>).toString + "\n"
      })
      html +=
"""</TABLE>
</BODY>
</HTML>"""
      htmlFile.write( html.getBytes( "UTF-8" ))
      htmlFile.close()
   }

   def getIconName( file: File, fsv: FileSystemView, map: HashMap[ String, String ], folder: File ) = {
//      val typ  = fsv.getSystemTypeDescription( file )
      val icon = fsv.getSystemIcon( file )
      val typ  = icon.hashCode.toString
      val iconName   = typ + ".png"
      if( !map.contains( typ )) {
         val img  = new BufferedImage( icon.getIconWidth, icon.getIconHeight, BufferedImage.TYPE_INT_ARGB )
         val g    = img.createGraphics
         val c    = new JLabel
         c.setSize( img.getWidth, img.getHeight )
         icon.paintIcon( c, g, 0, 0 )
         val iconFile   = new File( folder, iconName )
         ImageIO.write( img, "png", iconFile )
         map += typ -> iconName
      }
      iconName
   }
}