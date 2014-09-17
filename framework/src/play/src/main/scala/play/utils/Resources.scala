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

  def isDirectory(classLoader: ClassLoader, url: URL) = url.getProtocol match {
    case "file" => new File(url.toURI).isDirectory
    case "jar" => isJarResourceDirectory(url)
    case "bundle" => isBundleResourceDirectory(classLoader, url)
    case _ => throw new IllegalArgumentException(s"Cannot check isDirectory for a URL with protocol='${url.getProtocol}'")
  }

  private def isBundleResourceDirectory(classLoader: ClassLoader, url: URL): Boolean = {
    /* ClassLoader within an OSGi container behave differently than the standard classloader.
     * One difference is how getResource returns when the resource's name end with a slash.
     * In a standard JVM, getResource doesn't care of ending slashes, and return the URL of
     * any existing resources. In an OSGi container (tested with Apache Felix), ending slashe
     * refers to a directory (return null otherwise). */

    val path = url.getPath
    val pathSlash = if (path.last == '/') path else path + '/'

    classLoader.getResource(path) != null && classLoader.getResource(pathSlash) != null
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
