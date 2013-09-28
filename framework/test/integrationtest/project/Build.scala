/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "integrationtest"
  val appVersion = "0.1"
  // oops this is in risk of being out date!
  val ning =  ("com.ning" % "async-http-client" % "1.7.18" notTransitive ()).exclude("org.jboss.netty", "netty")

  val appDependencies = Seq(
    javaJdbc,
    javaCore,
    anorm,
    ning,
    cache)

  val main = play.Project(appName, appVersion, appDependencies).settings(
                  requireJs += "main.js",
                  play.Project.emojiLogs
             )
}
            
