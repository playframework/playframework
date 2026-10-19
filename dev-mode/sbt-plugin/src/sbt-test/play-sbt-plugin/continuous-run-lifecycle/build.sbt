// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val verifyLifecycle = inputKey[Unit]("Check server ownership, cleanup and recovery warnings.")
lazy val prepareScenario = taskKey[Unit]("Reset lifecycle recordings.")
lazy val verifyWrapped   = taskKey[Unit]("Check that the wrapped termination hook ran.")
lazy val verifyProjectConflict = taskKey[Unit]("Check that another project's active server was rejected.")
lazy val customRun = inputKey[Unit]("A run task built with PlayRun.playRunTask outside Compile / run.")

lazy val library = (project in file("library"))
  .settings(scalaVersion := ScriptedTools.scalaVersionFromJavaProperties())

lazy val lifecycleSettings = Seq(
  scalaVersion := ScriptedTools.scalaVersionFromJavaProperties(),
  libraryDependencies += guice,
  PlayKeys.playDefaultPort := ContinuousRunLifecycleTest.freePort(),
  PlayKeys.playRunHooks += play.sbt.PluginCompat.uncached {
    val base = (LocalRootProject / baseDirectory).value
    new play.sbt.PlayRunHook {
      override def afterStarted(): Unit = ContinuousRunLifecycleTest.record(base, "server-starts")
      override def afterStopped(): Unit = ContinuousRunLifecycleTest.record(base, "server-stops")
    }
  },
  PlayKeys.playInteractionMode := {
    val port = PlayKeys.playDefaultPort.value
    new play.sbt.PlayInteractionMode {
      override def waitForCancel(): Unit = ContinuousRunLifecycleTest.request(port)
      override def doWithoutEcho(f: => Unit): Unit = f
    }
  },
  extraAppenders := {
    val file = (LocalRootProject / baseDirectory).value / "target" / "lifecycle-output.log"
    IO.createDirectory(file.getParentFile)
    new sbt.internal.AppenderSupplier {
      override def apply(key: Def.ScopedKey[_]): Seq[sbt.internal.util.Appender] =
        Seq(sbt.internal.util.ConsoleAppender(new java.io.PrintWriter(new java.io.FileOutputStream(file, true), true)))
    }
  },
  watchOnTermination := {
    val base = (LocalRootProject / baseDirectory).value
    (_, _, _, state) => {
      ContinuousRunLifecycleTest.record(base, "user-callbacks")
      state
    }
  },
  Compile / run / watchOnIteration := { (_, _, _) => sbt.nio.Watch.CancelWatch },
  // Custom keys built with playRunTask get no termination hook, so they keep Play's blocking run.
  customRun := sbt.PlayRun
    .playRunTask(
      PlayKeys.playRunHooks,
      play.sbt.PlayInternalKeys.playDependencyClasspath,
      play.sbt.PlayInternalKeys.playReloaderClasspath,
      play.sbt.PlayInternalKeys.playAssetsClassLoader
    )
    .map(_ => ())
    .evaluated,
  customRun / watchOnIteration := { (_, _, _) => sbt.nio.Watch.CancelWatch },
  prepareScenario := play.sbt.PluginCompat.uncached {
    ContinuousRunLifecycleTest.reset((LocalRootProject / baseDirectory).value)
  },
  verifyLifecycle := {
    val expected = sbt.complete.DefaultParsers.spaceDelimited("starts stops warnings").parsed.map(_.toInt)
    ContinuousRunLifecycleTest.verify((LocalRootProject / baseDirectory).value, PlayKeys.playDefaultPort.value, expected)
  },
  verifyWrapped := play.sbt.PluginCompat.uncached {
    assert(ContinuousRunLifecycleTest.count((LocalRootProject / baseDirectory).value, "wrapped-callbacks") == 1)
  },
  verifyProjectConflict := play.sbt.PluginCompat.uncached {
    val output = IO.read((LocalRootProject / baseDirectory).value / "target" / "lifecycle-output.log")
    assert(output.contains("project root already owns it in this watch, so project other cannot start"), "The other project reused the running server")
  }
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .dependsOn(library)
  .settings(lifecycleSettings)

lazy val other = (project in file("other"))
  .enablePlugins(PlayScala)
  .dependsOn(library)
  .settings(lifecycleSettings)
