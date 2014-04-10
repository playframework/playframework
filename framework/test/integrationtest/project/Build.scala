/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import play.PlayJava
import play.Play.autoImport._

object ApplicationBuild extends Build {

  val appName = "integrationtest"
  val appVersion = "0.1"

  val appDependencies = Seq(
    javaJdbc,
    javaCore,
    javaWs,
    anorm,
    cache
  )

  val main = Project(appName, file(".")).addPlugins(PlayJava).settings(
    version := appVersion, 
    libraryDependencies ++= appDependencies,
    emojiLogs
  )
}
            
