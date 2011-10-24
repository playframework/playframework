import sbt._
import Keys._

import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "simple-dataset"
    val appVersion      = "0.1"

    val appDependencies = Nil

    val main = PlayProject(appName, appVersion, appDependencies).settings(
        templatesImport ++= Seq(
            "play.data._",
            "models._",
            "com.avaje.ebean._",
            "play.mvc.Http.Context.Implicit._",
            "play.api.i18n.Messages",
            "java.util._",
            "java.lang._"
        )        
    )

}
            