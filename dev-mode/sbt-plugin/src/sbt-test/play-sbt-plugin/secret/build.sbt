// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name          := "secret-sample",
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    libraryDependencies += guice,
    TaskKey[Unit]("checkSecret") := {
      val file   = IO.read(baseDirectory.value / "conf/application.conf")
      val Secret = """(?s).*play.http.secret.key="(.*)".*""".r
      file match {
        case Secret("changeme") => sys.error(s"secret not changed!!\n$file")
        case Secret(_)          =>
        case _                  => sys.error(s"secret not found!!\n$file")
      }
    }
  )
