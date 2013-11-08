/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "integrationtest-scala"
    val appVersion      = "1.0"

    val appDependencies = Seq(
       ws
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
      routesImport += "_root_.utils.BindersRoot",
      routesImport += "utils.Binders"
    )

}
