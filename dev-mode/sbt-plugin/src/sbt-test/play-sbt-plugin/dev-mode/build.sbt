// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    PlayKeys.fileWatchService    := play.dev.filewatch.FileWatchService.polling(500),
    libraryDependencies += guice,
    TaskKey[Unit]("resetReloads") := {
      (baseDirectory.value / "target" / "reload.log").delete()
      (baseDirectory.value / "target" / "stop.log").delete()
      (baseDirectory.value / "target" / "termination.log").delete()
    },
    InputKey[Unit]("verifyReloads") := {
      val expected = Def.spaceDelimited().parsed.head.toInt
      val actual   = IO.readLines(baseDirectory.value / "target" / "reload.log").count(_.nonEmpty)
      if (expected == actual) {
        println(s"Expected and got $expected reloads")
      } else {
        sys.error(s"Expected $expected reloads but got $actual")
      }
    },
    InputKey[Unit]("verifyStops") := {
      val expected = Def.spaceDelimited().parsed.head.toInt
      val stopLog  = baseDirectory.value / "target" / "stop.log"
      val actual   = if (stopLog.exists) IO.readLines(stopLog).count(_.nonEmpty) else 0
      if (expected == actual) {
        println(s"Expected and got $expected stops")
      } else {
        sys.error(s"Expected $expected stops but got $actual")
      }
    },
    InputKey[Unit]("verifyTerminations") := {
      val expected       = Def.spaceDelimited().parsed.head.toInt
      val terminationLog = baseDirectory.value / "target" / "termination.log"
      def actual         = if (terminationLog.exists) IO.readLines(terminationLog).count(_.nonEmpty) else 0
      // The actor system of an application that failed to start gets terminated in the background
      val deadline = System.currentTimeMillis() + 10000
      while (actual < expected && System.currentTimeMillis() < deadline) Thread.sleep(100)
      if (expected == actual) {
        println(s"Expected and got $expected terminations")
      } else {
        sys.error(s"Expected $expected terminations but got $actual")
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
