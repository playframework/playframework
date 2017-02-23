/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server

import com.google.common.io.Files
import com.typesafe.config.ConfigException
import java.io.File
import java.nio.charset.Charset
import java.util.Properties
import org.specs2.mutable.Specification
import play.api.{ Mode, Play, PlayException }
import play.core.ApplicationProvider

object ProdServerStartSpec extends Specification {

  def withTempDir[T](block: File => T) = {
    val temp = Files.createTempDir()
    try {
      block(temp)
    } finally {
      def rm(file: File): Unit = file match {
        case dir if dir.isDirectory =>
          dir.listFiles().foreach(rm)
          dir.delete()
        case f => f.delete()
      }
      rm(temp)
    }
  }

  case class ExitException(message: String, cause: Option[Throwable] = None, returnCode: Int = -1) extends Exception(s"Exit with $message, $returnCode", cause.orNull)

  def exitResult[A](f: => A): Either[(String, Option[String]), A] = try Right(f) catch {
    case ExitException(message, cause, _) =>
      val causeMessage: Option[String] = cause.flatMap(c => Option(c.getMessage))
      Left((message, causeMessage))
  }

  /** A mocked ServerProcess */
  class FakeServerProcess(
      val args: Seq[String] = Seq(),
      propertyMap: Map[String, String] = Map(),
      val pid: Option[String] = None) extends ServerProcess {

    val classLoader: ClassLoader = getClass.getClassLoader

    val properties = new Properties()
    for ((k, v) <- propertyMap) { properties.put(k, v) }

    private var hooks = Seq.empty[() => Unit]
    def addShutdownHook(hook: => Unit) = {
      hooks = hooks :+ (() => hook)
    }
    def shutdown(): Unit = {
      for (h <- hooks) h.apply()
    }

    def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing = {
      throw new ExitException(message, cause, returnCode)
    }
  }

  // A family of fake servers for us to test

  class FakeServer(context: ServerProvider.Context) extends Server with ServerWithStop {
    def config = context.config
    def applicationProvider = context.appProvider
    def mode = config.mode
    def mainAddress = ???
    @volatile var stopCallCount = 0
    override def stop() = {
      stopCallCount += 1
      super.stop()
    }
    def httpPort = config.port
    def httpsPort = config.sslPort
  }

  class FakeServerProvider extends ServerProvider {
    override def createServer(context: ServerProvider.Context) = new FakeServer(context)
  }

  "ProdServerStartSpec.start" should {

    "read settings, create custom ServerProvider, create a pid file, start the server and register shutdown hooks" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        propertyMap = Map("play.server.provider" -> classOf[FakeServerProvider].getName),
        pid = Some("999")
      )
      val pidFile = new File(tempDir, "RUNNING_PID")
      pidFile.exists must beFalse
      val server = ProdServerStart.start(process)
      def fakeServer: FakeServer = server.asInstanceOf[FakeServer]
      try {
        server.getClass must_== classOf[FakeServer]
        pidFile.exists must beTrue
        fakeServer.stopCallCount must_== 0
        fakeServer.httpPort must_== Some(9000)
        fakeServer.httpsPort must_== None
      } finally {
        process.shutdown()
      }
      pidFile.exists must beFalse
      fakeServer.stopCallCount must_== 1
    }

    "read configuration for ports" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        propertyMap = Map(
          "play.server.provider" -> classOf[FakeServerProvider].getName,
          "play.server.http.port" -> "disabled",
          "play.server.https.port" -> "443",
          "play.server.http.address" -> "localhost"
        ),
        pid = Some("123")
      )
      val pidFile = new File(tempDir, "RUNNING_PID")
      pidFile.exists must beFalse
      val server = ProdServerStart.start(process)
      def fakeServer: FakeServer = server.asInstanceOf[FakeServer]
      try {
        server.getClass must_== classOf[FakeServer]
        pidFile.exists must beTrue
        fakeServer.stopCallCount must_== 0
        fakeServer.config.port must_== None
        fakeServer.config.sslPort must_== Some(443)
        fakeServer.config.address must_== "localhost"
      } finally {
        process.shutdown()
      }
      pidFile.exists must beFalse
      fakeServer.stopCallCount must_== 1
    }

    "read configuration for disabled https port" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        propertyMap = Map(
          "play.server.provider" -> classOf[FakeServerProvider].getName,
          "play.server.http.port" -> "80",
          "play.server.https.port" -> "disabled",
          "play.server.http.address" -> "localhost"
        ),
        pid = Some("123")
      )
      val pidFile = new File(tempDir, "RUNNING_PID")
      pidFile.exists must beFalse
      val server = ProdServerStart.start(process)
      def fakeServer: FakeServer = server.asInstanceOf[FakeServer]
      try {
        server.getClass must_== classOf[FakeServer]
        pidFile.exists must beTrue
        fakeServer.stopCallCount must_== 0
        fakeServer.config.port must_== Some(80)
        fakeServer.config.sslPort must_== None
        fakeServer.config.address must_== "localhost"
      } finally {
        process.shutdown()
      }
      pidFile.exists must beFalse
      fakeServer.stopCallCount must_== 1
    }

    "exit with an error if no root dir defined" in withTempDir { tempDir =>
      val process = new FakeServerProcess()
      exitResult {
        ProdServerStart.start(process)
      } must beLeft
    }

  }

}
