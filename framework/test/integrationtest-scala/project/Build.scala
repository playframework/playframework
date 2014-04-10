/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import play.PlayScala
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

    val appName         = "integrationtest-scala"
    val appVersion      = "1.0"

    val appDependencies = Seq(
       ws
    )

    val main = Project(appName, file(".")).addPlugins(PlayScala).settings(
      version :=  appVersion, 
      libraryDependencies ++= appDependencies,      
      routesImport += "_root_.utils.BindersRoot",
      routesImport += "utils.Binders"
    )

}
