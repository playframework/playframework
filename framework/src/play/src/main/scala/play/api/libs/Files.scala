/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

import java.io.{ File, IOException }
import java.lang.ref.Reference
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{ Files => JFiles, _ }
import java.time.{ Clock, Instant }
import java.util.function.Predicate
import javax.inject.{ Inject, Provider, Singleton }

import akka.actor.{ ActorSystem, Cancellable }
import com.google.common.base.{ FinalizablePhantomReference, FinalizableReferenceQueue }
import com.google.common.collect.Sets
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.language.implicitConversions
import scala.util.{ Failure, Try }
import scala.concurrent.duration._

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
    /**
     * Creates a temporary file.
     *
     * @param prefix the prefix of the file.
     * @param suffix the suffix of the file
     * @return the newly created temporary file.
     */
    def create(prefix: String = "", suffix: String = ""): TemporaryFile

    /**
     * Creates a temporary file from an already existing file.
     *
     * @param path the existing temp file path
     * @return a Temporary file wrapping the existing file.
     */
    def create(path: Path): TemporaryFile

    /**
     * Deletes the temporary file.
     *
     * @param file the temporary file to be deleted.
     * @return the boolean value of the FS delete operation, or an throwable.
     */
    def delete(file: TemporaryFile): Try[Boolean]

    /**
     * @return the Java version for the temporary file creator.
     */
    def asJava: play.libs.Files.TemporaryFileCreator = new play.libs.Files.DelegateTemporaryFileCreator(this)
  }

  trait TemporaryFile {
    def path: Path

    @deprecated("Use path rather than file", "2.6.0")
    def file: java.io.File

    def temporaryFileCreator: TemporaryFileCreator

    /**
     * Move the file using a [[java.io.File]].
     *
     * @param to the path to the destination file
     * @param replace true if an existing file should be replaced, false otherwise.
     */
    def moveTo(to: java.io.File, replace: Boolean = false): TemporaryFile = {
      moveTo(to.toPath, replace)
    }

    /**
     * Move the file using a [[java.nio.file.Path]].
     *
     * @param to the path to the destination file
     * @param replace true if an existing file should be replaced, false otherwise.
     */
    def moveTo(to: Path, replace: Boolean): TemporaryFile = {
      try {
        if (replace)
          JFiles.move(path, to, StandardCopyOption.REPLACE_EXISTING)
        else
          JFiles.move(path, to)
      } catch {
        case ex: FileAlreadyExistsException => to
      }

      temporaryFileCreator.create(to)
    }
  }

  /**
   * Creates temporary folders inside a single temporary folder. deleting all files on a
   * successful application stop.  Note that this will not clean up the filesystem if the
   * application / JVM terminates abnormally.
   */
  @Singleton
  class DefaultTemporaryFileCreator @Inject() (
    applicationLifecycle: ApplicationLifecycle,
    temporaryFileReaper: TemporaryFileReaper)
      extends TemporaryFileCreator {

    private val logger = play.api.Logger(this.getClass)
    private val frq = new FinalizableReferenceQueue()

    // Much of the PhantomReference implementation is taken from
    // the Google Guava documentation example
    //
    // https://google.github.io/guava/releases/19.0/api/docs/com/google/common/base/FinalizableReferenceQueue.html
    // Keeping references ensures that the FinalizablePhantomReference itself is not garbage-collected.
    private val references = Sets.newConcurrentHashSet[Reference[TemporaryFile]]()
    private var _playTempFolder: Option[Path] = None

    override def create(prefix: String, suffix: String): TemporaryFile = {
      JFiles.createDirectories(playTempFolder)
      val tempFile = JFiles.createTempFile(playTempFolder, prefix, suffix)
      createReference(new DefaultTemporaryFile(tempFile, this))
    }

    private def playTempFolder: Path = _playTempFolder match {
      // We may need to recreate the file if it was deleted (e.g. by tmpwatch)
      case Some(folder) if JFiles.exists(folder) =>
        temporaryFileReaper.updateTempFolder(folder)
        folder
      case _ =>
        val folder = JFiles.createTempDirectory("playtemp")
        _playTempFolder = Some(folder)
        temporaryFileReaper.updateTempFolder(folder)
        folder
    }

    override def create(path: Path): TemporaryFile = {
      createReference(new DefaultTemporaryFile(path, this))
    }

    private def createReference(tempFile: TemporaryFile) = {
      val reference = new FinalizablePhantomReference[TemporaryFile](tempFile, frq) {
        override def finalizeReferent(): Unit = {
          references.remove(this)
          val path = tempFile.path
          delete(tempFile)
        }
      }
      references.add(reference)
      tempFile
    }

    override def delete(tempFile: TemporaryFile): Try[Boolean] = {
      deletePath(tempFile.path)
    }

    private def deletePath(path: Path): Try[Boolean] = {
      logger.debug(s"deletePath: deleting = $path")
      Try(JFiles.deleteIfExists(path)).recoverWith {
        case e: Exception =>
          logger.error(s"Cannot delete $path", e)
          Failure(e)
      }
    }

    /**
     * A temporary file hold a reference to a real path, and will delete
     * it when the reference is garbage collected.
     */
    class DefaultTemporaryFile private[DefaultTemporaryFileCreator] (
        val path: Path,
        val temporaryFileCreator: TemporaryFileCreator) extends TemporaryFile {
      def file: File = path.toFile
    }

    /**
     * Application stop hook which deletes the temporary folder recursively (including subfolders).
     */
    applicationLifecycle.addStopHook { () =>
      Future.successful(JFiles.walkFileTree(playTempFolder, new SimpleFileVisitor[Path] {
        override def visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult = {
          logger.debug(s"stopHook: Removing leftover temporary file $path from $playTempFolder")
          deletePath(path)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(path: Path, exc: IOException): FileVisitResult = {
          deletePath(path)
          FileVisitResult.CONTINUE
        }
      }))
    }
  }

  trait TemporaryFileReaper {
    def updateTempFolder(folder: Path): Unit
  }

  @Singleton
  class DefaultTemporaryFileReaper @Inject() (
    actorSystem: ActorSystem,
    config: TemporaryFileReaperConfiguration)
      extends TemporaryFileReaper {

    private val logger = play.api.Logger(this.getClass)
    private val blockingDispatcherName = "play.akka.blockingIoDispatcher"
    private val blockingExecutionContext = actorSystem.dispatchers.lookup(blockingDispatcherName)
    private var playTempFolder: Option[Path] = None
    private var cancellable: Option[Cancellable] = None

    // Use an overridable clock here so we can swap it out for testing.
    val clock: Clock = Clock.systemUTC()

    // Check that the reaper got a successful reference to the scheduler task
    def enabled: Boolean = cancellable.nonEmpty

    override def updateTempFolder(folder: Path): Unit = {
      playTempFolder = Option(folder)
    }

    def secondsAgo: Instant = clock.instant().minusSeconds(config.olderThan.toSeconds)

    def reap(): Future[Seq[Path]] = {
      logger.debug(s"reap: reaping old files from $playTempFolder")
      Future {
        playTempFolder.map { f =>
          import scala.compat.java8.StreamConverters._

          val reaped = JFiles.list(f)
            .filter(new Predicate[Path]() {
              override def test(p: Path): Boolean = {
                val lastModifiedTime = JFiles.getLastModifiedTime(p).toInstant
                lastModifiedTime.isBefore(secondsAgo)
              }
            }).toScala[List]

          reaped.foreach { p =>
            delete(p)
          }
          reaped
        }.getOrElse(Seq.empty)
      }(blockingExecutionContext)
    }

    def delete(path: Path): Unit = {
      logger.debug(s"delete: deleting $path")
      try JFiles.deleteIfExists(path) catch {
        case e: Exception =>
          logger.error(s"Cannot delete $path", e)
      }
    }

    private[play] def disable(): Unit = {
      cancellable.foreach(_.cancel())
    }

    if (config.enabled) {
      import config._
      logger.info(s"Reaper enabled on $playTempFolder, starting in $initialDelay with $interval intervals")
      cancellable = Some(actorSystem.scheduler.schedule(initialDelay, interval){
        reap()
      }(actorSystem.dispatcher))
    }
  }

  /**
   * Configuration for the TemporaryFileReaper.
   *
   * @param enabled true if the reaper is enabled, false otherwise.  Default is false.
   * @param olderThan the period after which the file is considered old. Default 5 minutes.
   * @param initialDelay the initial delay after application start when the reaper first run.  Default 5 minutes.
   * @param interval the duration after the initial run during which the reaper will scan for files it can remove.  Default 5 minutes.
   */
  case class TemporaryFileReaperConfiguration(
    enabled: Boolean = false,
    olderThan: FiniteDuration = 5.minutes,
    initialDelay: FiniteDuration = 5.minutes,
    interval: FiniteDuration = 5.minutes)

  object TemporaryFileReaperConfiguration {
    def fromConfiguration(config: Configuration): TemporaryFileReaperConfiguration = {
      def duration(key: String): FiniteDuration = {
        Duration(config.get[String](key)) match {
          case d: FiniteDuration if d.isFinite() =>
            d
          case _ =>
            throw new IllegalStateException(s"Only finite durations are allowed for $key")
        }
      }

      val enabled = config.get[Boolean]("play.temporaryFile.reaper.enabled")
      val olderThan = duration("play.temporaryFile.reaper.olderThan")
      val initialDelay = duration("play.temporaryFile.reaper.initialDelay")
      val interval = duration("play.temporaryFile.reaper.interval")

      TemporaryFileReaperConfiguration(enabled, olderThan, initialDelay, interval)
    }

    /**
     * For calling from Java.
     */
    def createWithDefaults() = apply()

    @Singleton
    class TemporaryFileReaperConfigurationProvider @Inject() (configuration: Configuration) extends Provider[TemporaryFileReaperConfiguration] {
      lazy val get = fromConfiguration(configuration)
    }
  }

  /**
   * Creates temporary folders using java.nio.file.Files.createTempFile.
   *
   * Files created by this method will not be cleaned up with the application
   * or JVM stops.
   */
  object SingletonTemporaryFileCreator extends TemporaryFileCreator {

    override def create(prefix: String, suffix: String): TemporaryFile = {
      val file = JFiles.createTempFile(prefix, suffix)
      new SingletonTemporaryFile(file, this)
    }

    override def create(path: Path): TemporaryFile = {
      new SingletonTemporaryFile(path, this)
    }

    override def delete(tempFile: TemporaryFile): Try[Boolean] = {
      Try(JFiles.deleteIfExists(tempFile.path))
    }

    class SingletonTemporaryFile private[SingletonTemporaryFileCreator] (
        val path: Path,
        val temporaryFileCreator: TemporaryFileCreator) extends TemporaryFile {
      def file: File = path.toFile
    }

  }

  /**
   * Utilities to manage temporary files.
   */
  object TemporaryFile {

    /**
     * Implicitly converts a [[TemporaryFile]] to a plain old [[java.io.File]].
     */
    implicit def temporaryFileToFile(tempFile: TemporaryFile): java.io.File = tempFile.path.toFile

    /**
     * Implicitly converts a [[TemporaryFile]] to a plain old [[java.nio.file.Path]] instance.
     */
    implicit def temporaryFileToPath(tempFile: TemporaryFile): Path = tempFile.path

    /**
     * Create a new temporary file.
     *
     * Example:
     * {{{
     * val tempFile = TemporaryFile(prefix = "uploaded")
     * }}}
     *
     * @param creator the temporary file creator
     * @param prefix The prefix used for the temporary file name.
     * @param suffix The suffix used for the temporary file name.
     * @return A temporary file instance.
     */
    @deprecated("Use temporaryFileCreator.create", "2.6.0")
    def apply(creator: TemporaryFileCreator, prefix: String = "", suffix: String = ""): TemporaryFile = {
      creator.create(prefix, suffix)
    }
  }

}
