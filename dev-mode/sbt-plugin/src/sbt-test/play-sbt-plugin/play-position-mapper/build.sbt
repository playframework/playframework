/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "secret-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    libraryDependencies += guice,
    extraLoggers ~= (fn => ScriptedTools.bufferLogger +: fn(_)),
    TaskKey[Unit]("compileIgnoreErrors") := state.map(s => Project.runTask(compile in Compile, s)).value,
    InputKey[Boolean]("checkLogContains") := {
      import sbt.complete.DefaultParsers._
      InputTask.separate[String, Boolean]((_: State) => Space ~> any.+.map(_.mkString(""))) {
        state(_ => (msg: String) => task {
          if (ScriptedTools.bufferLoggerMessages.forall(!_.contains(msg))) {
            sys.error(
              s"""Did not find log message:
                 |    '$msg'
                 |in output:
                 |    ${ScriptedTools.bufferLogger.messages.reverse.mkString("\n    ")}""".stripMargin
            )
          }
          true
        })
      }.evaluated
    }
  )
