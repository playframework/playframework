package play

import sbt._
import java.lang.{ ProcessBuilder => JProcessBuilder }
import java.io.{ File => JFile, BufferedReader, InputStream, InputStreamReader, OutputStream }
import scala.Console.{ GREEN, RESET }

/**
 * based on https://github.com/typesafehub/sbt-multi-jvm
 */
/**
 * trait PlayJvm {
 *
 * case class RunWith(java: File, scala: ScalaInstance)
 * case class JVMOptions(jvm: Seq[String], scala: (String, Option[String]) => Seq[String])
 * def multiSimpleName(name: String) = name.split("\\.").last
 *
 * def javaCommand(javaHome: Option[File], name: String): File = {
 * val home = javaHome.getOrElse(new File(System.getProperty("java.home")))
 * new File(new File(home, "bin"), name)
 * }
 *
 * def fork(name: String, mainClass: String, args: Option[String], runWith: RunWith, options: JVMOptions, srcDir: JFile, log: Logger) {
 * val logName = "* " + name
 * log.info(if (log.ansiCodesSupported) GREEN + logName + RESET else logName)
 * val process = {
 * val jvmLogger = new JvmLogger("info")
 * val scalaOptions = options.scala(mainClass, args)
 * log.debug("Starting %s for %t" format (name, mainClass))
 * log.debug("  with JVM options: %s" format options.jvm.mkString(" "))
 * startJvm(runWith.java, options.jvm, runWith.scala, scalaOptions, jvmLogger, true)
 * }
 * if (process.exitValue > 0) {
 * log.error("JVM Process failed")
 * sys.error(process.exitValue.toString)
 * }
 *
 * }
 *
 * def startJvm(javaBin: JFile, jvmOptions: Seq[String], si: ScalaInstance, scalaOptions: Seq[String], logger: Logger, connectInput: Boolean) = {
 * forkScala(javaBin, jvmOptions, si.jars, scalaOptions, logger, connectInput)
 * }
 *
 * def forkScala(javaBin: JFile, jvmOptions: Seq[String], scalaJars: Iterable[File], arguments: Seq[String], logger: Logger, connectInput: Boolean) = {
 * val scalaClasspath = scalaJars.map(_.getAbsolutePath).mkString(JFile.pathSeparator)
 * val bootClasspath = "-Xbootclasspath/a:" + scalaClasspath
 * val mainScalaClass = "scala.tools.nsc.MainGenericRunner"
 * val options = jvmOptions ++ Seq(bootClasspath, mainScalaClass) ++ arguments
 * forkJava(javaBin, options, logger, connectInput)
 * }
 *
 * def forkJava(javaBin: JFile, options: Seq[String], logger: Logger, connectInput: Boolean) = {
 * val java = javaBin.toString
 * val command = (java :: options.toList).toArray
 * val builder = new JProcessBuilder(command: _*)
 * Process(builder).run(JvmIO(logger, connectInput))
 * }
 * }
 */
final class JvmLogger(name: Option[String] = None) extends BasicLogger {
  def jvm(message: String) = name.map(n => "[%s] %s" format (n, message)).getOrElse(message)

  def log(level: Level.Value, message: => String) = System.out.synchronized {
    System.out.println(jvm(message))
  }

  def trace(t: => Throwable) = System.out.synchronized {
    val traceLevel = getTrace
    if (traceLevel >= 0) System.out.print(StackTrace.trimmed(t, traceLevel))
  }

  def success(message: => String) = log(Level.Info, message)
  def control(event: ControlEvent.Value, message: => String) = log(Level.Info, message)

  def logAll(events: Seq[LogEvent]) = System.out.synchronized { events.foreach(log) }
}

object JvmIO {
  def apply(log: Logger, connectInput: Boolean) =
    new ProcessIO(input(connectInput), processStream(log, Level.Info), processStream(log, Level.Error), (pb: JProcessBuilder) => false)

  final val BufferSize = 8192

  def processStream(log: Logger, level: Level.Value): InputStream => Unit =
    processStream(line => log.log(level, line))

  def processStream(processLine: String => Unit): InputStream => Unit = in => {
    val reader = new BufferedReader(new InputStreamReader(in))
    def process() {
      val line = reader.readLine()
      if (line != null) {
        processLine(line)
        process()
      }
    }
    process()
  }

  def input(connectInput: Boolean): OutputStream => Unit =
    if (connectInput) connectSystemIn else ignoreOutputStream

  def connectSystemIn(out: OutputStream) = transfer(System.in, out)

  def ignoreOutputStream = (out: OutputStream) => ()

  def transfer(in: InputStream, out: OutputStream): Unit = {
    try {
      val buffer = new Array[Byte](BufferSize)
      def read() {
        val byteCount = in.read(buffer)
        if (Thread.interrupted) throw new InterruptedException
        if (byteCount > 0) {
          out.write(buffer, 0, byteCount)
          out.flush()
          read()
        }
      }
      read()
    } catch {
      case _: InterruptedException => ()
    }
  }
}
