/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import java.io._
import play.utils.PlayIO
import scala.io.Codec

/**
 * FileSystem utilities.
 */
object Files {

  /**
   * A temporary file hold a reference to a real file, and will delete
   * it when the reference is garbaged.
   */
  case class TemporaryFile(file: File) {

    /**
     * Clean this temporary file now.
     */
    def clean(): Boolean = {
      file.delete()
    }

    /**
     * Move the file.
     */
    def moveTo(to: File, replace: Boolean = false) {
      Files.Deprecated.moveFile(file, to, replace = replace)
    }

    /**
     * Delete this file on garbage collection.
     */
    override def finalize {
      clean()
    }

  }

  /**
   * Utilities to manage temporary files.
   */
  object TemporaryFile {

    /**
     * Create a new temporary file.
     *
     * Example:
     * {{{
     * val tempFile = TemporaryFile(prefix = "uploaded")
     * }}}
     *
     * @param prefix The prefix used for the temporary file name.
     * @param suffix The suffix used for the temporary file name.
     * @return A temporary file instance.
     */
    def apply(prefix: String = "", suffix: String = ""): TemporaryFile = {
      new TemporaryFile(File.createTempFile(prefix, suffix))
    }

  }

  /**
   * Copy a file.
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def copyFile(from: File, to: File, replaceExisting: Boolean = true): File = {
    if (replaceExisting || !to.exists()) {
      val in = new FileInputStream(from).getChannel
      try {
        val out = new FileOutputStream(to).getChannel
        try {
          out.transferFrom(in, 0, in.size())
        } finally {
          PlayIO.closeQuietly(out)
        }
      } finally {
        PlayIO.closeQuietly(in)
      }
    }

    to
  }

  /**
   * Rename a file.
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def moveFile(from: File, to: File, replace: Boolean = true): File = {
    if (to.exists() && replace) {
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

  /**
   * Reads a file’s contents into a String.
   *
   * @param path the file to read.
   * @return the file contents
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def readFile(path: File): String = PlayIO.readFileAsString(path)(Codec.UTF8)

  /**
   * Write a file’s contents as a `String`.
   *
   * @param path the file to write to
   * @param content the contents to write
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def writeFile(path: File, content: String): Unit = {
    path.getParentFile.mkdirs()
    val out = new FileOutputStream(path)
    try {
      val writer = new OutputStreamWriter(out, Codec.UTF8.name)
      try {
        writer.write(content)
      } finally PlayIO.closeQuietly(writer)
    } finally PlayIO.closeQuietly(out)
  }

  /**
   * Creates a directory.
   *
   * @param path the directory to create
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def createDirectory(path: File): File = {
    path.mkdirs()
    path
  }

  /**
   * Writes a file’s content as String, only touching the file if the actual file content is different.
   *
   * @param path the file to write to
   * @param content the contents to write
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def writeFileIfChanged(path: File, content: String): Unit = {
    if (content != Option(path).filter(_.exists).map(readFile(_)).getOrElse("")) {
      writeFile(path, content)
    }
  }

  /**
   * Workaround to suppress deprecation warnings within the Play build.
   * Based on https://issues.scala-lang.org/browse/SI-7934
   */
  @deprecated("", "")
  private[play] class Deprecated {
    def copyFile(from: File, to: File, replaceExisting: Boolean = true): File =
      Files.copyFile(from, to, replaceExisting)

    def moveFile(from: File, to: File, replace: Boolean = true): File =
      Files.moveFile(from, to, replace)

    def readFile(path: File): String =
      Files.readFile(path)

    def writeFile(path: File, content: String): Unit =
      Files.writeFile(path, content)

    def createDirectory(path: File): File =
      Files.createDirectory(path)

    def writeFileIfChanged(path: File, content: String): Unit =
      Files.writeFileIfChanged(path, content)
  }

  private[play] object Deprecated extends Deprecated

}
