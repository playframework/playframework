/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "secret-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    libraryDependencies += guice,
    TaskKey[Unit]("checkSecret") := {
      val file: File     = baseDirectory.value / "conf/application.conf"
      val config: Config = ConfigFactory.parseFileAnySyntax(file)
      if (config.hasPath("play.http.secret.key")) {
        if (config.getString("play.http.secret.key") == "changeme") {
          sys.error(s"secret not changed!!\n$file")
        }
      } else sys.error(s"secret not found!!\n$file")
    }
  )
