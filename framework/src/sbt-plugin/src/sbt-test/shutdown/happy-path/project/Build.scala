/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import play.dev.filewatch.FileWatchService
import play.sbt.run.toLoggerProxy
import sbt._
import sbt.complete.Parser

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.Properties
import scala.sys.process.Process
import java.nio.file.Files
import java.util.concurrent.TimeUnit

object DevModeBuild {

  lazy val initialFileWatchService = play.dev.filewatch.FileWatchService.polling(500)

  def jdk7WatchService = Def.setting {
    FileWatchService.jdk7(Keys.sLog.value)
  }

  def jnotifyWatchService = Def.setting {
    FileWatchService.jnotify(Keys.target.value)
  }

  // Using 30 max attempts so that we can give more chances to
  // the file watcher service. This is relevant when using the
  // default JDK watch service which does uses polling.
  val MaxAttempts = 30
  val WaitTime = 500l

  val ConnectTimeout = 10000
  val ReadTimeout = 10000

  @tailrec
  def verifyResourceContains(path: String, status: Int, assertions: Seq[String], attempts: Int, headers: (String, String)*): Unit = {
    println(s"Attempt $attempts at $path")
    val messages = ListBuffer.empty[String]
    try {
      val url = new java.net.URL("http://localhost:9000" + path)
      val conn = url.openConnection().asInstanceOf[java.net.HttpURLConnection]
      conn.setConnectTimeout(ConnectTimeout)
      conn.setReadTimeout(ReadTimeout)

      headers.foreach(h => conn.setRequestProperty(h._1, h._2))

      if (status == conn.getResponseCode) {
        messages += s"Resource at $path returned $status as expected"
      } else {
        throw new RuntimeException(s"Resource at $path returned ${conn.getResponseCode} instead of $status")
      }

      val is = if (conn.getResponseCode >= 400) {
        conn.getErrorStream
      } else {
        conn.getInputStream
      }

      // The input stream may be null if there's no body
      val contents = if (is != null) {
        val c = IO.readStream(is)
        is.close()
        c
      } else ""
      conn.disconnect()

      assertions.foreach { assertion =>
        if (contents.contains(assertion)) {
          messages += s"Resource at $path contained $assertion"
        } else {
          throw new RuntimeException(s"Resource at $path didn't contain '$assertion':\n$contents")
        }
      }

      messages.foreach(println)
    } catch {
      case e: Exception =>
        println(s"Got exception: $e")
        if (attempts < MaxAttempts) {
          Thread.sleep(WaitTime)
          verifyResourceContains(path, status, assertions, attempts + 1, headers: _*)
        } else {
          messages.foreach(println)
          println(s"After $attempts attempts:")
          throw e
        }
    }
  }


  val assertProcessIsStopped: Command = Command.args("assertProcessIsStopped", "") { (state: State, args: Seq[String]) =>
    val pidFile = Project.extract(state).get(Keys.target) / "universal" / "stage" / "RUNNING_PID"
    if(!pidFile.exists())
      throw new RuntimeException("RUNNING_PID file not found. Can't assert the process is stopped without knowing the process ID.")

    val pidString = Files.readAllLines(pidFile.getAbsoluteFile.toPath).get(0)

    def processIsRunning(pidString: String): Boolean ={
      val foundProcesses = Process("jps").!! // runs the command and returns the output as a single String.
        .split("\n") // split per line
        .filter{_.contains("ProdServerStart")}
      foundProcesses // filter only the Play processes
        // This assertion is flaky since `11234` contains `123`. TODO: improve matcher
        .exists(_.contains(pidString)) // see if one of them is PID
    }

    println("Preparing to stop Prod...")
    Command2.process("stopProd --no-exit-sbt", state)
    println("Prod is stopping.")
    TimeUnit.SECONDS.sleep(1)
    println(s"Is the PID file deleted already? ${!(Project.extract(state).get(Keys.target) / "universal" / "stage" / "RUNNING_PID").exists()}")

    // Use a polling loop of at most 30sec. Without it, the `scripted-test` moves on
    // before the application has finished to shut down
    val secs = 10
    // NiceToHave: replace with System.nanoTime()
    val end = System.currentTimeMillis() + secs * 1000
    while ( processIsRunning(pidString) && System.currentTimeMillis() < end) {
      TimeUnit.SECONDS.sleep(3)
    }
    if (processIsRunning(pidString)) {
      throw new RuntimeException(s"Assertion failed: Process $pidString didn't stop in $secs sconds.")
    }

    state
  }

  // This is copy/pasted from https://github.com/sbt/sbt/commit/dfbb67e7d6699fd6c131d7259e1d5f72fdb097f6.
  // Command.process was remove in sbt 1.0 and put back on sbt 1.2. For this code to run
  // on sbt 0.13.x, 1.0.x, 1.1.x and 1.2.x I'm copy/pasting here.
  private object Command2 {
    def process(command: String, state: State): State = {
      val parser = Command.combine(state.definedCommands)
      Parser.parse(command, parser(state)) match {
        case Right(s) => s() // apply command.  command side effects happen here
        case Left(errMsg) =>
          state.log error errMsg
          state.fail
      }
    }
  }

}
