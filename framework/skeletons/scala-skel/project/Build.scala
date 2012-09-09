import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "%APPLICATION_NAME%"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
      
      // Add additinol resolvers here like this.
      // resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
    )
}
