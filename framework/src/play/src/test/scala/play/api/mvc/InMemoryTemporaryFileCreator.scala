package play.api.mvc

import java.io.File
import java.nio.file.{ FileSystem, Path, Files => JFiles }

import com.github.sbridges.ephemeralfs.EphemeralFsFileSystemBuilder
import play.api.libs.Files.{ TemporaryFile, TemporaryFileCreator }

import scala.util.Try

class InMemoryTemporaryFile(val path: Path, val temporaryFileCreator: TemporaryFileCreator) extends TemporaryFile {
  def file: File = path.toFile
}

class InMemoryTemporaryFileCreator(totalSpace: Long) extends TemporaryFileCreator {
  val fs: FileSystem = EphemeralFsFileSystemBuilder
    .unixFs()
    .setTotalSpace(totalSpace)
    .build()
  private val playTempFolder: Path = fs.getPath("/tmp")

  def create(prefix: String = "", suffix: String = ""): TemporaryFile = {
    JFiles.createDirectories(playTempFolder)
    val tempFile = JFiles.createTempFile(playTempFolder, prefix, suffix)
    new InMemoryTemporaryFile(tempFile, this)
  }

  def create(path: Path): TemporaryFile = new InMemoryTemporaryFile(path, this)

  def delete(file: TemporaryFile): Try[Boolean] = Try(JFiles.deleteIfExists(file.path))
}
