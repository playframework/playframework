/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{ Path, Files => JFiles }
import java.util.concurrent.{ CountDownLatch, ExecutorService, Executors }

import org.specs2.mock.Mockito
import org.specs2.mutable.{ After, Specification }
import org.specs2.specification.Scope
import play.api.ApplicationLoader.Context
import play.api._
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.Files._
import play.api.routing.Router

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

class TemporaryFileCreatorSpec extends Specification with Mockito {

  sequential

  val utf8: Charset = Charset.forName("UTF8")

  "DefaultTemporaryFileCreator" should {

    abstract class WithScope extends Scope with After {
      val parentDirectory: Path = {
        val f = JFiles.createTempDirectory(null)
        f.toFile.deleteOnExit()
        f
      }

      override def after: Any = {
        val files = parentDirectory.toFile.listFiles()
        if (files != null) {
          files.foreach(_.delete())
        }

        parentDirectory.toFile.delete()
      }
    }

    "not have a race condition when creating temporary files" in {

      // See issue https://github.com/playframework/playframework/issues/7700
      // We were having problems by creating to many temporary folders and
      // keeping track of them inside TemporaryFileCreator and between it and
      // TemporaryFileReaper.

      val threads = 25
      val threadPool: ExecutorService = Executors.newFixedThreadPool(threads)

      val lifecycle = new DefaultApplicationLifecycle
      val reaper = mock[TemporaryFileReaper]
      val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

      try {
        val executionContext = ExecutionContext.fromExecutorService(threadPool)

        // Use a latch to stall the threads until they are all ready to go, then
        // release them all at once. This maximizes the chance of a race condition
        // being visible.
        val raceLatch = new CountDownLatch(threads)

        val futureResults: Seq[Future[TemporaryFile]] = for (_ <- 0 until threads) yield {
          Future {
            raceLatch.countDown()
            creator.create("foo", "bar")
          }(executionContext)
        }

        val results: Seq[TemporaryFile] = {
          import ExecutionContext.Implicits.global // implicit for Future.sequence
          Await.result(Future.sequence(futureResults), 30.seconds)
        }

        val parentDir = results.head.path.getParent

        // All temporary files should be created at the same directory
        results.forall(_.path.getParent.equals(parentDir)) must beTrue
      } finally {
        threadPool.shutdown()
      }
      ok
    }

    "recreate directory if it is deleted" in new WithScope() {
      val lifecycle = new DefaultApplicationLifecycle
      val reaper = mock[TemporaryFileReaper]
      val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)
      val temporaryFile = creator.create("foo", "bar")
      JFiles.delete(temporaryFile.toPath)
      creator.create("foo", "baz")
      lifecycle.stop()
      success
    }

    "when copying file" in {

      "copy when destination does not exists and replace disabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("copy.txt")
        val destination = parentDirectory.resolve("does-not-exists.txt")

        // Create a source file, but not the destination
        writeFile(file, "file to be copied")

        // do the copy
        creator.create(file).copyTo(destination, replace = false)

        // Both source and destination must exist
        JFiles.exists(file) must beTrue
        JFiles.exists(destination) must beTrue

        // Both must have the same content
        val sourceContent = new String(java.nio.file.Files.readAllBytes(file))
        val destinationContent = new String(java.nio.file.Files.readAllBytes(destination))

        destinationContent must beEqualTo(sourceContent)
      }

      "copy when destination does not exists and replace enabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("copy.txt")
        val destination = parentDirectory.resolve("destination.txt")

        // Create source file only
        writeFile(file, "file to be copied")

        creator.create(file).copyTo(destination, replace = true)

        // Both source and destination must exist
        JFiles.exists(file) must beTrue
        JFiles.exists(destination) must beTrue

        // Both must have the same content
        val sourceContent = new String(java.nio.file.Files.readAllBytes(file))
        val destinationContent = new String(java.nio.file.Files.readAllBytes(destination))

