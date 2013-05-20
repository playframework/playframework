import sbt._
import Keys._
import play.Project._
import sbt.inc.Analysis
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object ApplicationBuild extends Build {

  val logFile = new File(System.getProperty("performance.log"))
  val appName         = "scala-compilation-times"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm
  )

  val timedCompile = Command.single("timed-compile") { (state, name) => 
    val start = System.currentTimeMillis
    try {
      Project.runTask(compile in Compile, state).get._1
    } finally {
      val time = System.currentTimeMillis - start
      val props = new Properties()
      if (logFile.exists) {
        withResourceIgnoringErrors(new FileInputStream(logFile))(props.load)
      }
      props.put(name, time.toString)
      withResourceIgnoringErrors(new FileOutputStream(logFile)) { os =>
        props.store(os, "Performance test run at " + new java.util.Date())
      }
    }
  }

  def withResourceIgnoringErrors[C <: Closeable](closeable: => C)(block: C => Unit) = {
    try {
      val resource = closeable
      try {
        block(resource)
      } finally {
        resource.close()
      }
    } catch {
      case e => e.printStackTrace()
    }
  }

  val main = play.Project(appName, appVersion, appDependencies).settings(
    commands += timedCompile
  )

}
