package play.api.libs

import scalax.io._
import scalax.file._

import java.io._

/**
 * Files utilities.
 */
object Files {

  /**
   * Read a file content as String.
   *
   * @param path The file to read.
   * @return File content as String.
   */
  def readFile(path: File): String = Path(path).slurpString

  /**
   * Write a file content as String.
   *
   * @param path The file to write.
   * @param content The content to write.
   */
  def writeFile(path: File, content: String) = Path(path).write(content)

  /**
   * Create a directory.
   *
   * @param path The directory to create.
   */
  def createDirectory(path: File) = Path(path).createDirectory(failIfExists = false)

  /**
   * Write a file content as String.
   *
   * Only touch the file if the actual file content is different.
   *
   * @param path The file to write.
   * @param content The content to write.
   */
  def writeFileIfChanged(path: File, content: String) {
    if (content != Option(path).filter(_.exists).map(readFile(_)).getOrElse("")) {
      writeFile(path, content)
    }
  }

}