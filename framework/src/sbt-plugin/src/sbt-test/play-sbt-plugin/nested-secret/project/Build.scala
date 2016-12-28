/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

import com.typesafe.config.{Config, ConfigFactory}
import play.sbt.PlayScala
import play.sbt.PlayImport._
import sbt.Keys._
import sbt._

object ApplicationBuild extends Build {

  val appName = "secret-sample"
  val appVersion = "1.0-SNAPSHOT"

  val main = Project(appName, file(".")).enablePlugins(PlayScala).settings(
    version := appVersion,
    libraryDependencies += guice,
    TaskKey[Unit]("checkSecret") := {
      val file: File = baseDirectory.value / "conf/application.conf"
      val config: Config = ConfigFactory.parseFileAnySyntax(file)
      if(!config.hasPath("play.http.secret.key")){
        throw new RuntimeException("secret not found!!\n" + file)
      } else {
        config.getString("play.http.secret.key") match {
          case "changeme" => throw new RuntimeException("secret not changed!!\n" + file)
          case _ =>
        }
      }
    }
  )

}
