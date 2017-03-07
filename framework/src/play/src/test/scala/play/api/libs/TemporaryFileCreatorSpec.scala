/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{ FileSystems, Path, StandardWatchEventKinds, WatchEvent, Files => JFiles }

import akka.actor.ActorSystem
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.{ After, Specification }
import org.specs2.specification.Scope
import play.api.ApplicationLoader.Context
import play.api._
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.Files._
import play.api.routing.Router

class TemporaryFileCreatorSpec extends Specification with Mockito {

  sequential

  val utf8 = Charset.forName("UTF8")

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

    "replace file when moving with replace enabled" in new WithScope() {
      val lifecycle = new DefaultApplicationLifecycle
      val reaper = mock[TemporaryFileReaper]
      val creator = new DefaultTemporaryFileCreator(lifecycle, reaper)

      val file = parentDirectory.resolve("move.txt")
      writeFile(file, "file to be moved")

      val destination = parentDirectory.resolve("destination.txt")
      creator.create(file).moveTo(destination, replace = true)

      JFiles.exists(file) must beFalse
      JFiles.exists(destination) must beTrue
    }

    "do not replace file when moving with replace disabled" in new WithScope() {
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

    "works when using compile time dependency injection" in {
      val context = ApplicationLoader.createContext(
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
