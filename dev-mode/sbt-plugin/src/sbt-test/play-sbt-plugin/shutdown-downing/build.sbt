// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import java.util.concurrent.TimeUnit

import sbt._
import sbt.Keys.libraryDependencies

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  // disable PlayLayoutPlugin because the `test` file used by `sbt-scripted` collides with the `test/` Play expects.
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    libraryDependencies += guice,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.0" % Test,
    test / fork                                     := true,
    PlayKeys.playInteractionMode                    := play.sbt.StaticPlayNonBlockingInteractionMode,
    commands += ScriptedTools.assertProcessIsStopped,
    InputKey[Unit]("awaitPidfileDeletion") := {
      val pidFile = target.value / "universal" / "stage" / "RUNNING_PID"
      // Use a polling loop of at most 30sec. Without it, the `scripted-test` moves on
      // before the application has finished to shut down
      val secs = 30
      // NiceToHave: replace with System.nanoTime()
      val end = System.currentTimeMillis() + secs * 1000
      while (pidFile.exists() && System.currentTimeMillis() < end) {
        TimeUnit.SECONDS.sleep(3)
      }
      if (pidFile.exists()) {
        println(s"[ERROR] RUNNING_PID file was not deleted in ${secs}s")
      } else {
        println("Application stopped.")
      }
    },
    InputKey[Unit]("verifyResourceContains") := {
      val args                         = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      ScriptedTools.verifyResourceContains(path, status.toInt, assertions)
    }
  )

//sigtermApplication
