package play.api.libs

import scalax.io._
import scalax.file._

import java.io._

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
  def copyFile(from: File, to: File, copyAttributes: Boolean = true, replaceExisting: Boolean = true): Path = {
    Path(from).copyTo(target = Path(to), copyAttributes = copyAttributes, replaceExisting = replaceExisting)
  }

  /**
   * Rename a file.
   */
  def moveFile(from: File, to: File, replace: Boolean = true, atomicMove: Boolean = true): Path = {
    Path(from).moveTo(target = Path(to), replace = replace, atomicMove = atomicMove)
  }

  /**
   * Reads a file’s contents into a String.
   *
   * @param path the file to read.
   * @return the file contents
   */
  def readFile(path: File): String = Path(path).string

  /**
   * Write a file’s contents as a `String`.
   *
   * @param path the file to write to
   * @param content the contents to write
   */
  def writeFile(path: File, content: String): Unit = Path(path).write(content)

  /**
   * Creates a directory.
   *
   * @param path the directory to create
   */
  def createDirectory(path: File): Path = Path(path).createDirectory(failIfExists = false)

  /**
   * Writes a file’s content as String, only touching the file if the actual file content is different.
   *
   * @param path the file to write to
   * @param content the contents to write
   */
  def writeFileIfChanged(path: File, content: String) {
    if (content != Option(path).filter(_.exists).map(readFile(_)).getOrElse("")) {
      writeFile(path, content)
    }
  }

}