import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "comet-clock"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      "play" %% "play-java" % play.core.PlayVersion.current
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    )

}
