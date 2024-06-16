// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

// See:
// https://github.com/playframework/playframework/pull/12474
// https://github.com/playframework/playframework/pull/12624
val jjwtVersion = "0.12.5"
val jjwts = Seq(
  "io.jsonwebtoken" % "jjwt-api",
  "io.jsonwebtoken" % "jjwt-impl"
).map(_ % jjwtVersion) ++ Seq(
  ("io.jsonwebtoken" % "jjwt-jackson" % jjwtVersion).excludeAll(ExclusionRule("com.fasterxml.jackson.core"))
)

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    name          := "jjwt-compatibility",
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies += guice,
    libraryDependencies ++= jjwts,
    InputKey[Unit]("makeRequestWithSessionCookie") := {
      val args                                   = Def.spaceDelimited("<path> <status> <cookie> <words> ...").parsed
      val path :: status :: cookie :: assertions = args
      ScriptedTools.verifyResourceContains(path, status.toInt, assertions, "Cookie" -> s"PLAY_SESSION=$cookie")
    },
  )
