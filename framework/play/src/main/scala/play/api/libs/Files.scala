package play.api.libs

import scalax.io._
import scalax.file._

import java.io._

/** File utilities. */
object Files {

  /** Reads a file’s contents into a String.
    *
    * @param path the file to read.
    * @return the file contents
    */
  def readFile(path: File): String = Path(path).slurpString

  /** Write a file’s contents as a `String`.
    *
    * @param path the file to write to
    * @param content the contents to write
    */
  def writeFile(path: File, content: String) = Path(path).write(content)

  /** Creates a directory.
    *
    * @param path the directory to create
    */
  def createDirectory(path: File) = Path(path).createDirectory(failIfExists = false)

  /** Writes a file’s content as String, only touching the file if the actual file content is different.
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