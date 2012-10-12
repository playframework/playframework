import sbt._
import Keys._

import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "zentask"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      jdbc,
      anorm
    )

    val main = PlayProject(appName, appVersion, appDependencies)

}
            
