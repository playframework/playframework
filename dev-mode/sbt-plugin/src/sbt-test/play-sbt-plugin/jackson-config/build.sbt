// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .dependsOn(`play-scala`, `play-java`)
  .aggregate(`play-scala`, `play-java`)
  .settings(
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    InputKey[Unit]("makeRequest") := {
      val args                          = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      ScriptedTools.verifyResourceContains(path, status.toInt, assertions)
    },
    InputKey[Unit]("equals") := {
      val args                  = Def.spaceDelimited("<source> <target>").parsed
      val source :: target :: _ = args
      ScriptedTools.equals(source, target)
    },
  )

lazy val `play-scala` = (project in file("play-scala"))
  .enablePlugins(PlayScala)
  .dependsOn(`json-utils`)
  .settings(
    libraryDependencies += guice,
    libraryDependencies += ws,
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
  )

lazy val `play-java` = (project in file("play-java"))
  .enablePlugins(PlayJava)
  .dependsOn(`json-utils`)
  .settings(
    libraryDependencies += guice,
    libraryDependencies += javaWs,
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
  )

lazy val `json-utils` = (project in file("json-utils"))
  .enablePlugins(PlayService) // enabled just because it pulls in jackson dependencies needed
  .settings(
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties()
  )
