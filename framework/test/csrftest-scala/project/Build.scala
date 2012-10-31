import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "csrftest-scala"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      "play" %% "filters-helpers" % Option(System.getProperty("play.version")).getOrElse("2.0")
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
    )

}
