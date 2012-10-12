import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "computer-database"
    val appVersion      = "1.0"

    val appDependencies = Seq(
    	jdbc,
    	anorm
    )

    val main = PlayProject(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
            
