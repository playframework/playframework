import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "csrftest-java"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      "play" %% "csrf" % Option(System.getProperty("play.version")).getOrElse("2.0")
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
    )

}
