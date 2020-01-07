/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.io.File
import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.{ Files => JFiles }

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import play.api.libs.Files.TemporaryFile
import play.api.libs.Files.TemporaryFileCreator

import scala.util.Try

class InMemoryTemporaryFile(val path: Path, val temporaryFileCreator: TemporaryFileCreator) extends TemporaryFile {
  def file: File = path.toFile
}

class InMemoryTemporaryFileCreator(totalSpace: Long) extends TemporaryFileCreator {
  private val fsConfig: Configuration = Configuration.unix.toBuilder
    .setMaxSize(totalSpace)
    .build()
  private val fs: FileSystem       = Jimfs.newFileSystem(fsConfig)
  private val playTempFolder: Path = fs.getPath("/tmp")

  def create(prefix: String = "", suffix: String = ""): TemporaryFile = {
    JFiles.createDirectories(playTempFolder)
    val tempFile = JFiles.createTempFile(playTempFolder, prefix, suffix)
    new InMemoryTemporaryFile(tempFile, this)
  }

  def create(path: Path): TemporaryFile = new InMemoryTemporaryFile(path, this)

  def delete(file: TemporaryFile): Try[Boolean] = Try(JFiles.deleteIfExists(file.path))
}
