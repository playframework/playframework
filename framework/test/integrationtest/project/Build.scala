import sbt._
import Keys._
object ApplicationBuild extends Build {

    val appName         = "integrationtest"
    val appVersion      = "0.1"

    val appDependencies = Nil 

    val main = PlayProject(appName, appVersion, appDependencies)

}
            
