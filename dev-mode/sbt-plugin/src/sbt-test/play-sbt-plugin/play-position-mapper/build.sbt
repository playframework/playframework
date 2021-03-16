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
    // In sbt 1.4 extraLoggers got deprecated in favor of extraAppenders (new in sbt 1.4) and as a consequence logManager switched to use extraAppenders.
    // To be able to run the tests in sbt 1.2+ however we can't use extraAppenders yet and to run the tests in sbt 1.4+ we need to make logManager use extraLoggers again.
    // https://github.com/sbt/sbt/commit/2e9805b9d01c6345214c14264c61692d9c21651c#diff-6d9589bfb3f1247d2eace99bab7e928590337680d1aebd087d9da286586fba77R455
    logManager := sbt.internal.LogManager.defaults(extraLoggers.value, ConsoleOut.systemOut),
    extraLoggers ~= (fn => BufferLogger +: fn(_)),
    TaskKey[Unit]("compileIgnoreErrors") := state.map(s => Project.runTask(compile in Compile, s)).value,
    InputKey[Boolean]("checkLogContains") := {
      import sbt.complete.DefaultParsers._
      InputTask.separate[String, Boolean]((_: State) => Space ~> any.+.map(_.mkString(""))) {
        state(_ => (msg: String) => task {
          if (BufferLogger.messages.forall(!_.contains(msg))) {
            sys.error(
              s"""Did not find log message:
                 |    '$msg'
                 |in output:
                 |    ${BufferLogger.messages.reverse.mkString("\n    ")}""".stripMargin
            )
          }
          true
        })
      }.evaluated
    }
  )
