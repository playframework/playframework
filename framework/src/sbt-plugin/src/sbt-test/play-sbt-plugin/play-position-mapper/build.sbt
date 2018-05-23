/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

import Common._

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "secret-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.6"),
    libraryDependencies += guice,
    extraLoggers := {
      val currentFunction = extraLoggers.value
      (key: ScopedKey[_]) => bufferLogger +: currentFunction(key)
    },
    InputKey[Boolean]("checkLogContains") := {
      InputTask.separate[String, Boolean](simpleParser _)(state(s => checkLogContains)).evaluated
    },

    TaskKey[Unit]("compileIgnoreErrors") := state.map { state =>
      Project.runTask(compile in Compile, state)
    }.value
  )
