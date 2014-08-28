/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import java.net.{ URI, URL }
import java.io.File
import java.util.zip.ZipFile

/**
 * Provide resources helpers
 */
object Resources {

  def isDirectory(url: URL) = url.getProtocol match {
    case "file" => new File(url.toURI).isDirectory
    case "jar" | "wsjar" => isJarResourceDirectory(url)
    case _ => throw new IllegalArgumentException(s"Cannot check isDirectory for a URL with protocol='${url.getProtocol}'")
  }

  private def isJarResourceDirectory(url: URL): Boolean = {
    val path = url.getPath
    val bangIndex = url.getFile.indexOf("!")

    val jarFile: File = new File(URI.create(path.substring(0, bangIndex)))
    val resourcePath = URI.create(path.substring(bangIndex + 1)).getPath.drop(1)
    val zip = new ZipFile(jarFile)

    try {
      val entry = zip.getEntry(resourcePath)
      if (entry.isDirectory) true
      else {
        val stream = zip.getInputStream(entry)
        val isDir = stream == null
        if (stream != null) stream.close()
        isDir
      }
    } finally {
      zip.close()
    }
  }
}
