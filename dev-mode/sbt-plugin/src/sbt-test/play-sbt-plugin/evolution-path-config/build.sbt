// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  // .enablePlugins(PlayScala)
  .settings(
    name          := "evolutions-path-config",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies ++= Seq(
      guice,
      jdbc,
      evolutions,
      "com.h2database" % "h2" % "2.2.220",
    )
  )

lazy val itTests = (project in file("integration-tests"))
  .enablePlugins(PlayScala)
  .dependsOn(root)
  .settings(
    name          := "evolutions-path-config-integration-tests",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    publish / skip               := true,
    // test dependencies
    libraryDependencies ++= Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.0-M6" % Test
    )
  )
