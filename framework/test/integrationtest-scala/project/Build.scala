import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "integrationtest-scala"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
