/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */

import play.sbt.PlayScala
import play.sbt.test.MediatorWorkaroundPlugin
import sbt.Keys._
import sbt._

object ApplicationBuild extends Build {

  val appName = "secret-sample"
  val appVersion = "1.0-SNAPSHOT"

  val Secret = """(?s).*play.http.secret.key="(.*)".*""".r

  val main = Project(appName, file(".")).enablePlugins(PlayScala, MediatorWorkaroundPlugin).settings(
    version := appVersion,
    TaskKey[Unit]("checkSecret") := {
      val file = IO.read(baseDirectory.value / "conf/application.conf")
      file match {
        case Secret("changeme") => throw new RuntimeException("secret not changed!!\n" + file)
        case Secret(_) =>
        case _ => throw new RuntimeException("secret not found!!\n" + file)
      }
    }
  )

}
