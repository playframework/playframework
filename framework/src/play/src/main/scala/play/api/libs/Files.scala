/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import java.io._
import play.utils.PlayIO

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
      Files.moveFile(file, to, replace = replace)
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
    PlayIO.copyFile(from, to, replaceExisting)
  }

  /**
   * Rename a file.
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def moveFile(from: File, to: File, replace: Boolean = true): File = {
    PlayIO.moveFile(from, to, replace)
  }

  /**
   * Reads a file’s contents into a String.
   *
   * @param path the file to read.
   * @return the file contents
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def readFile(path: File): String = PlayIO.readFileAsString(path)

  /**
   * Write a file’s contents as a `String`.
   *
   * @param path the file to write to
   * @param content the contents to write
   */
  @deprecated("Use Java 7 Files API instead", "2.3")
  def writeFile(path: File, content: String): Unit = PlayIO.writeStringToFile(path, content)

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
  def writeFileIfChanged(path: File, content: String) {
    if (content != Option(path).filter(_.exists).map(readFile(_)).getOrElse("")) {
      writeFile(path, content)
    }
  }

}