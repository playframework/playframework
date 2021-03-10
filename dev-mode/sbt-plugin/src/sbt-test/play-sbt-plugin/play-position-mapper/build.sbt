/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

// These two imports are a hack to import LogManager
// which is defined as sbt.LogManager in sbt 0.13.x
// and as sbt.internal.LogManager in sbt 1.0+
import sbt._
import sbt.internal._

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
    // In sbt 1.4 extraLoggers got deprecated in favor of extraAppenders (new in sbt 1.4) and as a consequence logManager switched to use extraAppenders.
    // To be able to run the tests in sbt 1.2+ however we can't use extraAppenders yet and to run the tests in sbt 1.4+ we need to make logManager use extraLoggers again.
    // https://github.com/sbt/sbt/commit/2e9805b9d01c6345214c14264c61692d9c21651c#diff-6d9589bfb3f1247d2eace99bab7e928590337680d1aebd087d9da286586fba77R455
    logManager := LogManager.defaults(extraLoggers.value, ConsoleOut.systemOut),
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
