/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import play.PlayJava
import play.Play.autoImport._

object ApplicationBuild extends Build {

    val appName         = "integrationtest-java"
    val appVersion      = "1.0"

    val appDependencies = Seq(
    	javaCore,
    	javaEbean,
      javaWs,
      "org.hamcrest" % "hamcrest-all" % "1.3"
    )

    val main = Project(appName, file(".")).addPlugins(PlayJava).settings(
      version := appVersion, 
      libraryDependencies ++= appDependencies
    )

}
