/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{ Files => JFiles }

import org.specs2.mutable.Specification
import org.specs2.specification.After
import play.api.ApplicationLoader.Context
import play.api._
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.Files.{ DefaultTemporaryFileCreator, TemporaryFile }
import play.api.routing.Router
import play.utils.PlayIO

object FilesSpec extends Specification with After {

  val parentDirectory = new File("/tmp/play/specs/")
  val utf8 = Charset.forName("UTF8")

  override def after: Any = {
    parentDirectory.listFiles().foreach(_.delete())
    parentDirectory.delete()
  }

  "Files" should {

    "DefaultTemporaryFileCreator" should {
      "recreate directory if it is deleted" in {
        val lifecycle = new DefaultApplicationLifecycle
        val creator = new DefaultTemporaryFileCreator(lifecycle)
        val file = creator.create("foo", "bar")
        JFiles.delete(file.toPath)
        JFiles.delete(creator.playTempFolder)
        creator.create("foo", "baz")
        lifecycle.stop()
        success
      }
    }

    "Temporary files" should {

      "delete file when cleaning" in {
        val file = new File(parentDirectory, "delete.txt")
        writeFile(file, "file to be delete")

        TemporaryFile(file).clean()
        new File(file.getAbsolutePath).exists() must beFalse
      }

      "replace file when moving with replace enabled" in {
        val file = new File(parentDirectory, "move.txt")
        writeFile(file, "file to be moved")

        val destination = new File(file.getParentFile, "destination.txt")
        TemporaryFile(file).moveTo(destination, replace = true)

        new File(file.getAbsolutePath).exists() must beFalse
        new File(destination.getAbsolutePath).exists() must beTrue
      }

      "do not replace file when moving with replace disabled" in {
        val file = new File(parentDirectory, "do-not-replace.txt")
        val destination = new File(parentDirectory, "already-exists.txt")

        writeFile(file, "file that won't be replaced")
        writeFile(destination, "already exists")

        val to = TemporaryFile(file).moveTo(destination, replace = false)
        new String(java.nio.file.Files.readAllBytes(to.toPath)) must contain("already exists")
      }

      "works when using compile time dependency injection" in {
        val context = ApplicationLoader.createContext(
          new Environment(new java.io.File("."), ApplicationLoader.getClass.getClassLoader, Mode.Test))
        val appLoader = new ApplicationLoader {
          def load(context: Context) = {
            (new BuiltInComponentsFromContext(context) {
              lazy val router = Router.empty
            }).application
          }
        }
        val app = appLoader.load(context)
        Play.start(app)
        val tempFile = try {
          val tempFile = TemporaryFile()
          tempFile.file.exists must beTrue
          tempFile.file
        } finally {
          Play.stop(app)
        }
        tempFile.exists must beFalse
      }

    }

  }

  "PlayIO" should {

    "read file content" in {
      val file = new File(parentDirectory, "file.txt")
      writeFile(file, "file content")

      retry(new String(PlayIO.readFile(file), utf8) must beEqualTo("file content"))
    }

    "read file content as a String" in {
      val file = new File(parentDirectory, "file.txt")
      writeFile(file, "file content")

      retry(PlayIO.readFileAsString(file) must beEqualTo("file content"))
    }

    "read url content as a String" in {
      val file = new File(parentDirectory, "file.txt")
      writeFile(file, "file content")

      val url = file.toURI.toURL

      retry(PlayIO.readUrlAsString(url) must beEqualTo("file content"))
    }
  }

  private def writeFile(file: File, content: String) = {
    if (file.exists()) file.delete()

    file.getParentFile.mkdirs()
    java.nio.file.Files.write(file.toPath, content.getBytes(utf8))
  }

  private def retry[T](block: => T): T = {
    def step(attempt: Int): T = {
      try {
        block
      } catch {
        case t if attempt < 10 =>
          Thread.sleep(10)
          step(attempt + 1)
      }
    }
    step(0)
  }
}
