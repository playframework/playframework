/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt.forkrun

import sbt._
import java.io.File
import java.lang.{ ProcessBuilder => JProcessBuilder, Runtime => JRuntime }
import java.util.concurrent.CountDownLatch

case class PlayForkOptions(
  workingDirectory: File,
  jvmOptions: Seq[String],
  classpath: Seq[File],
  baseDirectory: File,
  configKey: String,
  logLevel: Level.Value,
  logSbtEvents: Boolean)

/**
 * This differs from sbt's fork run mainly in the way that the process is stopped.
 *
 * When the (background job) thread is interrupted, or the sbt process exits while the fork
 * is still running, then the process is stopped by closing its input.
 * This is checked for by the fork run process and allows a graceful shutdown, rather than
 * forcibly terminating the process with `destroy`.
 */
object PlayForkProcess {
  def apply(options: PlayForkOptions, args: Seq[String], log: Logger): Unit = {
    val logProperties = Seq("-Dfork.run.log.level=" + options.logLevel.toString, "-Dfork.run.log.events=" + options.logSbtEvents)
    val jvmOptions = options.jvmOptions ++ logProperties
    val arguments = Seq(options.baseDirectory.getAbsolutePath, options.configKey) ++ args
    run(options.workingDirectory, jvmOptions, options.classpath, "play.forkrun.ForkRun", arguments, log)
  }

  def run(workingDirectory: File, jvmOptions: Seq[String], classpath: Seq[File], mainClass: String, arguments: Seq[String], log: Logger): Unit = {
    val java = (file(sys.props("java.home")) / "bin" / "java").absolutePath
    val (classpathEnv, options) = makeOptions(jvmOptions, classpath, mainClass, arguments)
    val command = (java +: options).toArray
    val builder = new JProcessBuilder(command: _*)
    builder.directory(workingDirectory)
    for (cp <- classpathEnv) builder.environment.put("CLASSPATH", cp)
    val process = builder.start()
    val stopLatch = new CountDownLatch(1)
    val inputThread = spawn { stopLatch.await(); process.getOutputStream.close() }
    val outputThread = spawn { BasicIO.processFully(logLine(log, Level.Info))(process.getInputStream) }
    val errorThread = spawn { BasicIO.processFully(logLine(log, Level.Error))(process.getErrorStream) }
    def stop() = {
      stopLatch.countDown()
      outputThread.join()
      errorThread.join()
      process.exitValue()
    }
    val shutdownHook = newThread { stop() }
    JRuntime.getRuntime.addShutdownHook(shutdownHook)
    try process.waitFor() catch { case _: InterruptedException => stop() }
    try JRuntime.getRuntime.removeShutdownHook(shutdownHook)
    catch { case _: IllegalStateException => } // thrown when already shutting down
  }

  def makeOptions(jvmOptions: Seq[String], classpath: Seq[File], mainClass: String, arguments: Seq[String]): (Option[String], Seq[String]) = {
    val classpathOption = Path.makeString(classpath)
    val options = jvmOptions ++ Seq("-classpath", classpathOption, mainClass) ++ arguments
    // if the options get too long for Windows, put the classpath in an environment variable
    if (optionsTooLong(options)) {
      val otherOptions = jvmOptions ++ Seq(mainClass) ++ arguments
      (Option(classpathOption), otherOptions)
    } else {
      (None, options)
    }
  }

  val isWindows: Boolean = sys.props("os.name").toLowerCase(java.util.Locale.ENGLISH).contains("windows")

  val MaxOptionsLength = 5000

  def optionsTooLong(options: Seq[String]): Boolean = isWindows && (options.mkString(" ").length > MaxOptionsLength)

  val ansiCode = "(?:\\033\\[[0-9;]+m)?"
  val LogLine = s"^${ansiCode}\\[${ansiCode}([a-z]+)${ansiCode}\\] (.*)".r

  // detect log level from output lines and re-log at the same level
  def logLine(logger: Logger, defaultLevel: Level.Value): String => Unit = (line: String) => line match {
    case LogLine(level, message) => logger.log(Level(level).getOrElse(defaultLevel), message)
    case message => logger.log(defaultLevel, message)
  }

  def spawn(f: => Unit): Thread = {
    val thread = newThread(f)
    thread.start()
    thread
  }

  def newThread(f: => Unit): Thread = new Thread(new Runnable { def run(): Unit = f })
}
