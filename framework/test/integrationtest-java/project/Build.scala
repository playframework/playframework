import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "integrationtest-java"
    val appVersion      = "1.0"

    val appDependencies = Seq(
    	javaCore,
    	javaEbean,
      "org.hamcrest" % "hamcrest-all" % "1.3"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here
      compile in Test <<= PostCompile(Test)
    )

}
