// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val library = (project in file("library"))
  .settings(scalaVersion := ScriptedTools.scalaVersionFromJavaProperties())

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .dependsOn(library)
  .settings(
    scalaVersion := ScriptedTools.scalaVersionFromJavaProperties(),
    libraryDependencies ++= Seq(guice, "org.playframework" %% "play-netty-server" % sys.props("project.version")),
    PlayKeys.playDefaultPort := ContinuousRunTest.freePort(),
    Compile / unmanagedSourceDirectories += baseDirectory.value / "new-sources",
    commands += Command.command("runWithArguments")(ContinuousRunTest.runWithArguments),
    PlayKeys.playRunHooks += play.sbt.PluginCompat.uncached {
      val base = baseDirectory.value
      new play.sbt.PlayRunHook {
        override def afterStarted(): Unit = ContinuousRunTest.record(base, "server-starts")
        override def afterStopped(): Unit = ContinuousRunTest.record(base, "server-stops")
      }
    },
    // The plugin must preserve a user's termination callback as well as closing the server.
    watchOnTermination := {
      val base = baseDirectory.value
      (_, _, _, state) => {
        ContinuousRunTest.record(base, "watch-stops")
        state
      }
    },
    Compile / run / watchOnIteration := {
      val base           = baseDirectory.value
      val classes        = (Compile / classDirectory).value
      val libraryClasses = (library / Compile / classDirectory).value
      val port           = PlayKeys.playDefaultPort.value
      (_, _, _) => ContinuousRunTest.onIteration(base, classes, libraryClasses, port)
    },
    TaskKey[Unit]("verifyStopped") := play.sbt.PluginCompat.uncached {
      ContinuousRunTest.verifyStopped(baseDirectory.value, PlayKeys.playDefaultPort.value)
    },
    TaskKey[Unit]("resetScenario") := play.sbt.PluginCompat.uncached {
      ContinuousRunTest.reset(baseDirectory.value)
    },
    TaskKey[Unit]("verifyArguments") := play.sbt.PluginCompat.uncached {
      ContinuousRunTest.verifyArguments(baseDirectory.value)
    }
  )
