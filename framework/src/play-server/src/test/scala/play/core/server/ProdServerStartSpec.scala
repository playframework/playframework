/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.Properties
import java.util.concurrent._

import com.google.common.io.{ Files => GFiles }
import org.specs2.mutable.Specification

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

case class ExitException(message: String, cause: Option[Throwable] = None, returnCode: Int = -1) extends Exception(s"Exit with $message, $returnCode", cause.orNull)

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

class FakeServer(context: ServerProvider.Context) extends Server with ReloadableServer {
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

class StartupErrorServerProvider extends ServerProvider {
  override def createServer(context: ServerProvider.Context) = throw new Exception("server fails to start")
}

class ProdServerStartSpec extends Specification {

  def withTempDir[T](block: File => T) = {
    val temp = GFiles.createTempDir()
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

  def exitResult[A](f: => A): Either[(String, Option[String]), A] = try Right(f) catch {
    case ExitException(message, cause, _) =>
      val causeMessage: Option[String] = cause.flatMap(c => Option(c.getMessage))
      Left((message, causeMessage))
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

    "delete the pidfile if server fails to start" in withTempDir { tempDir =>
      val process = new FakeServerProcess(
        args = Seq(tempDir.getAbsolutePath),
        propertyMap = Map("play.server.provider" -> classOf[StartupErrorServerProvider].getName),
        pid = Some("999")
      )
      val pidFile = new File(tempDir, "RUNNING_PID")
      pidFile.exists must beFalse

      def startServer = { ProdServerStart.start(process) }
      startServer must throwA[ExitException]

      pidFile.exists must beFalse
    }

    "not have a race condition when creating a pid file" in withTempDir { tempDir =>

      // This test creates several fake server processes and starts them concurrently,
      // checking whether or not PID file creation behaves properly. The test is
      // not deterministic; it might pass even if there is a bug in the code. In practice,
      // this test does appear to fail every time when there is a bug. Behavior may
      // differ across machines.

      // Number of fake process threads to create.
      val fakeProcessThreads = 25

      // Where the PID file will be created.
      val expectedPidFile = new File(tempDir, "RUNNING_PID")

      // Run the test with one thread per fake process
      val threadPoolService: ExecutorService = Executors.newFixedThreadPool(fakeProcessThreads)
      try {
        val threadPool: ExecutionContext = ExecutionContext.fromExecutorService(threadPoolService)

        // Use a latch to stall the threads until they are all ready to go, then
        // release them all at once. This maximizes the chance of a race condition
        // being visible.
        val raceLatch = new CountDownLatch(fakeProcessThreads)

        // Spin up each thread and collect the result in a future. The boolean
        // results indicate whether or not the process believes it created a PID file.
        val futureResults: Seq[Future[Boolean]] = for (fakePid <- 0 until fakeProcessThreads) yield {
          Future {

            // Create the process and await the latch
            val process = new FakeServerProcess(
              args = Seq(tempDir.getAbsolutePath),
              pid = Some(fakePid.toString)
            )
            val serverConfig: ServerConfig = ProdServerStart.readServerConfigSettings(process)
            raceLatch.countDown()

            // The code to be tested - creating the PID file
            val createPidResult: Try[Option[File]] = Try {
              ProdServerStart.createPidFile(process, serverConfig.configuration)
            }

            // Check the result of creating the PID file
            createPidResult match {
              case Success(None) =>
                ko("createPidFile didn't even try to create a file")
                false
              case Success(Some(createdFile)) =>
                // Check file is written to the right place
                createdFile.exists must beTrue
                createdFile.getAbsolutePath must_== expectedPidFile.getAbsolutePath
                // Check file contains exactly the PID
                val writtenPid: String = new String(Files.readAllBytes(createdFile.toPath()), Charset.forName("UTF-8"))
                writtenPid must_== fakePid.toString
                true
              case Failure(sse: ServerStartException) =>
                // Check the exception when the PID file couldn't be written
                sse.message must contain("application is already running")
                false
              case Failure(e) =>
                throw e
            }
          }(threadPool)
        }

        // Await the result
        val results: Seq[Boolean] = {
          import ExecutionContext.Implicits.global // implicit for Future.sequence
          Await.result(
            Future.sequence(futureResults),
            Duration(30, TimeUnit.SECONDS))
        }

        // Check that at most 1 PID file was created
        val pidFilesCreated: Int = results.filter(identity).size
        pidFilesCreated must_== 1

      } finally threadPoolService.shutdown()
      ok
    }

  }

}
