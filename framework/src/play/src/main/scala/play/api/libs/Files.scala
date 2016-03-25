/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

import java.io._
import java.nio.file.{ FileAlreadyExistsException, StandardCopyOption, SimpleFileVisitor, Path, FileVisitResult }
import java.nio.file.attribute.BasicFileAttributes

import javax.inject.{ Inject, Singleton }

import play.api.{ Application, Play }
import play.api.inject.ApplicationLifecycle
import java.nio.file.{ Files => JFiles }

import scala.concurrent.Future

/**
 * FileSystem utilities.
 */
object Files {

  /**
   * Logic for creating a temporary file. Users should try to clean up the
   * file themselves, but this TemporaryFileCreator implementation may also
   * try to clean up any leaked files, e.g. when the Application or JVM stops.
   */
  trait TemporaryFileCreator {
    def create(prefix: String, suffix: String): File
  }

  /**
   * Creates temporary folders inside a single temporary folder. The folder
   * is deleted when the application stops.
   */
  @Singleton
  class DefaultTemporaryFileCreator @Inject() (applicationLifecycle: ApplicationLifecycle) extends TemporaryFileCreator {
    private var _playTempFolder: Option[Path] = None

    private[libs] def playTempFolder: Path = _playTempFolder match {
      // We may need to recreate the file if it was deleted (e.g. by tmpwatch)
      case Some(folder) if JFiles.exists(folder) => folder
      case _ =>
        val folder = JFiles.createTempDirectory("playtemp")
        _playTempFolder = Some(folder)
        folder
    }

    /**
     * Application stop hook which deletes the temporary folder recursively (including subfolders).
     */
    applicationLifecycle.addStopHook { () =>
      Future.successful(JFiles.walkFileTree(playTempFolder, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          JFiles.deleteIfExists(file)
          FileVisitResult.CONTINUE
        }
        override def postVisitDirectory(dir: Path, exc: IOException) = {
          JFiles.deleteIfExists(dir)
          FileVisitResult.CONTINUE
        }
      }))
    }

    def create(prefix: String, suffix: String): File = {
      JFiles.createTempFile(playTempFolder, prefix, suffix).toFile
    }
  }

  /**
   * Creates temporary folders using the default JRE method. Files
   * created by this method will not be cleaned up with the application
   * or JVM stops.
   */
  object SingletonTemporaryFileCreator extends TemporaryFileCreator {
    def create(prefix: String, suffix: String): File = {
      JFiles.createTempFile(prefix, suffix).toFile
    }
  }

  /**
   * A temporary file hold a reference to a real file, and will delete
   * it when the reference is garbaged.
   */
  case class TemporaryFile(file: File) {

    /**
     * Clean this temporary file now.
     */
    def clean(): Boolean = {
      JFiles.deleteIfExists(file.toPath)
    }

    /**
     * Move the file.
     */
    def moveTo(to: File, replace: Boolean = false): File = {
      try {
        if (replace)
          JFiles.move(file.toPath, to.toPath, StandardCopyOption.REPLACE_EXISTING)
        else
          JFiles.move(file.toPath, to.toPath)
      } catch {
        case ex: FileAlreadyExistsException => to
      }

      to
    }

    /**
     * Delete this file on garbage collection.
     */
    override def finalize() {
      clean()
    }

  }

  /**
   * Utilities to manage temporary files.
   */
  object TemporaryFile {

    /**
     * Cache the current Application's TemporaryFileCreator
     */
    private val creatorCache = Application.instanceCache[TemporaryFileCreator]

    /**
     * Get the current TemporaryFileCreator - either the injected
     * instance or the SingletonTemporaryFileCreator if no application
     * is currently running.
     */
    private def currentCreator: TemporaryFileCreator = Play.privateMaybeApplication.fold[TemporaryFileCreator](SingletonTemporaryFileCreator)(creatorCache)

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
      TemporaryFile(currentCreator.create(prefix, suffix))
    }

  }
}
