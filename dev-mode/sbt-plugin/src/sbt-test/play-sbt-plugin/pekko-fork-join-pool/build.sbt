// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

name := """pekko-fork-join-pool"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies += guice,
    InputKey[Unit]("callIndex") := {
      try ScriptedTools.callIndex()
      catch {
        case e: java.net.ConnectException =>
          play.sbt.run.PlayRun.stop(state.value)
          throw e
      }
    },
    InputKey[Unit]("checkLines") := {
      val args                  = Def.spaceDelimited("<source> <target>").parsed
      val source :: target :: _ = args
      try ScriptedTools.checkLines(source, target)
      catch {
        case e: java.net.ConnectException =>
          play.sbt.run.PlayRun.stop(state.value)
          throw e
      }
    }
  )
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
