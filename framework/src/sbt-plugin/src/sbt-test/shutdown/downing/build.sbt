import java.util.concurrent.TimeUnit
import sbt._

import sbt.Keys.libraryDependencies

//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

scalaVersion := "2.12.8"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  // disable PlayLayoutPlugin because the `test` file used by `sbt-scripted` collides with the `test/` Play expects.
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    libraryDependencies += guice,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.8"),

    fork in test := true ,

    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,

    PlayKeys.fileWatchService := DevModeBuild.initialFileWatchService,

    commands += DevModeBuild.assertProcessIsStopped,

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
      val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      DevModeBuild.verifyResourceContains(path, status.toInt, assertions, 0)
    }
  )

//sigtermApplication
