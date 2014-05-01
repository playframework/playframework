/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import java.io._
import scala.io.Codec
import java.net.URL
import play.api.Logger

/**
 * IO utilites for internal use by Play projects.
 *
 * This is intentionally not public API.
 */
private[play] object PlayIO {

  /**
   * Read the given stream into a byte array.
   *
   * Does not close the stream.
   */
  def readStream(stream: InputStream): Array[Byte] = {
    val buffer = new Array[Byte](8192)
    var len = stream.read(buffer)
    val out = new ByteArrayOutputStream()
    while (len != -1) {
      out.write(buffer, 0, len)
      len = stream.read(buffer)
    }
    out.toByteArray
  }

  /**
   * Read the file into a byte array.
   */
  def readFile(file: File): Array[Byte] = {
    val is = new FileInputStream(file)
    try {
      readStream(is)
    } finally {
      closeQuietly(is)
    }
  }

  /**
   * Read the given stream into a String.
   *
   * Does not close the stream.
   */
  def readStreamAsString(stream: InputStream)(implicit codec: Codec): String = {
    new String(readStream(stream), codec.name)
  }

  /**
   * Read the URL as a String.
   */
  def readUrlAsString(url: URL)(implicit codec: Codec): String = {
    val is = url.openStream()
    try {
      readStreamAsString(is)
    } finally {
      closeQuietly(is)
    }
  }

  /**
   * Read the file as a String.
   */
  def readFileAsString(file: File)(implicit codec: Codec): String = {
    val is = new FileInputStream(file)
    try {
      readStreamAsString(is)
    } finally {
      closeQuietly(is)
    }
  }

  /**
   * Write the given String to a file
   */
  def writeStringToFile(file: File, contents: String)(implicit codec: Codec) = {
    file.getParentFile.mkdirs()
    val writer = new OutputStreamWriter(new FileOutputStream(file), codec.name)
    try {
      writer.write(contents)
    } finally {
      closeQuietly(writer)
    }
  }

  /**
   * Close the given closeable quietly.
   *
   * Logs any IOExceptions encountered.
   */
  def closeQuietly(closeable: Closeable) = {
    try {
      if (closeable != null) {
        closeable.close()
      }
    } catch {
      case e: IOException => play.api.Play.logger.warn("Error closing stream", e)
    }
  }

  /**
   * Copy a file from one location to another
   */
  def copyFile(from: File, to: File, replaceExisting: Boolean = true): File = {
    if (replaceExisting || !to.exists()) {
      val in = new FileInputStream(from).getChannel
      try {
        val out = new FileOutputStream(to).getChannel
        try {
          out.transferFrom(in, 0, in.size())
        } finally {
          closeQuietly(out)
        }
      } finally {
        closeQuietly(in)
      }
    }

    to
  }

  /**
   * Move a file from one location to another
   */
  def moveFile(from: File, to: File, replaceExisting: Boolean = true): File = {
    if (to.exists() && replaceExisting) {
      to.delete()
    }

    if (!to.exists()) {
      if (!from.renameTo(to)) {
        copyFile(from, to)
        from.delete()
      }
    }

    to
  }
}
