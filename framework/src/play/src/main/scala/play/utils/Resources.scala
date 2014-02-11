/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import java.net.URL
import java.io.File
import java.util.zip.ZipFile

/**
 * Provide resources helpers
 */
object Resources {

  def isDirectory(url: URL) = url.getProtocol match {
    case "file" => new File(url.getFile).isDirectory
    case "jar" => isJarResourceDirectory(url)
    case _ => throw new IllegalArgumentException(s"Cannot check isDirectory for a URL with protocol='${url.getProtocol}'")
  }

  private def isJarResourceDirectory(url: URL): Boolean = {
    val startIndex = if (url.getFile.startsWith("file:")) 5 else 0
    val bangIndex = url.getFile.indexOf("!")
    val jarFilePath = url.getFile.substring(startIndex, bangIndex)
    val resourcePath = url.getFile.substring(bangIndex + 2)
    val zip = new ZipFile(jarFilePath)

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
