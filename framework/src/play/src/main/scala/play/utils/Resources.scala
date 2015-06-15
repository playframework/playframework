/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import java.net.{ JarURLConnection, URLConnection, URI, URL }
import java.io.File
import java.util.zip.ZipFile

import sun.net.www.protocol.file.FileURLConnection

/**
 * Provide resources helpers
 */
object Resources {

  def isDirectory(url: URL) = url.getProtocol match {
    case "file" => new File(url.toURI).isDirectory
    case "jar" => isJarResourceDirectory(url)
    case _ => throw new IllegalArgumentException(s"Cannot check isDirectory for a URL with protocol='${url.getProtocol}'")
  }

  /**
   * Tries to work out whether the given URL connection is a directory or not.
   *
   * Depends on the URL connection type whether it's accurate.  If it's unable to determine whether it's a directory,
   * this returns false.
   */
  def isUrlConnectionADirectory(urlConnection: URLConnection) = urlConnection match {
    case file: FileURLConnection => new File(file.getURL.toURI).isDirectory
    case jar: JarURLConnection =>
      if (jar.getJarEntry.isDirectory) {
        true
      } else {
        // JarEntry.isDirectory is rubbish....
        val is = jar.getJarFile.getInputStream(jar.getJarEntry)
        if (is == null) {
          true
        } else {
          is.close()
          false
        }
      }
    case other => false
  }

  /**
   * Close a URL connection.
   *
   * This works around a JDK bug where if the URL connection is to a JAR file, and the entry is a directory, an NPE is
   * thrown.
   */
  def closeUrlConnection(connection: URLConnection): Unit = {
    connection match {
      case jar: JarURLConnection =>
        if (!jar.getUseCaches) {
          jar.getJarFile.close()
        }
      case other =>
        other.getInputStream.close()
    }
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
