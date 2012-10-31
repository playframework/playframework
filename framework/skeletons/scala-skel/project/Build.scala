import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "%APPLICATION_NAME%"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      jdbc,
      anorm
    )


    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
      
      // Add additional resolvers here like this.
      // resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
    )
}
