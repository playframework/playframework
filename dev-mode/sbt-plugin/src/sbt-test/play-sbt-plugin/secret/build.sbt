/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

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
      val file = IO.read(baseDirectory.value / "conf/application.conf")
      val Secret = """(?s).*play.http.secret.key="(.*)".*""".r
      file match {
        case Secret("changeme") => sys.error(s"secret not changed!!\n$file")
        case Secret(_)          =>
        case _                  => sys.error(s"secret not found!!\n$file")
      }
    }
  )
