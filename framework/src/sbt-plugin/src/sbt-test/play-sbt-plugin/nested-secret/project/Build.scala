/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

import com.typesafe.config.{Config, ConfigFactory}
import play.sbt.PlayScala
import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appName = "secret-sample"
  val appVersion = "1.0-SNAPSHOT"

  val main = Project(appName, file(".")).enablePlugins(PlayScala).settings(
    version := appVersion,
    TaskKey[Unit]("checkSecret") := {
      val file: File = baseDirectory.value / "conf/application.conf"
      val config: Config = ConfigFactory.parseFileAnySyntax(file)
      if(!config.hasPath("play.crypto.secret")){
        throw new RuntimeException("secret not found!!\n" + file)
      } else {
        config.getString("play.crypto.secret") match {
          case "changeme" => throw new RuntimeException("secret not changed!!\n" + file)
          case _ =>
        }
      }
    }
  )

}
