import sbt._
import Keys._

object ApplicationBuild extends Build {

    val appName         = "todolist-scala-scalaquery"
    val appVersion      = "0.1"

    val appDependencies = Seq(
        "org.scalaquery" % "scalaquery_2.9.0" % "0.9.4"
    )

    val main = PlayProject(appName, appVersion, appDependencies)

}
            
