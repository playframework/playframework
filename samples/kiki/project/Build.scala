import sbt._
import Keys._

object ApplicationBuild extends Build {

    val appName         = "Kiki"
    val appVersion      = "0.1"

    val appDependencies = Seq(
        "org.hibernate"     %   "hibernate-validator"   %   "4.2.0.Final"
    )

    val main = PlayProject(appName, appVersion, appDependencies)

}
            
