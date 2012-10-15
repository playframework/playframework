import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "forms"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      javaCore
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
