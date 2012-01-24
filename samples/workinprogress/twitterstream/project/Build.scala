import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "twitterstream"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies)
        .settings(defaultScalaSettings:_*)

}
