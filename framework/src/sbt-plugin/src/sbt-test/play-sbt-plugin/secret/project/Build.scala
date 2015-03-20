/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

import play.sbt.PlayScala
import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appName = "secret-sample"
  val appVersion = "1.0-SNAPSHOT"

  val Secret = """(?s).*play.crypto.secret="(.*)".*""".r

  val main = Project(appName, file(".")).enablePlugins(PlayScala).settings(
    version := appVersion,
    TaskKey[Unit]("check-secret") := {
      val file = IO.read(baseDirectory.value / "conf/application.conf")
      file match {
        case Secret("changeme") => throw new RuntimeException("secret not changed!!\n" + file)
        case Secret(_) =>
        case _ => throw new RuntimeException("secret not found!!\n" + file)
      }
    }
  )

}
