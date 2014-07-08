/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import com.google.common.io.Files
import java.io.File
import java.nio.charset.Charset
import java.util.Properties
import org.specs2.mutable.Specification
import play.api.{ Mode, Play }
import play.core.ApplicationProvider

object ServerStartSpec extends Specification {

  sequential

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

  case class ExitException(message: String, cause: Option[Throwable] = None, returnCode: Int = -1) extends Exception(s"Exit with $message, $cause, $returnCode", cause.orNull)

  def startResult[A](f: => A): Either[String,A] = try Right(f) catch {
    case ServerStartException(message, _) => Left(message)
  }

  def exitResult[A](f: => A): Either[String,A] = try Right(f) catch {
    case ExitException(message, _, _) => Left(message)
  }

  /** A mocked ServerProcess */
  class FakeServerProcess(
    val args: Seq[String] = Seq(),
    propertyMap: Map[String,String] = Map(),
    val pid: Option[String] = None
  ) extends ServerProcess {

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

  class FakeApplicationProvider(appPath: File) extends ApplicationProvider {
    val path = appPath
    def get = ??? // Never called, because we're not serving requests
  }

  // A family of fake servers for us to test

  class FakeServer(config: ServerConfig, appProvider: ApplicationProvider) extends Server with ServerWithStop {
    def applicationProvider = appProvider
    def mode = config.mode
    def mainAddress = ???
    @volatile var stopCallCount = 0
    override def stop() = {
      stopCallCount += 1
      Play.stop()
      super.stop()
    }
  }

  class FakeServerProvider extends ServerProvider {
    override def createServer(config: ServerConfig, appProvider: ApplicationProvider) = new FakeServer(config, appProvider)
  }

  class FakeServer2(config: ServerConfig, appProvider: ApplicationProvider) extends FakeServer(config, appProvider)

  class FakeServerProvider2 extends ServerProvider {
    override def createServer(config: ServerConfig, appProvider: ApplicationProvider) = new FakeServer2(config, appProvider)
  }

  class InvalidCtorFakeServerProvider(foo: String) extends FakeServerProvider {
    override def createServer(config: ServerConfig, appProvider: ApplicationProvider) = ???
  }

  class PrivateCtorFakeServer private () extends FakeServerProvider {
    override def createServer(config: ServerConfig, appProvider: ApplicationProvider) = ???
  }

  "ServerStart.readServerConfigSettings" should {

    "read settings from the current process (root dir in args, default HTTP port)" in withTempDir { tempDir =>
      val process = new FakeServerProcess(Seq(tempDir.getAbsolutePath))
      startResult(ServerStart.readServerConfigSettings(process)) must_== Right(ServerConfig(
        rootDir = tempDir,
        port = Some(9000),
        sslPort = None,
        mode = Mode.Prod,
        properties = process.properties
      ))
    }
    "read settings from the current process (root dir, HTTP port and HTTPS port in props)" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        propertyMap = Map(
          "user.dir" -> tempDir.getAbsolutePath,
          "http.port" -> "80",
          "https.port" -> "443"
        )
      )
      startResult(ServerStart.readServerConfigSettings(process)) must_== Right(ServerConfig(
        rootDir = tempDir,
        port = Some(80),
        sslPort = Some(443),
        mode = Mode.Prod,
        properties = process.properties
      ))
    }
    "require a root dir path when reading settings" in {
      val process = new FakeServerProcess()
      startResult(ServerStart.readServerConfigSettings(process)) must_== Left("No root server path supplied")
    }
    "require an HTTP or HTTPS port when reading settings" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        propertyMap = Map("http.port" -> "disabled")
      )
      startResult(ServerStart.readServerConfigSettings(process)) must_== Left("Must provide either an HTTP or HTTPS port")
    }
    "require an integer HTTP port when reading settings" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        propertyMap = Map("http.port" -> "xyz")
      )
      startResult(ServerStart.readServerConfigSettings(process)) must_== Left("Invalid HTTP port: xyz")
    }
    "require an integer HTTPS port when reading settings" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        propertyMap = Map("https.port" -> "xyz")
      )
      startResult(ServerStart.readServerConfigSettings(process)) must_== Left("Invalid HTTPS port: xyz")
    }

  }

  "ServerStart.readServerProviderSetting" should {

    "return None by default" in {
      val process = new FakeServerProcess()
      ServerStart.readServerProviderSetting(process) must beNone
    }
    "create a custom provider when the server.provider property is supplied" in {
      val serverProviderClass = classOf[FakeServerProvider]
      val process = new FakeServerProcess(
        propertyMap = Map("server.provider" -> serverProviderClass.getName)
      )
      ServerStart.readServerProviderSetting(process).map(_.getClass) must_== Some(serverProviderClass)
    }
    "fail if the class doesn't exist" in {
      val process = new FakeServerProcess(
        propertyMap = Map("server.provider" -> "garble.barble.Phnarble")
      )
      startResult(ServerStart.readServerProviderSetting(process)) must_== Left("Couldn't find ServerProvider class 'garble.barble.Phnarble'")
    }
    "fail if the class doesn't implement ServerProvider" in {
      val serverProvider = classOf[String].getName
      val process = new FakeServerProcess(
        propertyMap = Map("server.provider" -> serverProvider)
      )
      startResult(ServerStart.readServerProviderSetting(process)) must_== Left(s"Class $serverProvider must implement ServerProvider interface")
    }
    "fail if the class doesn't have a default constructor" in {
      val serverProvider = classOf[InvalidCtorFakeServerProvider].getName
      val process = new FakeServerProcess(
        propertyMap = Map("server.provider" -> serverProvider)
      )
      startResult(ServerStart.readServerProviderSetting(process)) must_== Left(s"ServerProvider class $serverProvider must have a public default constructor")
    }
    "fail if the class has a private constructor" in {
      val serverProvider = classOf[PrivateCtorFakeServer].getName
      val process = new FakeServerProcess(
        propertyMap = Map("server.provider" -> serverProvider)
      )
      startResult(ServerStart.readServerProviderSetting(process)) must_== Left(s"ServerProvider class $serverProvider must have a public default constructor")
    }

  }


  "ServerStart.createPidFile" should {

    "create a pid file with the current id, then remove it on process shutdown" in withTempDir { tempDir =>
      val pid = "12345"
      val process = new FakeServerProcess(pid = Some(pid))
      startResult(ServerStart.createPidFile(process, tempDir)) must_== Right(())
      val pidFile = new File(tempDir, "RUNNING_PID")
      try {
        pidFile.exists must beTrue
        Files.toString(pidFile, Charset.forName("US-ASCII")) must_== pid
      } finally {
        process.shutdown()
      }
      pidFile.exists must beFalse
   }
    "fail to create a pid file if it can't get the process pid" in withTempDir { tempDir =>
      val process = new FakeServerProcess(pid = None)
      startResult(ServerStart.createPidFile(process, tempDir)) must_== Left("Couldn't determine current process's pid")
    }
    "fail to create a pid file if the pid file already exists" in withTempDir { tempDir =>
      val process = new FakeServerProcess(pid = Some("123"))
      Files.write("x".getBytes, new File(tempDir, "RUNNING_PID"))
      startResult(ServerStart.createPidFile(process, tempDir)) must_== Left(s"This application is already running (Or delete ${tempDir.getAbsolutePath}/RUNNING_PID file).")
    }

  }

  "ServerStart.start" should {

    "read settings, create a pid file, start the the server and register shutdown hooks" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        pid = Some("999")
      )
      val server = ServerStart.start(process, new FakeServerProvider, (config: ServerConfig) => new FakeApplicationProvider(config.rootDir))
      val pidFile = new File(tempDir, "RUNNING_PID")
      try {
        server.getClass must_== classOf[FakeServer]
        pidFile.exists must beTrue
        server.asInstanceOf[FakeServer].stopCallCount must_== 0
      } finally {
        process.shutdown()
      }
      pidFile.exists must beFalse
      server.asInstanceOf[FakeServer].stopCallCount must_== 1
    }

    "read settings, create custom ServerProvider, create a pid file, start the the server and register shutdown hooks" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        propertyMap = Map("server.provider" -> classOf[FakeServerProvider2].getName),
        pid = Some("999")
      )
      val server = ServerStart.start(process, new FakeServerProvider, (config: ServerConfig) => new FakeApplicationProvider(config.rootDir))
      val pidFile = new File(tempDir, "RUNNING_PID")
      try {
        server.getClass must_== classOf[FakeServer2]
        pidFile.exists must beTrue
        server.asInstanceOf[FakeServer2].stopCallCount must_== 0
      } finally {
        process.shutdown()
      }
      pidFile.exists must beFalse
      server.asInstanceOf[FakeServer2].stopCallCount must_== 1
    }

    "exit with an error if settings are wrong" in withTempDir { tempDir =>
      val process = new FakeServerProcess()
      exitResult {
        ServerStart.start(process, new FakeServerProvider, (config: ServerConfig) => new FakeApplicationProvider(config.rootDir))
      } must_== (Left("No root server path supplied"))
    }

  }

}