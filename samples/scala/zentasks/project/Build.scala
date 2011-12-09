import sbt._
import Keys._

import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "zentask"
    val appVersion      = "0.1"

    val main = PlayProject(appName, appVersion, mainLang = SCALA)

}
            
