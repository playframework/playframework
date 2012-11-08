import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "integrationtest"
  val appVersion = "0.1"

  val appDependencies = Seq(
    javaJdbc,
    javaCore,
    anorm)

  val distTestSettings = DistTest.makeSettings(appName, appVersion)

  val main = play.Project(appName, appVersion, appDependencies).settings(
                  requireJs += "main.js"
             ).settings(distTestSettings: _*)
}
            
