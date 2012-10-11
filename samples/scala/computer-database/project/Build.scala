import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "computer-database"
    val appVersion      = "1.0"

    val appDependencies = Seq(
    	"play" %% "play-jdbc" % play.core.PlayVersion.current,
    	"play" %% "anorm" % play.core.PlayVersion.current
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
            
