/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.internal.utils

import java.io._

import scala.io.Codec
import java.net.URL
import java.nio.file.{ Files, Path }

import akka.annotation.InternalApi
import play.api.Logger

/**
 * INTERNAL API: IO utilities for internal use by Play projects.
 *
 * This should not be necessary by user code and it is an internal API subject to change without following our
 * deprecation policy.
 */
// This is public so that it can be used in Lagom without any hacks or copy-and-paste.
@InternalApi
object PlayIO {

  private val logger = Logger(this.getClass)

  /**
   * Read the given stream into a byte array.
   *
   * Closes the stream.
   */
  private def readStream(stream: InputStream): Array[Byte] = {
    try {
      val buffer = new Array[Byte](8192)
      var len = stream.read(buffer)
      val out = new ByteArrayOutputStream() // Doesn't need closing
      while (len != -1) {
        out.write(buffer, 0, len)
        len = stream.read(buffer)
      }
      out.toByteArray
    } finally closeQuietly(stream)
  }

  /**
   * Read the file into a byte array.
   */
  def readFile(file: Path): Array[Byte] = {
    readStream(Files.newInputStream(file))
  }

  /**
   * Read the given stream into a String.
   *
   * Closes the stream.
   */
  def readStreamAsString(stream: InputStream)(implicit codec: Codec): String = {
    new String(readStream(stream), codec.name)
  }

  /**
   * Read the URL as a String.
   */
  def readUrlAsString(url: URL)(implicit codec: Codec): String = {
    readStreamAsString(url.openStream())
  }

  /**
   * Read the file as a String.
   */
  def readFileAsString(file: Path)(implicit codec: Codec): String = {
    readStreamAsString(Files.newInputStream(file))
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
      case e: IOException => logger.warn("Error closing stream", e)
    }
  }
}
