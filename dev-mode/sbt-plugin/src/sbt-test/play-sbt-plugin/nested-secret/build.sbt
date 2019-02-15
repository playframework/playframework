/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

import com.typesafe.config.{Config, ConfigFactory}

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "secret-sample",
    version := "1.0-SNAPSHOT",
    libraryDependencies += guice,
    TaskKey[Unit]("checkSecret") := {
      val file: File = baseDirectory.value / "conf/application.conf"
      val config: Config = ConfigFactory.parseFileAnySyntax(file)
      if (!config.hasPath("play.http.secret.key")) {
        throw new RuntimeException("secret not found!!\n" + file)
      } else {
        config.getString("play.http.secret.key") match {
          case "changeme" => throw new RuntimeException("secret not changed!!\n" + file)
          case _ =>
        }
      }
    }
  )
