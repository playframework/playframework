//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    PlayKeys.fileWatchService := play.dev.filewatch.FileWatchService.polling(500),
    libraryDependencies += guice,
    TaskKey[Unit]("resetReloads") := (target.value / "reload.log").delete(),
    InputKey[Unit]("verifyReloads") := {
      val expected = Def.spaceDelimited().parsed.head.toInt
      val actual   = IO.readLines(target.value / "reload.log").count(_.nonEmpty)
      if (expected == actual) {
        println(s"Expected and got $expected reloads")
      } else {
        sys.error(s"Expected $expected reloads but got $actual")
      }
    },
    InputKey[Unit]("makeRequestWithHeader") := {
      val args                      = Def.spaceDelimited("<path> <status> <headers> ...").parsed
      val path :: status :: headers = args
      val headerName                = headers.mkString
      ScriptedTools.verifyResourceContains(path, status.toInt, Nil, headerName -> "Header-Value")
    },
    InputKey[Unit]("verifyResourceContains") := {
      val args                         = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      ScriptedTools.verifyResourceContains(path, status.toInt, assertions)
    }
  )
