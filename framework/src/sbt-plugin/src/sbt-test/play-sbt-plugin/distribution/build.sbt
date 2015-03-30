name := "dist-sample"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

val checkStartScript = InputKey[Unit]("check-start-script")

checkStartScript := {
  val args = Def.spaceDelimited().parsed
  val startScript = target.value / "universal/stage/bin/dist-sample"
  def startScriptError(contents: String, msg: String) = {
    println("Error in start script, dumping contents:")
    println(contents)
    sys.error(msg)
  }
  val contents = IO.read(startScript)
  if (!contents.contains( """app_mainclass="play.core.server.NettyServer"""")) {
    startScriptError(contents, "Cannot find the declaration of the main class in the script")
  }
  if (args.contains("no-conf")) {
    if (contents.contains("../conf")) {
      startScriptError(contents, "Start script is adding conf directory to the classpath when it shouldn't be")
    }
  } else {
    if (!contents.contains("../conf")) {
      startScriptError(contents, "Start script is not adding conf directory to the classpath when it should be")
    }
  }
}

def retry[B](max: Int = 10, sleep: Long = 500, current: Int = 1)(block: => B): B = {
  try {
    block
  } catch {
    case scala.util.control.NonFatal(e) =>
      if (current == max) {
        throw e
      } else {
        Thread.sleep(sleep)
        retry(max, sleep, current + 1)(block)
      }
  }
}

InputKey[Unit]("check-config") := {
  val expected = Def.spaceDelimited().parsed.head
  import java.net.URL
  val config = retry() {
    IO.readLinesURL(new URL("http://localhost:9000/config")).mkString("\n")
  }
  if (expected != config) {
    sys.error(s"Expected config $expected but got $config")
  }
}