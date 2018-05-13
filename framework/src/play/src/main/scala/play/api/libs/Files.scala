/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.{ Failure, Try }

/**
 * FileSystem utilities.
 */
object Files {

  lazy val logger = LoggerFactory.getLogger("play.api.libs.Files")

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
        else if (!to.toFile.exists())
          JFiles.move(path, to)
        else to
      } catch {
        case ex: FileAlreadyExistsException => to
      }

      temporaryFileCreator.create(to)
    }

    /**
     * Attempts to move source to target atomically and falls back to a non-atomic move if it fails.
     *
     * This always tries to replace existent files. Since it is platform dependent if atomic moves replaces
     * existent files or not, considering that it will always replaces, makes the API more predictable.
     *
     * @param to the path to the destination file
     */
    // see https://github.com/apache/kafka/blob/d345d53/clients/src/main/java/org/apache/kafka/common/utils/Utils.java#L608-L626
    def atomicMoveWithFallback(to: Path): TemporaryFile = {
      try {
        JFiles.move(path, to, StandardCopyOption.ATOMIC_MOVE)
      } catch {
        case outer: IOException =>
          try {
            JFiles.move(path, to, StandardCopyOption.REPLACE_EXISTING)
            logger.debug(s"Non-atomic move of $path to $to succeeded after atomic move failed due to ${outer.getMessage}")
          } catch {
            case inner: IOException =>
              inner.addSuppressed(outer)
              throw inner
          }
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

    private val TempDirectoryPrefix = "playtemp"
    private val playTempFolder: Path = {
      val tmpFolder = JFiles.createTempDirectory(TempDirectoryPrefix)
      temporaryFileReaper.updateTempFolder(tmpFolder)
      tmpFolder
    }

    override def create(prefix: String, suffix: String): TemporaryFile = {
      JFiles.createDirectories(playTempFolder)
      val tempFile = JFiles.createTempFile(playTempFolder, prefix, suffix)
      createReference(new DefaultTemporaryFile(tempFile, this))
    }

    override def create(path: Path): TemporaryFile = {
      createReference(new DefaultTemporaryFile(path, this))
    }

    private def createReference(tempFile: TemporaryFile) = {
      val reference = new FinalizablePhantomReference[TemporaryFile](tempFile, frq) {
        override def finalizeReferent(): Unit = {
          references.remove(this)
          val path = tempFile.path
          deletePath(path)
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
          logger.debug(s"stopHook: Removing leftover temporary file $path from ${playTempFolder}")
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

          val directoryStream = JFiles.list(f)

          try {
            val reaped = directoryStream.filter(new Predicate[Path]() {
              override def test(p: Path): Boolean = {
                val lastModifiedTime = JFiles.getLastModifiedTime(p).toInstant
                lastModifiedTime.isBefore(secondsAgo)
              }
            }).toScala[List]

            reaped.foreach(delete)
            reaped
          } finally {
            directoryStream.close()
          }

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
      playTempFolder match {
        case Some(folder) =>
          logger.debug(s"Reaper enabled on $folder, starting in $initialDelay with $interval intervals")
        case None =>
          logger.debug(s"Reaper enabled but no temp folder has been created yet, starting in $initialDelay with $interval intervals")
      }
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
    @deprecated("On JDK8 and earlier, Class.getSimpleName on doubly nested Scala classes throws an exception. Use Files.TemporaryFileReaperConfigurationProvider instead. See https://github.com/scala/bug/issues/2034.", "2.6.14")
    class TemporaryFileReaperConfigurationProvider @Inject() (configuration: Configuration) extends Provider[TemporaryFileReaperConfiguration] {
      lazy val get = fromConfiguration(configuration)
    }
  }

  @Singleton
  class TemporaryFileReaperConfigurationProvider @Inject() (configuration: Configuration) extends Provider[TemporaryFileReaperConfiguration] {
    lazy val get = TemporaryFileReaperConfiguration.fromConfiguration(configuration)
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
