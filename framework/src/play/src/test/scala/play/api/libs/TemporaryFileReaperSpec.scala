/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import java.nio.charset.Charset
import java.nio.file.{ Path, Files => JFiles }
import java.time.{ Clock, Instant, ZoneId }

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.api.libs.Files.{ DefaultTemporaryFileReaper, TemporaryFileReaperConfiguration }

class TemporaryFileReaperSpec(implicit ee: ExecutionEnv) extends Specification with AfterAll {
  sequential

  val utf8 = Charset.forName("UTF8")

  val system = ActorSystem()

  override def afterAll = {
    system.terminate()
  }

  "DefaultTemporaryFileReaper" should {

    "Find an expired file" in {
      import scala.concurrent.duration._
      val parentDirectory: Path = {
        val f = JFiles.createTempDirectory(null)
        f.toFile.deleteOnExit()
        f
      }

      // Start with "enabled = false" so that reaper must be called manually
      val config = TemporaryFileReaperConfiguration(
        enabled = false,
        olderThan = 1.seconds,
        initialDelay = 0 seconds,
        interval = 100 millis)

      val file = parentDirectory.resolve("notcollected.txt")
      writeFile(file, "notcollected")

      val reaper = new DefaultTemporaryFileReaper(system, config) {
        override val clock = Clock.fixed(Instant.now.plusSeconds(60), ZoneId.systemDefault())
      }
      reaper.updateTempFolder(parentDirectory)
      val result = reaper.reap() must contain(file).await

      result
    }

    "Not reap a non-expired file" in {
      import scala.concurrent.duration._
      val parentDirectory: Path = {
        val f = JFiles.createTempDirectory(null)
        f.toFile.deleteOnExit()
        f
      }

      // Start with "enabled = false" so that reaper must be called manually
      val config = TemporaryFileReaperConfiguration(
        enabled = false,
        olderThan = 1.seconds,
        initialDelay = 0 seconds,
        interval = 100 millis)

      val file = parentDirectory.resolve("notcollected.txt")
      writeFile(file, "notcollected")

      val reaper = new DefaultTemporaryFileReaper(system, config) {
        override val clock = Clock.fixed(Instant.now, ZoneId.systemDefault())
      }
      reaper.updateTempFolder(parentDirectory)
      val result = reaper.reap() must beEmpty[Seq[Path]].await

      result
    }

    "Disable the reaper if set in config" in {
      import scala.concurrent.duration._
      val system = ActorSystem()

      val config = TemporaryFileReaperConfiguration(
        enabled = false,
        olderThan = 1.seconds,
        initialDelay = 0 seconds,
        interval = 100 millis)
      val reaper = new DefaultTemporaryFileReaper(system, config) {
        override val clock = Clock.fixed(Instant.now, ZoneId.systemDefault())
      }
      val result = reaper.enabled must be_==(false)

      result
    }

    "Enable the reaper if set in config" in {
      import scala.concurrent.duration._

      val config = TemporaryFileReaperConfiguration(
        enabled = true,
        olderThan = 1.seconds,
        initialDelay = 0 seconds,
        interval = 100 millis)
      val reaper = new DefaultTemporaryFileReaper(system, config) {
        override val clock = Clock.fixed(Instant.now, ZoneId.systemDefault())
      }
      val result = reaper.enabled must be_==(true)

      reaper.disable() // prevent spam messages
      result
    }

  }

  "TemporaryFileReaperConfiguration" should {
    "read configuration successfully" in {
      import scala.concurrent.duration._

      val configuration = play.api.Configuration(ConfigFactory.parseString(
        """
          |play.temporaryFile.reaper {
          |  olderThan = 1 seconds
          |  initialDelay = 42 seconds
          |  interval = 23 seconds
          |  enabled = true
          |}
        """.stripMargin))

      val tfrConfig = TemporaryFileReaperConfiguration.fromConfiguration(configuration)
      tfrConfig.enabled must be_==(true)
      tfrConfig.olderThan must be_==(1.seconds)
      tfrConfig.initialDelay must be_==(42.seconds)
      tfrConfig.interval must be_==(23.seconds)
    }
  }

  private def writeFile(file: Path, content: String) = {
    if (JFiles.exists(file)) JFiles.delete(file)

    JFiles.createDirectories(file.getParent)
    JFiles.write(file, content.getBytes(utf8))
  }
}
