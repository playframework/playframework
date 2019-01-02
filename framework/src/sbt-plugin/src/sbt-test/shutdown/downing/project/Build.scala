/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import play.dev.filewatch.FileWatchService
import play.sbt.run.toLoggerProxy
import sbt._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.sys.process.Process
import scala.util.Properties
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
        // check that there is a process line starting with 'pidString ' 
        // (note padding with whitespace)
        .exists(_.startsWith(pidString + " ")) 
    }

    println("Preparing to stop Prod...")
    verifyResourceContains("/simulate-downing", 200, Seq.empty[String], 3)
    println("Prod is stopping.")

    // Use a polling loop of at most 30sec. Without it, the `scripted-test` moves on
    // before the application has finished to shut down
    val secs = 10
    // NiceToHave: replace with System.nanoTime()
    val end = System.currentTimeMillis() + secs * 1000
    do{
      println(s"Is the PID file deleted already? ${!(Project.extract(state).get(Keys.target) / "universal" / "stage" / "RUNNING_PID").exists()}")
      TimeUnit.SECONDS.sleep(3)
    }while ( processIsRunning(pidString) && System.currentTimeMillis() < end)

    if (processIsRunning(pidString)) {
      throw new RuntimeException(s"Assertion failed: Process $pidString didn't stop in $secs sconds.")
    }

    state
  }


}
