/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.router

import java.io._
import scala.io.Codec

/**
 * Because every project needs its own IO utilities.
 */
private[router] object RouterIO {
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
   * Read the file as a String.
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
      case e: IOException => // Ignore
    }
  }

}
