import sbt._
import Keys._
object ApplicationBuild extends Build {

    val appName         = "integrationtest"
    val appVersion      = "0.1"

    val appDependencies = Seq(
    	"play" %% "play-jdbc" % play.core.PlayVersion.current,
    	"play" %% "play-java" % play.core.PlayVersion.current,
    	"play" %% "anorm" % play.core.PlayVersion.current
    ) 

    val main = PlayProject(appName, appVersion, appDependencies)

}
            
