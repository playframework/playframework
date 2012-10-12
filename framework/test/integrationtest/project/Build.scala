import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "integrationtest"
    val appVersion      = "0.1"

    val appDependencies = Seq(
    	javaJdbc,
    	java,
    	anorm
    ) 

    val main = PlayProject(appName, appVersion, appDependencies)

}
            