        destinationContent must beEqualTo(sourceContent)
      }

      "copy when destination exists and replace enabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("copy.txt")
        val destination = parentDirectory.resolve("destination.txt")

        // Create both files
        writeFile(file, "file to be copied")
        writeFile(destination, "the destination file")

        creator.create(file).copyTo(destination, replace = true)

        // Both source and destination must exist
        JFiles.exists(file) must beTrue
        JFiles.exists(destination) must beTrue

        // Both must have the same content
        val sourceContent = new String(java.nio.file.Files.readAllBytes(file))
        val destinationContent = new String(java.nio.file.Files.readAllBytes(destination))

        destinationContent must beEqualTo(sourceContent)
      }

      "do not copy when destination exists and replace disabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("do-not-replace.txt")
        val destination = parentDirectory.resolve("already-exists.txt")

        writeFile(file, "file that won't be replaced")
        writeFile(destination, "already exists")

        val to = creator.create(file).copyTo(destination, replace = false)
        new String(java.nio.file.Files.readAllBytes(to.toPath)) must contain("already exists")
      }

      "delete source file has no impact on the destination file" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("move.txt")
        writeFile(file, "file to be moved")

        val destination = parentDirectory.resolve("destination.txt")
        creator.create(file).copyTo(destination, replace = true)

        // File was copied
        JFiles.exists(file) must beTrue
        JFiles.exists(destination) must beTrue

        // When deleting the source file the destination will NOT be delete
        // since they are NOT using the same inode.
        JFiles.delete(file)

        // Only source is gone
        JFiles.exists(file) must beFalse
        JFiles.exists(destination) must beTrue
      }
    }

    "when moving file" in {

      "move when destination does not exists and replace disabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("move.txt")
        val destination = parentDirectory.resolve("does-not-exists.txt")

        // Create a source file, but not the destination
        writeFile(file, "file to be moved")

        // move the file
        creator.create(file).moveTo(destination, replace = false)

        JFiles.exists(file) must beFalse
        JFiles.exists(destination) must beTrue

        val destinationContent = new String(java.nio.file.Files.readAllBytes(destination))
        destinationContent must beEqualTo("file to be moved")
      }

      "move when destination does not exists and replace enabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("move.txt")
        val destination = parentDirectory.resolve("destination.txt")

        // Create source file only
        writeFile(file, "file to be moved")

        creator.create(file).moveTo(destination, replace = true)

        JFiles.exists(file) must beFalse
        JFiles.exists(destination) must beTrue

        val destinationContent = new String(java.nio.file.Files.readAllBytes(destination))
        destinationContent must beEqualTo("file to be moved")
      }

      "move when destination exists and replace enabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("move.txt")
        val destination = parentDirectory.resolve("destination.txt")

        // Create both files
        writeFile(file, "file to be moved")
        writeFile(destination, "the destination file")

        creator.create(file).moveTo(destination, replace = true)

        JFiles.exists(file) must beFalse
        JFiles.exists(destination) must beTrue

        val destinationContent = new String(java.nio.file.Files.readAllBytes(destination))
        destinationContent must beEqualTo("file to be moved")
      }

      "do not move when destination exists and replace disabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("do-not-replace.txt")
        val destination = parentDirectory.resolve("already-exists.txt")

        writeFile(file, "file that won't be replaced")
        writeFile(destination, "already exists")

        val to = creator.create(file).moveTo(destination, replace = false)
        new String(java.nio.file.Files.readAllBytes(to.toPath)) must contain("already exists")
      }

      "move a file atomically with replace enabled" in new WithScope() {
        val lifecycle = new DefaultApplicationLifecycle
        val reaper = mock[TemporaryFileReaper]
        val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

        val file = parentDirectory.resolve("move.txt")
        writeFile(file, "file to be moved")

        val destination = parentDirectory.resolve("destination.txt")
        creator.create(file).atomicMoveWithFallback(destination)

        JFiles.exists(file) must beFalse
        JFiles.exists(destination) must beTrue
      }
    }

    "works when using compile time dependency injection" in {
      val context = ApplicationLoader.Context.create(
        new Environment(new File("."), ApplicationLoader.getClass.getClassLoader, Mode.Test))
      val appLoader = new ApplicationLoader {
        def load(context: Context) = {
          new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
            lazy val router = Router.empty
          }.application
        }
      }
      val app = appLoader.load(context)
      Play.start(app)
      val tempFile = try {
        val tempFileCreator = app.injector.instanceOf[TemporaryFileCreator]
        val tempFile = tempFileCreator.create()
        tempFile.exists must beTrue
        tempFile
      } finally {
        Play.stop(app)
      }
      tempFile.exists must beFalse
    }
  }

  private def writeFile(file: Path, content: String) = {
    if (JFiles.exists(file)) JFiles.delete(file)

    JFiles.createDirectories(file.getParent)
    java.nio.file.Files.write(file, content.getBytes(utf8))
  }

}
